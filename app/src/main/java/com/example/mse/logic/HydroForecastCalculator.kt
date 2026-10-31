package com.example.mse.logic

import com.example.mse.data.remote.dto.FloodResponseDto
import com.example.mse.data.remote.dto.StationCatalogDto
import com.example.mse.data.remote.dto.WeatherResponseDto
import com.example.mse.model.CascadeSummary
import com.example.mse.model.DailyHydroForecast
import com.example.mse.model.DataOrigin
import com.example.mse.model.FloodRisk
import com.example.mse.model.HydroStation
import com.example.mse.model.StationForecast
import com.example.mse.model.StationSort

class InvalidForecastDataException(message: String) : Exception(message)

data class ValidatedDay(
    val date: String,
    val discharge: Double,
    val dischargeMax: Double,
    val dischargeMin: Double,
    val precipitation: Double,
    val tempMax: Double?,
    val tempMin: Double?,
    val snowfall: Double
)

object ForecastValidator {
    private const val MAX_PLAUSIBLE_DISCHARGE = 60_000.0

    fun parseCatalog(dto: StationCatalogDto?): List<HydroStation> =
        dto?.stations.orEmpty().mapNotNull { s ->
            val loc = s.location ?: return@mapNotNull null
            val hyd = s.hydraulics ?: return@mapNotNull null
            val installed = s.units.orEmpty().sumOf { it.capacityMw ?: 0.0 }
            HydroStation(
                id = s.id ?: return@mapNotNull null,
                name = s.name ?: return@mapNotNull null,
                river = s.river ?: "—",
                city = loc.city ?: "—",
                lat = loc.lat ?: return@mapNotNull null,
                lon = loc.lon ?: return@mapNotNull null,
                designHeadM = hyd.designHeadM?.takeIf { it > 0 } ?: return@mapNotNull null,
                normFlowM3s = hyd.normFlowM3s?.takeIf { it > 0 } ?: return@mapNotNull null,
                efficiency = (hyd.efficiency ?: 0.9).coerceIn(0.5, 0.97),
                installedMw = installed,
                unitsDescription = s.units.orEmpty().joinToString { "${it.name} (${it.type})" }
            )
        }

    fun combine(flood: FloodResponseDto, weather: WeatherResponseDto?): Pair<List<ValidatedDay>, Int> {
        val daily = flood.daily ?: throw InvalidForecastDataException("У відповіді немає блоку daily")
        val dates = daily.time.orEmpty()
        if (dates.isEmpty()) throw InvalidForecastDataException("Порожній масив дат")

        val w = weather?.daily
        val weatherIndex = w?.time.orEmpty().withIndex().associate { (i, d) -> d to i }

        var dropped = 0
        val days = dates.mapIndexedNotNull { i, date ->
            val q = daily.discharge?.getOrNull(i)
            if (q == null || q < 0 || q > MAX_PLAUSIBLE_DISCHARGE) {
                dropped++
                return@mapIndexedNotNull null
            }
            val wi = weatherIndex[date]
            ValidatedDay(
                date = date,
                discharge = q,
                dischargeMax = daily.dischargeMax?.getOrNull(i)?.takeIf { it >= q } ?: q,
                dischargeMin = daily.dischargeMin?.getOrNull(i)?.takeIf { it in 0.0..q } ?: q,
                precipitation = wi?.let { w?.precipitationSum?.getOrNull(it) }?.coerceAtLeast(0.0) ?: 0.0,
                tempMax = wi?.let { w?.temperatureMax?.getOrNull(it) },
                tempMin = wi?.let { w?.temperatureMin?.getOrNull(it) },
                snowfall = wi?.let { w?.snowfallSum?.getOrNull(it) }?.coerceAtLeast(0.0) ?: 0.0
            )
        }
        if (days.isEmpty()) throw InvalidForecastDataException("Усі ${dates.size} днів містять некоректні значення витрати")
        return days to dropped
    }
}

object HydroForecastCalculator {
    private const val RHO = 1000.0
    private const val G = 9.81

    fun powerMw(dischargeM3s: Double, headM: Double, efficiency: Double, installedMw: Double): Double {
        val raw = efficiency * RHO * G * dischargeM3s * headM / 1_000_000.0
        return if (installedMw > 0) raw.coerceAtMost(installedMw) else raw
    }

    fun riskLevel(score: Double): FloodRisk = when {
        score < 1.0 -> FloodRisk.LOW
        score < 1.25 -> FloodRisk.MODERATE
        score < 1.5 -> FloodRisk.HIGH
        else -> FloodRisk.CRITICAL
    }

    fun build(
        station: HydroStation,
        days: List<ValidatedDay>,
        origin: DataOrigin,
        fetchedAt: Long,
        droppedDays: Int
    ): StationForecast {
        val forecastDays = days.map { d ->
            val power = powerMw(d.discharge, station.designHeadM, station.efficiency, station.installedMw)
            DailyHydroForecast(
                date = d.date,
                dischargeM3s = d.discharge,
                dischargeMaxM3s = d.dischargeMax,
                dischargeMinM3s = d.dischargeMin,
                precipitationMm = d.precipitation,
                tempMaxC = d.tempMax,
                tempMinC = d.tempMin,
                snowfallCm = d.snowfall,
                powerMw = power,
                energyMwh = power * 24
            )
        }
        val mean = forecastDays.map { it.dischargeM3s }.average()
        val peak = forecastDays.maxOf { it.dischargeMaxM3s }
        val precipitation = forecastDays.sumOf { it.precipitationMm }
        val trend = if (mean > 0) slope(forecastDays.map { it.dischargeM3s }) / mean * 100 else 0.0
        // Індекс ризику: піковий приплив / норма + опади (60 мм за тиждень = +1.0), сніг як відкладений стік ×0.5.
        val score = peak / station.normFlowM3s + (precipitation + forecastDays.sumOf { it.snowfallCm } * 0.5) / 60.0
        val risk = riskLevel(score)
        val avgPower = forecastDays.map { it.powerMw }.average()

        return StationForecast(
            station = station,
            days = forecastDays,
            currentDischargeM3s = forecastDays.first().dischargeM3s,
            peakDischargeM3s = peak,
            meanDischargeM3s = mean,
            trendPercentPerDay = trend,
            currentPowerMw = forecastDays.first().powerMw,
            weeklyEnergyMwh = forecastDays.sumOf { it.energyMwh },
            capacityFactor = if (station.installedMw > 0) avgPower / station.installedMw else 0.0,
            precipitationWeekMm = precipitation,
            riskScore = score,
            risk = risk,
            recommendation = recommendation(risk, trend),
            origin = origin,
            fetchedAt = fetchedAt,
            droppedInvalidDays = droppedDays
        )
    }

    fun recommendation(risk: FloodRisk, trendPercent: Double): String = when {
        risk == FloodRisk.CRITICAL -> "Аварійна готовність: передпаводкове спрацювання, відкриття водозливу"
        risk == FloodRisk.HIGH -> "Збільшити скид і навантаження агрегатів, звільнити резерв ємності"
        trendPercent > 3 -> "Приплив зростає — поступово підвищувати навантаження ГА"
        trendPercent < -3 -> "Приплив спадає — режим накопичення, робота в пікові години"
        else -> "Штатний режим, добове регулювання"
    }

    fun slope(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val meanX = (values.size - 1) / 2.0
        val meanY = values.average()
        var num = 0.0
        var den = 0.0
        values.forEachIndexed { i, y ->
            num += (i - meanX) * (y - meanY)
            den += (i - meanX) * (i - meanX)
        }
        return num / den
    }

    fun summarize(forecasts: List<StationForecast>): CascadeSummary = CascadeSummary(
        stationsCount = forecasts.size,
        totalPowerMw = forecasts.sumOf { it.currentPowerMw },
        totalInstalledMw = forecasts.sumOf { it.station.installedMw },
        weeklyEnergyGwh = forecasts.sumOf { it.weeklyEnergyMwh } / 1000.0,
        totalInflowM3s = forecasts.sumOf { it.currentDischargeM3s },
        riskiestStation = forecasts.maxByOrNull { it.riskScore }?.station?.name,
        riskCounts = FloodRisk.entries.associateWith { r -> forecasts.count { it.risk == r } }
    )

    fun sort(forecasts: List<StationForecast>, sort: StationSort): List<StationForecast> = when (sort) {
        StationSort.RISK -> forecasts.sortedByDescending { it.riskScore }
        StationSort.DISCHARGE -> forecasts.sortedByDescending { it.currentDischargeM3s }
        StationSort.POWER -> forecasts.sortedByDescending { it.currentPowerMw }
        StationSort.NAME -> forecasts.sortedBy { it.station.name }
    }
}
