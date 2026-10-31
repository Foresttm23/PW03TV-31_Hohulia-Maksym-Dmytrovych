package com.example.mse.data.remote

import com.example.mse.data.remote.dto.FloodResponseDto
import com.example.mse.data.remote.dto.WeatherResponseDto
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface FloodApiService {
    @GET("v1/flood")
    suspend fun getRiverDischarge(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("daily") daily: String = "river_discharge,river_discharge_max,river_discharge_min",
        @Query("forecast_days") forecastDays: Int = 7
    ): FloodResponseDto
}

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getDailyWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("daily") daily: String = "precipitation_sum,temperature_2m_max,temperature_2m_min,snowfall_sum",
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("timezone") timezone: String = "Europe/Kyiv"
    ): WeatherResponseDto
}

object OpenMeteoClient {
    private const val FLOOD_BASE_URL = "https://flood-api.open-meteo.com/"
    private const val WEATHER_BASE_URL = "https://api.open-meteo.com/"

    val gson: Gson = GsonBuilder().create()

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
    }

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val floodApi: FloodApiService by lazy { retrofit(FLOOD_BASE_URL).create(FloodApiService::class.java) }
    val weatherApi: WeatherApiService by lazy { retrofit(WEATHER_BASE_URL).create(WeatherApiService::class.java) }
}
