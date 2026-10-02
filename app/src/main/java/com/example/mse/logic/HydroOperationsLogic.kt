package com.example.mse.logic

import kotlin.math.max
import kotlin.math.round

enum class RiskLevel(val title: String, val description: String) {
    SAFE("Безпечний рівень", "Показники у межах нормального підпірного рівня (НПУ)"),
    WARNING("Увага! Наповнення", "Рівень води наближається до критичної позначки"),
    DANGER("Аварійна загроза!", "Перевищено максимальний форсажного підпірного рівня (ФПУ). Необхідне скидання")
}

data class GateInfo(
    val id: Int,
    val name: String,
    val waterLevel: Float,
    val isOpen: Boolean,
    val openingPercentage: Int
)

data class WaterBalanceResult(
    val netFlowM3PerSec: Double,
    val hourlyLevelChangeMeters: Double,
    val hoursToCriticalLevel: Double,
    val isEmergencyDischargeRecommended: Boolean
)

object HydroOperationsLogic {

    fun assessGateRisk(waterLevel: Float, isOpen: Boolean): RiskLevel {
        return when {
            waterLevel >= 123.5f -> RiskLevel.DANGER
            waterLevel >= 121.5f -> if (isOpen) RiskLevel.WARNING else RiskLevel.DANGER
            else -> RiskLevel.SAFE
        }
    }

    fun calculateWaterBalance(
        inflowM3PerSec: Double,
        turbineFlowM3PerSec: Double,
        spillwayFlowM3PerSec: Double,
        currentWaterLevelMeters: Double,
        reservoirAreaKm2: Double = 50.0,
        criticalWaterLevelMeters: Double = 124.0
    ): WaterBalanceResult {
        val netFlow = inflowM3PerSec - (turbineFlowM3PerSec + spillwayFlowM3PerSec)
        val areaM2 = reservoirAreaKm2 * 1_000_000.0

        val volumeChangePerHourM3 = netFlow * 3600.0
        val hourlyChangeMeters = volumeChangePerHourM3 / areaM2

        val remainingMarginMeters = max(0.0, criticalWaterLevelMeters - currentWaterLevelMeters)
        val hoursToCritical = if (hourlyChangeMeters > 0) {
            remainingMarginMeters / hourlyChangeMeters
        } else {
            Double.POSITIVE_INFINITY
        }

        val emergencyRecommended = currentWaterLevelMeters >= 122.5 && netFlow > 0

        return WaterBalanceResult(
            netFlowM3PerSec = round(netFlow * 10.0) / 10.0,
            hourlyLevelChangeMeters = round(hourlyChangeMeters * 1000.0) / 1000.0,
            hoursToCriticalLevel = if (hoursToCritical.isInfinite()) -1.0 else round(hoursToCritical * 10.0) / 10.0,
            isEmergencyDischargeRecommended = emergencyRecommended
        )
    }

    fun calculateDownstreamRiverLevel(totalSpillwayFlowM3PerSec: Double): String {
        return when {
            totalSpillwayFlowM3PerSec > 1200 -> "Критичний підйом нижнього б'єфу (Загроза затоплення)"
            totalSpillwayFlowM3PerSec > 600 -> "Підвищений рівень нижнього б'єфу (Забережні попередження)"
            else -> "Нормальний рівень річки в нижньому б'єфі"
        }
    }
}
