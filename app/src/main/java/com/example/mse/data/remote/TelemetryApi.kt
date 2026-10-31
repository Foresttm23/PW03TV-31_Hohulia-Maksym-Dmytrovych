package com.example.mse.data.remote

import com.example.mse.model.StationTelemetryResponse
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface TelemetryApi {
    @GET("api/telemetry/{stationCode}")
    suspend fun getTelemetry(
        @Path("stationCode") stationCode: String,
        @Query("hours") hours: Int
    ): StationTelemetryResponse
}

object TelemetryNetwork {
    private const val BASE_URL = "https://scada.hydro-ges.mock/"

    fun createApi(server: Interceptor, gson: Gson = Gson()): TelemetryApi {
        val client = OkHttpClient.Builder()
            .callTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .addInterceptor(server)
            .build()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(TelemetryApi::class.java)
    }
}
