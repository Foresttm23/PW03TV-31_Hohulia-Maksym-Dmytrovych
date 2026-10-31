package com.example.mse

import com.example.mse.data.db.CachedForecastDao
import com.example.mse.data.error.RetryPolicy
import com.example.mse.data.remote.FloodApiService
import com.example.mse.data.remote.WeatherApiService
import com.example.mse.data.remote.dto.FloodDailyDto
import com.example.mse.data.remote.dto.FloodResponseDto
import com.example.mse.data.remote.dto.StationCatalogDto
import com.example.mse.data.remote.dto.WeatherDailyDto
import com.example.mse.data.remote.dto.WeatherResponseDto
import com.example.mse.data.repository.HydroForecastRepository
import com.example.mse.model.CachedForecastEntity
import com.example.mse.model.DataOrigin
import com.example.mse.model.DataResult
import com.google.gson.Gson
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

private const val CATALOG_JSON = """
{ "cascade": "Тест", "version": 1, "stations": [
  { "id": "kyiv", "name": "Київська ГЕС", "river": "Дніпро",
    "location": { "lat": 50.44, "lon": 30.50, "city": "Вишгород" },
    "hydraulics": { "designHeadM": 11.5, "normFlowM3s": 100.0, "efficiency": 0.9 },
    "units": [ { "name": "ГА-1", "type": "Капсульний", "capacityMw": 408.5 } ] }
] }
"""

private fun flood(vararg q: Double?) = FloodResponseDto(
    50.44, 30.5, 1.0, mapOf("river_discharge" to "m³/s"),
    FloodDailyDto(q.indices.map { "2026-11-0${it + 1}" }, q.toList(), q.map { it?.plus(10) }, q.map { it?.minus(5) })
)

private fun weather(vararg p: Double) = WeatherResponseDto(
    50.44, 30.5, "Europe/Kyiv", null,
    WeatherDailyDto(p.indices.map { "2026-11-0${it + 1}" }, p.toList(), p.map { 10.0 }, p.map { 2.0 }, p.map { 0.0 })
)

class FakeFloodApi(private val responses: MutableList<() -> FloodResponseDto>) : FloodApiService {
    var calls = 0
    override suspend fun getRiverDischarge(latitude: Double, longitude: Double, daily: String, forecastDays: Int): FloodResponseDto {
        calls++
        return responses.removeAt(0).invoke()
    }
}

class FakeWeatherApi(private val response: () -> WeatherResponseDto) : WeatherApiService {
    override suspend fun getDailyWeather(latitude: Double, longitude: Double, daily: String, forecastDays: Int, timezone: String) = response()
}

class FakeForecastDao : CachedForecastDao {
    val rows = mutableMapOf<String, CachedForecastEntity>()
    override suspend fun get(stationId: String) = rows[stationId]
    override suspend fun upsert(entity: CachedForecastEntity) { rows[entity.stationId] = entity }
    override suspend fun clear() = rows.clear()
}

class HydroForecastTest {
    private val gson = Gson()
    private val catalog = gson.fromJson(CATALOG_JSON, StationCatalogDto::class.java)

    @Test
    fun repositoryRetriesTransientErrorAndCachesResponse() = runTest {
        val error503 = HttpException(Response.error<Any>(503, "{}".toResponseBody(null)))
        val floodApi = FakeFloodApi(mutableListOf({ throw error503 }, { throw IOException("reset") }, { flood(100.0, 105.0) }))
        val dao = FakeForecastDao()
        val repo = repository(floodApi, dao)

        val result = repo.loadForecast(forceRefresh = true) as DataResult.Success
        assertEquals(DataOrigin.NETWORK, result.source)
        assertEquals("503 і IOException повторено, третя спроба успішна", 3, floodApi.calls)
        assertTrue("Відповідь має потрапити в офлайн-кеш", dao.rows.containsKey("kyiv"))
        assertEquals(10.153, result.data.stations.single().currentPowerMw, 1e-3)

        val cached = repo.loadForecast(forceRefresh = false) as DataResult.Success
        assertEquals(DataOrigin.MEMORY_CACHE, cached.source)
        assertEquals("Повторне відкриття не робить запитів", 3, floodApi.calls)
    }

    @Test
    fun repositoryFallsBackToOfflineCache() = runTest {
        val dao = FakeForecastDao().apply {
            rows["kyiv"] = CachedForecastEntity("kyiv", gson.toJson(flood(90.0, 95.0)), gson.toJson(weather(0.0, 0.0)), 123L)
        }
        val failing = FakeFloodApi(MutableList(3) { { throw IOException("no route") } })
        val result = repository(failing, dao).loadForecast(forceRefresh = true) as DataResult.Success
        assertEquals(DataOrigin.OFFLINE_CACHE, result.data.origin)
        assertEquals(123L, result.data.stations.single().fetchedAt)
    }

    private fun TestScope.repository(floodApi: FloodApiService, dao: CachedForecastDao) =
        HydroForecastRepository(
            floodApi = floodApi,
            weatherApi = FakeWeatherApi { weather(1.0, 2.0) },
            cacheDao = dao,
            catalogSource = { catalog },
            gson = gson,
            retryPolicy = RetryPolicy(maxAttempts = 3, initialDelayMs = 10),
            ioDispatcher = StandardTestDispatcher(testScheduler),
            clock = { testScheduler.currentTime }
        )
}
