package com.example.mse.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class HydroStation(
    val id: String,
    val name: String,
    val river: String,
    val city: String,
    val lat: Double,
    val lon: Double,
    val designHeadM: Double,
    val normFlowM3s: Double,
    val efficiency: Double,
    val installedMw: Double,
    val unitsDescription: String
)

data class DailyHydroForecast(
    val date: String,
    val dischargeM3s: Double,
    val dischargeMaxM3s: Double,
    val dischargeMinM3s: Double,
    val precipitationMm: Double,
    val tempMaxC: Double?,
    val tempMinC: Double?,
    val snowfallCm: Double,
    val powerMw: Double,
    val energyMwh: Double
)

enum class FloodRisk(val title: String) {
    LOW("Низький"),
    MODERATE("Помірний"),
    HIGH("Високий"),
    CRITICAL("Критичний")
}

data class StationForecast(
    val station: HydroStation,
    val days: List<DailyHydroForecast>,
    val currentDischargeM3s: Double,
    val peakDischargeM3s: Double,
    val meanDischargeM3s: Double,
    val trendPercentPerDay: Double,
    val currentPowerMw: Double,
    val weeklyEnergyMwh: Double,
    val capacityFactor: Double,
    val precipitationWeekMm: Double,
    val riskScore: Double,
    val risk: FloodRisk,
    val recommendation: String,
    val origin: DataOrigin,
    val fetchedAt: Long,
    val droppedInvalidDays: Int
)

data class StationFailure(val station: HydroStation, val message: String)

data class CascadeSummary(
    val stationsCount: Int,
    val totalPowerMw: Double,
    val totalInstalledMw: Double,
    val weeklyEnergyGwh: Double,
    val totalInflowM3s: Double,
    val riskiestStation: String?,
    val riskCounts: Map<FloodRisk, Int>
)

data class CascadeForecast(
    val stations: List<StationForecast>,
    val failures: List<StationFailure>,
    val summary: CascadeSummary,
    val origin: DataOrigin
)

enum class StationSort(val title: String) {
    RISK("За ризиком"),
    DISCHARGE("За витратою"),
    POWER("За потужністю"),
    NAME("За назвою")
}

@Entity(tableName = "forecast_cache")
data class CachedForecastEntity(
    @PrimaryKey val stationId: String,
    val floodJson: String,
    val weatherJson: String,
    val fetchedAt: Long
)
