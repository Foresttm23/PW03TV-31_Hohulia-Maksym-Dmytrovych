package com.example.mse.data.remote.dto

import com.google.gson.annotations.SerializedName

// Поля nullable: Gson створює об'єкти через рефлексію в обхід null-safety Kotlin.
data class FloodResponseDto(
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("generationtime_ms") val generationTimeMs: Double?,
    @SerializedName("daily_units") val dailyUnits: Map<String, String>?,
    @SerializedName("daily") val daily: FloodDailyDto?
)

data class FloodDailyDto(
    @SerializedName("time") val time: List<String>?,
    @SerializedName("river_discharge") val discharge: List<Double?>?,
    @SerializedName("river_discharge_max") val dischargeMax: List<Double?>?,
    @SerializedName("river_discharge_min") val dischargeMin: List<Double?>?
)

data class WeatherResponseDto(
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("timezone") val timezone: String?,
    @SerializedName("daily_units") val dailyUnits: Map<String, String>?,
    @SerializedName("daily") val daily: WeatherDailyDto?
)

data class WeatherDailyDto(
    @SerializedName("time") val time: List<String>?,
    @SerializedName("precipitation_sum") val precipitationSum: List<Double?>?,
    @SerializedName("temperature_2m_max") val temperatureMax: List<Double?>?,
    @SerializedName("temperature_2m_min") val temperatureMin: List<Double?>?,
    @SerializedName("snowfall_sum") val snowfallSum: List<Double?>?
)

data class StationCatalogDto(
    @SerializedName("cascade") val cascade: String?,
    @SerializedName("version") val version: Int?,
    @SerializedName("stations") val stations: List<StationDto>?
)

data class StationDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("river") val river: String?,
    @SerializedName("location") val location: LocationDto?,
    @SerializedName("hydraulics") val hydraulics: HydraulicsDto?,
    @SerializedName("units") val units: List<TurbineUnitDto>?
)

data class LocationDto(
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lon") val lon: Double?,
    @SerializedName("city") val city: String?
)

data class HydraulicsDto(
    @SerializedName("designHeadM") val designHeadM: Double?,
    @SerializedName("normFlowM3s") val normFlowM3s: Double?,
    @SerializedName("efficiency") val efficiency: Double?
)

data class TurbineUnitDto(
    @SerializedName("name") val name: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("capacityMw") val capacityMw: Double?
)
