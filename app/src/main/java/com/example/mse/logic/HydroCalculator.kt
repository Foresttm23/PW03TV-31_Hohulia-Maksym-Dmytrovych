package com.example.mse.logic

import com.example.mse.model.HydroStationData
import kotlin.math.max
import kotlin.math.round
import kotlin.random.Random

data class HydroCalculationResult(
    val powerMw: Double,
    val dailyGenerationMwh: Double,
    val monthlyGenerationMwh: Double,
    val totalWaterVolumeM3: Double
)

data class HydroCalculatorInput(
    val waterHeadMeters: Double,
    val waterFlowM3PerSec: Double,
    val efficiencyPercentage: Double = 88.0,
    val operatingHoursPerDay: Double = 24.0
)

object HydroCalculator {

    private const val WATER_DENSITY = 1000.0
    private const val GRAVITY = 9.81

    fun calculateEnergy(input: HydroCalculatorInput): HydroCalculationResult {
        val head = max(0.0, input.waterHeadMeters)
        val flow = max(0.0, input.waterFlowM3PerSec)
        val eff = max(0.0, input.efficiencyPercentage) / 100.0
        val hours = max(0.0, input.operatingHoursPerDay)

        val powerWatts = eff * WATER_DENSITY * GRAVITY * flow * head
        val powerMw = powerWatts / 1_000_000.0
        val dailyGenerationMwh = powerMw * hours
        val monthlyGenerationMwh = dailyGenerationMwh * 30.0
        val totalWaterVolumeM3 = flow * 3600.0 * hours

        return HydroCalculationResult(
            powerMw = round(powerMw * 100.0) / 100.0,
            dailyGenerationMwh = round(dailyGenerationMwh * 100.0) / 100.0,
            monthlyGenerationMwh = round(monthlyGenerationMwh * 10.0) / 10.0,
            totalWaterVolumeM3 = round(totalWaterVolumeM3)
        )
    }

    fun generateRandomMetrics(): HydroStationData {
        val level = 118.0f + Random.nextFloat() * 5.0f
        val flow = 200.0f + Random.nextFloat() * 300.0f
        val head = 12.0f + Random.nextFloat() * 6.0f

        val calc = calculateEnergy(
            HydroCalculatorInput(
                waterHeadMeters = head.toDouble(),
                waterFlowM3PerSec = flow.toDouble()
            )
        )

        return HydroStationData(
            waterLevelMeters = round(level * 10f) / 10f,
            targetWaterLevelMeters = 122.0f,
            waterFlowRateM3PerSec = round(flow * 10f) / 10f,
            generatedPowerMw = calc.powerMw.toFloat(),
            waterHeadMeters = round(head * 10f) / 10f
        )
    }
}
