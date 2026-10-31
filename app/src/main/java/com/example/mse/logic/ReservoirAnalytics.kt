package com.example.mse.logic

import com.example.mse.model.GaugeReading
import com.example.mse.model.LevelStatus
import com.example.mse.model.ReadingFilter
import com.example.mse.model.ReadingSort
import com.example.mse.model.Reservoir
import com.example.mse.model.ReservoirStats

object ReservoirAnalytics {
    private const val MS_IN_HOUR = 3_600_000.0

    fun applyFilter(readings: List<GaugeReading>, filter: ReadingFilter): List<GaugeReading> {
        val filtered = readings.filter { r ->
            (filter.reservoir == null || r.reservoir == filter.reservoir) &&
                (filter.status == null || r.status == filter.status)
        }
        return when (filter.sort) {
            ReadingSort.NEWEST -> filtered.sortedByDescending { it.timestamp }
            ReadingSort.OLDEST -> filtered.sortedBy { it.timestamp }
            ReadingSort.LEVEL_DESC -> filtered.sortedByDescending { it.fillPercent }
            ReadingSort.LEVEL_ASC -> filtered.sortedBy { it.fillPercent }
            ReadingSort.BALANCE_DESC -> filtered.sortedByDescending { it.balanceM3s }
        }
    }

    fun aggregate(readings: List<GaugeReading>): List<ReservoirStats> =
        readings.groupBy { it.reservoir }
            .map { (reservoir, items) -> statsFor(reservoir, items) }
            .sortedBy { it.reservoir.ordinal }

    private fun statsFor(reservoir: Reservoir, items: List<GaugeReading>): ReservoirStats {
        val chronological = items.sortedBy { it.timestamp }
        val latest = chronological.last()
        return ReservoirStats(
            reservoir = reservoir,
            readingsCount = items.size,
            avgLevel = items.map { it.waterLevelM }.average(),
            minLevel = items.minOf { it.waterLevelM },
            maxLevel = items.maxOf { it.waterLevelM },
            latestLevel = latest.waterLevelM,
            avgBalanceM3s = items.map { it.balanceM3s }.average(),
            trendMPerHour = levelTrendPerHour(chronological),
            criticalCount = items.count { it.status == LevelStatus.CRITICAL || it.status == LevelStatus.WARNING },
            latestStatus = latest.status
        )
    }

    fun levelTrendPerHour(chronological: List<GaugeReading>): Double {
        if (chronological.size < 2) return 0.0
        val t0 = chronological.first().timestamp
        val xs = chronological.map { (it.timestamp - t0) / MS_IN_HOUR }
        val ys = chronological.map { it.waterLevelM }
        val meanX = xs.average()
        val meanY = ys.average()
        val denominator = xs.sumOf { (it - meanX) * (it - meanX) }
        if (denominator == 0.0) return 0.0
        val numerator = xs.indices.sumOf { (xs[it] - meanX) * (ys[it] - meanY) }
        return numerator / denominator
    }
}

data class ReadingFormErrors(
    val gaugeCode: String? = null,
    val level: String? = null,
    val inflow: String? = null,
    val outflow: String? = null
) {
    val hasErrors: Boolean get() = listOfNotNull(gaugeCode, level, inflow, outflow).isNotEmpty()
}

object GaugeReadingValidator {
    private val GAUGE_CODE_REGEX = Regex("^[A-Z]{3}-\\d{2}$")
    private const val MAX_FLOW_M3S = 25_000.0

    fun validate(
        reservoir: Reservoir,
        gaugeCode: String,
        level: String,
        inflow: String,
        outflow: String
    ): ReadingFormErrors {
        val levelValue = level.replace(',', '.').toDoubleOrNull()
        return ReadingFormErrors(
            gaugeCode = when {
                gaugeCode.isBlank() -> "Вкажіть код гідропоста"
                !GAUGE_CODE_REGEX.matches(gaugeCode.trim()) -> "Формат: ${reservoir.codePrefix}-01"
                !gaugeCode.trim().startsWith(reservoir.codePrefix) -> "Код має починатися з ${reservoir.codePrefix}"
                else -> null
            },
            level = when {
                levelValue == null -> "Введіть число (м)"
                levelValue < reservoir.deadLevel - 5 || levelValue > reservoir.forcedLevel + 3 ->
                    "Допустимо ${reservoir.deadLevel - 5}…${reservoir.forcedLevel + 3} м"
                else -> null
            },
            inflow = flowError(inflow),
            outflow = flowError(outflow)
        )
    }

    private fun flowError(raw: String): String? {
        val value = raw.replace(',', '.').toDoubleOrNull()
        return when {
            value == null -> "Введіть число (м³/с)"
            value < 0 -> "Витрата не може бути від'ємною"
            value > MAX_FLOW_M3S -> "Нереальне значення (> 25 000 м³/с)"
            else -> null
        }
    }
}
