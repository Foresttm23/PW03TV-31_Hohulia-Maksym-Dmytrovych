package com.example.mse.data.repository

import android.content.Context
import com.example.mse.data.db.CachedForecastDao
import com.example.mse.data.error.RetryEvent
import com.example.mse.data.error.RetryPolicy
import com.example.mse.data.error.retryWithBackoff
import com.example.mse.data.remote.FloodApiService
import com.example.mse.data.remote.WeatherApiService
import com.example.mse.data.remote.dto.FloodResponseDto
import com.example.mse.data.remote.dto.StationCatalogDto
import com.example.mse.data.remote.dto.WeatherResponseDto
import com.example.mse.data.source.MemoryCache
import com.example.mse.logic.ForecastValidator
import com.example.mse.logic.HydroForecastCalculator
import com.example.mse.logic.InvalidForecastDataException
import com.example.mse.model.CachedForecastEntity
import com.example.mse.model.CascadeForecast
import com.example.mse.model.DataOrigin
import com.example.mse.model.DataResult
import com.example.mse.model.HydroStation
import com.example.mse.model.StationFailure
import com.example.mse.model.StationForecast
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicInteger

fun interface StationCatalogSource {
    suspend fun load(): StationCatalogDto
}

class AssetStationCatalogSource(private val context: Context, private val gson: Gson) : StationCatalogSource {
    override suspend fun load(): StationCatalogDto =
        context.assets.open("hpp_stations.json").bufferedReader().use {
            gson.fromJson(it, StationCatalogDto::class.java)
        }
}

sealed interface ForecastProgress {
    data class Loading(val done: Int, val total: Int) : ForecastProgress
    data class Retrying(val stationName: String, val event: RetryEvent) : ForecastProgress
}

class HydroForecastRepository(
    private val floodApi: FloodApiService,
    private val weatherApi: WeatherApiService,
    private val cacheDao: CachedForecastDao,
    private val catalogSource: StationCatalogSource,
    private val gson: Gson,
    private val retryPolicy: RetryPolicy = RetryPolicy(maxAttempts = 3, initialDelayMs = 800),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val memoryCache = MemoryCache<CascadeForecast>(ttlMs = 10 * 60_000L, clock = clock)
    // Обмеження одночасних з'єднань: публічний API повертає HTTP 429 при перевантаженні.
    private val requestLimiter = Semaphore(permits = 3)
    private var stations: List<HydroStation>? = null

    suspend fun loadForecast(
        forceRefresh: Boolean,
        onProgress: suspend (ForecastProgress) -> Unit = {}
    ): DataResult<CascadeForecast> = withContext(ioDispatcher) {
        if (!forceRefresh) {
            memoryCache.getIfFresh()?.let {
                return@withContext DataResult.Success(it.copy(origin = DataOrigin.MEMORY_CACHE), DataOrigin.MEMORY_CACHE)
            }
        }
        val catalog = try {
            stations ?: ForecastValidator.parseCatalog(catalogSource.load()).also { stations = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return@withContext DataResult.Error("Не вдалося прочитати каталог станцій: ${e.message}", e)
        }
        if (catalog.isEmpty()) return@withContext DataResult.Error("Каталог станцій порожній або некоректний")

        val done = AtomicInteger(0)
        onProgress(ForecastProgress.Loading(0, catalog.size))
        val outcomes = coroutineScope {
            catalog.map { station ->
                async {
                    loadStation(station, onProgress).also {
                        onProgress(ForecastProgress.Loading(done.incrementAndGet(), catalog.size))
                    }
                }
            }.awaitAll()
        }

        val forecasts = outcomes.mapNotNull { it.getOrNull() }
        val failures = outcomes.mapIndexedNotNull { i, r ->
            r.exceptionOrNull()?.let { StationFailure(catalog[i], describe(it)) }
        }
        if (forecasts.isEmpty()) {
            return@withContext DataResult.Error(
                "Немає даних ні з мережі, ні з офлайн-кешу. ${failures.firstOrNull()?.message.orEmpty()}"
            )
        }
        val origin = when {
            forecasts.all { it.origin == DataOrigin.NETWORK } -> DataOrigin.NETWORK
            else -> DataOrigin.OFFLINE_CACHE
        }
        val cascade = CascadeForecast(forecasts, failures, HydroForecastCalculator.summarize(forecasts), origin)
        // Офлайн-результат не кешуємо в пам'яті, щоб наступне відкриття знову спробувало мережу.
        if (origin == DataOrigin.NETWORK) memoryCache.put(cascade) else memoryCache.invalidate()
        DataResult.Success(cascade, origin)
    }

    private suspend fun loadStation(
        station: HydroStation,
        onProgress: suspend (ForecastProgress) -> Unit
    ): Result<StationForecast> {
        val networkError: Throwable = try {
            val (flood, weather) = fetchBothResources(station, onProgress)
            val now = clock()
            val (days, dropped) = ForecastValidator.combine(flood, weather)
            cacheDao.upsert(CachedForecastEntity(station.id, gson.toJson(flood), gson.toJson(weather), now))
            return Result.success(HydroForecastCalculator.build(station, days, DataOrigin.NETWORK, now, dropped))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e
        }
        return try {
            val cached = cacheDao.get(station.id) ?: return Result.failure(networkError)
            val flood = gson.fromJson(cached.floodJson, FloodResponseDto::class.java)
            val weather = gson.fromJson(cached.weatherJson, WeatherResponseDto::class.java)
            val (days, dropped) = ForecastValidator.combine(flood, weather)
            Result.success(HydroForecastCalculator.build(station, days, DataOrigin.OFFLINE_CACHE, cached.fetchedAt, dropped))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(networkError)
        }
    }

    private suspend fun fetchBothResources(
        station: HydroStation,
        onProgress: suspend (ForecastProgress) -> Unit
    ): Pair<FloodResponseDto, WeatherResponseDto?> = coroutineScope {
        val flood = async {
            requestLimiter.withPermit {
                retryWithBackoff(retryPolicy, onRetry = { onProgress(ForecastProgress.Retrying(station.name, it)) }) {
                    floodApi.getRiverDischarge(station.lat, station.lon)
                }
            }
        }
        // Погода — допоміжний ресурс: її збій не скасовує прогноз витрати.
        val weather = async {
            try {
                requestLimiter.withPermit {
                    retryWithBackoff(retryPolicy) { weatherApi.getDailyWeather(station.lat, station.lon) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
        flood.await() to weather.await()
    }

    private fun describe(error: Throwable): String = when (error) {
        is UnknownHostException -> "Немає з'єднання з Інтернетом"
        is SocketTimeoutException -> "Сервер не відповів вчасно"
        is HttpException -> "HTTP ${error.code()}: ${if (error.code() == 429) "перевищено ліміт запитів" else "помилка сервера"}"
        is InvalidForecastDataException -> "Некоректні дані: ${error.message}"
        is IOException -> "Мережева помилка: ${error.message}"
        else -> error.message ?: error.javaClass.simpleName
    }
}
