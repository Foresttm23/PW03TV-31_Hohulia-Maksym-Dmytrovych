package com.example.mse.ui.screens.task10

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mse.model.CascadeSummary
import com.example.mse.model.DataOrigin
import com.example.mse.model.FloodRisk
import com.example.mse.model.StationForecast
import com.example.mse.model.StationSort
import com.example.mse.ui.components.LabeledValue
import com.example.mse.ui.components.SectionTitle
import com.example.mse.ui.components.StateColors
import com.example.mse.ui.components.StatusPill

@Composable
fun CascadeForecastScreen(viewModel: CascadeForecastViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 340.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        fullWidth { HeaderCard(state, onRefresh = { viewModel.load(forceRefresh = true) }) }

        state.errorMessage?.let { message ->
            fullWidth { ErrorCard(message, onRetry = { viewModel.load(forceRefresh = true) }) }
        }

        state.forecast?.let { forecast ->
            fullWidth { SummaryCard(forecast.summary) }
            fullWidth { FilterBar(state, viewModel) }
            if (forecast.failures.isNotEmpty()) {
                fullWidth {
                    Text(
                        "Без даних: " + forecast.failures.joinToString { "${it.station.name} — ${it.message}" },
                        color = StateColors.Danger,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            items(state.visibleStations, key = { it.station.id }) { station ->
                StationCard(
                    forecast = station,
                    expanded = station.station.id in state.expandedIds,
                    onToggle = { viewModel.toggleExpanded(station.station.id) }
                )
            }
            if (state.visibleStations.isEmpty()) {
                fullWidth { Text("Жодна станція не відповідає фільтрам", Modifier.padding(16.dp)) }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeaderCard(state: CascadeForecastUiState, onRefresh: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Прогноз притоку каскаду ГЕС", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "REST: Open-Meteo Flood API (GloFAS, витрата річки) + Weather API (опади). Каталог станцій — JSON у assets.",
                style = MaterialTheme.typography.bodySmall
            )
            if (state.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                state.progressText?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
            }
            FlowRow(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.forecast?.let { f ->
                    StatusPill(
                        "Джерело: ${f.origin.title}",
                        when (f.origin) {
                            DataOrigin.NETWORK -> StateColors.Ok
                            DataOrigin.MEMORY_CACHE -> StateColors.Info
                            else -> StateColors.Warning
                        },
                        icon = if (f.origin == DataOrigin.OFFLINE_CACHE) Icons.Default.CloudOff else null
                    )
                }
                Button(onClick = onRefresh, enabled = !state.isLoading) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Оновити")
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WarningAmber, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(10.dp))
            Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            OutlinedButton(onClick = onRetry) { Text("Повторити") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryCard(summary: CascadeSummary) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Зведення по каскаду", "Станцій: ${summary.stationsCount} · найвищий ризик: ${summary.riskiestStation ?: "—"}")
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                LabeledValue("Сумарний приплив", "%.0f м³/с".format(summary.totalInflowM3s))
                LabeledValue("Потужність зараз", "%.1f МВт".format(summary.totalPowerMw))
                LabeledValue("Виробіток 7 діб", "%.2f ГВт·год".format(summary.weeklyEnergyGwh))
            }
            Text(
                "Використання встановленої потужності (%.0f МВт): %.1f%%".format(
                    summary.totalInstalledMw,
                    if (summary.totalInstalledMw > 0) summary.totalPowerMw / summary.totalInstalledMw * 100 else 0.0
                ),
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FloodRisk.entries.forEach { r ->
                    StatusPill("${r.title}: ${summary.riskCounts[r] ?: 0}", riskColor(r))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterBar(state: CascadeForecastUiState, viewModel: CascadeForecastViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(state.riverFilter == null, { viewModel.onRiverFilter(null) }, { Text("Усі річки") })
            state.rivers.forEach { river ->
                FilterChip(state.riverFilter == river, { viewModel.onRiverFilter(if (state.riverFilter == river) null else river) }, { Text(river) })
            }
            FloodRisk.entries.forEach { risk ->
                FilterChip(state.riskFilter == risk, { viewModel.onRiskFilter(if (state.riskFilter == risk) null else risk) }, { Text("Ризик: ${risk.title}") })
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StationSort.entries.forEach { sort ->
                FilterChip(state.sort == sort, { viewModel.onSort(sort) }, { Text(sort.title) })
            }
        }
    }
}

@Composable
private fun StationCard(forecast: StationForecast, expanded: Boolean, onToggle: () -> Unit) {
    val color = riskColor(forecast.risk)
    Card(Modifier.fillMaxWidth().clickable(onClick = onToggle), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(forecast.station.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${forecast.station.river} · ${forecast.station.city} · H=${forecast.station.designHeadM} м", style = MaterialTheme.typography.labelSmall)
                }
                StatusPill(forecast.risk.title, color)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LabeledValue("Витрата", "%.0f м³/с".format(forecast.currentDischargeM3s))
                LabeledValue("Пік 7 діб", "%.0f".format(forecast.peakDischargeM3s))
                LabeledValue("Потужність", "%.1f МВт".format(forecast.currentPowerMw))
                LabeledValue("Опади", "%.1f мм".format(forecast.precipitationWeekMm))
            }
            DischargeChart(forecast, color)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val trend = forecast.trendPercentPerDay
                Icon(
                    when {
                        trend > 1 -> Icons.AutoMirrored.Filled.TrendingUp
                        trend < -1 -> Icons.AutoMirrored.Filled.TrendingDown
                        else -> Icons.AutoMirrored.Filled.TrendingFlat
                    },
                    null, tint = color
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Тренд %+.1f%%/добу · індекс ризику %.2f · КВВП %.1f%%".format(trend, forecast.riskScore, forecast.capacityFactor * 100),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(forecast.recommendation, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = color)
            if (forecast.origin == DataOrigin.OFFLINE_CACHE) {
                val ageMin = (System.currentTimeMillis() - forecast.fetchedAt) / 60_000
                StatusPill("Офлайн-кеш, вік $ageMin хв", StateColors.Warning, icon = Icons.Default.CloudOff)
            }
            if (forecast.droppedInvalidDays > 0) {
                Text("Відкинуто некоректних днів: ${forecast.droppedInvalidDays}", color = StateColors.Warning, style = MaterialTheme.typography.labelSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (expanded) "Сховати добовий прогноз" else "Показати добовий прогноз", style = MaterialTheme.typography.labelMedium)
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
            AnimatedVisibility(expanded) { DailyTable(forecast) }
        }
    }
}

@Composable
private fun DischargeChart(forecast: StationForecast, color: Color) {
    val maxQ = forecast.days.maxOf { it.dischargeMaxM3s }.coerceAtLeast(1.0)
    val maxP = forecast.days.maxOf { it.precipitationMm }.coerceAtLeast(1.0)
    val rainColor = StateColors.Info
    Column {
        Canvas(Modifier.fillMaxWidth().height(64.dp)) {
            val n = forecast.days.size
            val slot = size.width / n
            val barWidth = slot * 0.55f
            forecast.days.forEachIndexed { i, d ->
                val h = (d.dischargeM3s / maxQ * size.height).toFloat()
                drawRoundRect(
                    color = color.copy(alpha = 0.75f),
                    topLeft = Offset(i * slot + (slot - barWidth) / 2, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                if (d.precipitationMm > 0) {
                    drawCircle(
                        color = rainColor,
                        radius = (4 + 6 * d.precipitationMm / maxP).toFloat(),
                        center = Offset(i * slot + slot / 2, 10f)
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            forecast.days.forEach {
                Text(it.date.takeLast(5), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun DailyTable(forecast: StationForecast) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        HorizontalDivider()
        TableRow("Дата", "Q, м³/с", "Опади", "T, °C", "P, МВт", header = true)
        forecast.days.forEach { d ->
            TableRow(
                d.date.takeLast(5),
                "%.0f".format(d.dischargeM3s),
                "%.1f".format(d.precipitationMm),
                if (d.tempMinC != null && d.tempMaxC != null) "%.0f…%.0f".format(d.tempMinC, d.tempMaxC) else "—",
                "%.1f".format(d.powerMw)
            )
        }
        Text("Агрегати: ${forecast.station.unitsDescription}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun TableRow(vararg cells: String, header: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        cells.forEach {
            Text(
                it,
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
                fontFamily = if (header) FontFamily.Default else FontFamily.Monospace
            )
        }
    }
}

private fun riskColor(risk: FloodRisk): Color = when (risk) {
    FloodRisk.LOW -> StateColors.Ok
    FloodRisk.MODERATE -> StateColors.Info
    FloodRisk.HIGH -> StateColors.Warning
    FloodRisk.CRITICAL -> StateColors.Danger
}
