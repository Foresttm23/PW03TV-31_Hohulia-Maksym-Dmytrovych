package com.example.mse.ui.screens.task9

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mse.logic.GaugeReadingValidator
import com.example.mse.logic.ReadingFormErrors
import com.example.mse.model.DataOrigin
import com.example.mse.model.GaugeReading
import com.example.mse.model.LevelStatus
import com.example.mse.model.ReadingFilter
import com.example.mse.model.ReadingSort
import com.example.mse.model.Reservoir
import com.example.mse.model.ReservoirStats
import com.example.mse.model.SourceMode
import com.example.mse.ui.components.LabeledValue
import com.example.mse.ui.components.SectionTitle
import com.example.mse.ui.components.StateColors
import com.example.mse.ui.components.StatusPill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun ReservoirJournalScreen(viewModel: ReservoirJournalViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var editorTarget by remember { mutableStateOf<EditorTarget?>(null) }
    var deleteTarget by remember { mutableStateOf<GaugeReading?>(null) }

    LaunchedEffect(Unit) {
        viewModel.messageFlow.collect { snackbarHostState.showSnackbar(it) }
    }

    // Системні відступи вже враховує зовнішній Scaffold у MainActivity.
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editorTarget = EditorTarget.New },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Новий запис") }
            )
        }
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val wide = maxWidth >= 720.dp
            val onEdit: (GaugeReading) -> Unit = { editorTarget = EditorTarget.Existing(it) }
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.weight(0.42f).fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { ControlPanel(state, viewModel) }
                        item { SectionTitle("Аналітика водосховищ", "Агрегація по відфільтрованих записах") }
                        items(state.dashboard.stats, key = { it.reservoir.name }) { StatsCard(it, Modifier.fillMaxWidth()) }
                    }
                    LazyColumn(
                        modifier = Modifier.weight(0.58f).fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        journalItems(state, viewModel, onEdit) { deleteTarget = it }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { ControlPanel(state, viewModel) }
                    if (state.dashboard.stats.isNotEmpty()) {
                        item { SectionTitle("Аналітика водосховищ", "Гортайте горизонтально →") }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(state.dashboard.stats, key = { it.reservoir.name }) {
                                    StatsCard(it, Modifier.width(280.dp))
                                }
                            }
                        }
                    }
                    journalItems(state, viewModel, onEdit) { deleteTarget = it }
                }
            }
        }
    }

    editorTarget?.let { target ->
        ReadingEditorDialog(
            initial = (target as? EditorTarget.Existing)?.reading,
            onDismiss = { editorTarget = null },
            onSave = { reservoir, code, level, inflow, outflow ->
                val existing = (target as? EditorTarget.Existing)?.reading
                if (existing == null) {
                    viewModel.addReading(reservoir, code, level, inflow, outflow)
                } else {
                    viewModel.updateReading(
                        existing.copy(
                            reservoir = reservoir, gaugeCode = code,
                            waterLevelM = level, inflowM3s = inflow, outflowM3s = outflow
                        )
                    )
                }
                editorTarget = null
            }
        )
    }

    deleteTarget?.let { reading ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text("Видалити запис?") },
            text = { Text("${reading.gaugeCode} · ${reading.reservoir.title} · ${"%.2f".format(reading.waterLevelM)} м.\nСинхронізований запис буде видалено і на сервері.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteReading(reading); deleteTarget = null }) { Text("Видалити") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Скасувати") } }
        )
    }
}

private sealed interface EditorTarget {
    data object New : EditorTarget
    data class Existing(val reading: GaugeReading) : EditorTarget
}

private fun LazyListScope.journalItems(
    state: ReservoirJournalUiState,
    viewModel: ReservoirJournalViewModel,
    onEdit: (GaugeReading) -> Unit,
    onDelete: (GaugeReading) -> Unit
) {
    item { FilterPanel(state.filter, viewModel) }
    item {
        SectionTitle(
            "Журнал вимірювань",
            "Показано ${state.dashboard.readings.size} з ${state.dashboard.totalCount} · несинхронізовано: ${state.dashboard.unsyncedCount}"
        )
    }
    if (state.dashboard.readings.isEmpty()) {
        item {
            Text(
                if (state.dashboard.totalCount == 0) "Журнал порожній. Оновіть дані із сервера або додайте запис."
                else "Немає записів, що відповідають фільтрам.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
    items(state.dashboard.readings, key = { it.id }) { reading ->
        ReadingCard(reading, onEdit = { onEdit(reading) }, onDelete = { onDelete(reading) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlPanel(state: ReservoirJournalUiState, viewModel: ReservoirJournalViewModel) {
    val control = state.control
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Журнал гідропостів каскаду", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Repository агрегує Room, SCADA-сервер і кеш у пам'яті", style = MaterialTheme.typography.bodySmall)

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SourceMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = control.mode == mode,
                        onClick = { viewModel.onModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, SourceMode.entries.size)
                    ) { Text(mode.title, maxLines = 1) }
                }
            }
            Text(control.mode.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Нестабільна мережа (70% збоїв)", style = MaterialTheme.typography.bodyMedium)
                    Text("Звернень до сервера: ${state.networkRequests}", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = control.networkUnstable, onCheckedChange = viewModel::onNetworkUnstableChange)
            }

            if (control.isRefreshing) LinearProgressIndicator(Modifier.fillMaxWidth())

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                control.lastOrigin?.let { origin ->
                    StatusPill(
                        text = "Джерело: ${origin.title}",
                        color = when (origin) {
                            DataOrigin.NETWORK -> StateColors.Ok
                            DataOrigin.MEMORY_CACHE -> StateColors.Info
                            else -> StateColors.Neutral
                        }
                    )
                }
                control.lastRefreshAt?.let {
                    Text(timeFormat.format(Date(it)), style = MaterialTheme.typography.labelSmall)
                }
            }

            control.errorBanner?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WarningAmber, null, tint = StateColors.Danger)
                    Spacer(Modifier.width(8.dp))
                    Text(it, color = StateColors.Danger, style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = viewModel::refresh, enabled = !control.isRefreshing) {
                    Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Оновити")
                }
                OutlinedButton(onClick = viewModel::syncPending, enabled = state.dashboard.unsyncedCount > 0) {
                    Icon(Icons.Default.CloudSync, null); Spacer(Modifier.width(6.dp)); Text("Синхронізувати")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterPanel(filter: ReadingFilter, viewModel: ReservoirJournalViewModel) {
    var sortMenu by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = filter.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                label = { Text("Пошук за кодом гідропоста") }
            )
            Spacer(Modifier.width(8.dp))
            Column {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Filled.Sort, "Сортування") }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    ReadingSort.entries.forEach { sort ->
                        DropdownMenuItem(
                            text = { Text((if (sort == filter.sort) "✓ " else "") + sort.title) },
                            onClick = { viewModel.onSortChange(sort); sortMenu = false }
                        )
                    }
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = filter.reservoir == null, onClick = { viewModel.onReservoirFilter(null) }, label = { Text("Усі") })
            Reservoir.entries.forEach { r ->
                FilterChip(
                    selected = filter.reservoir == r,
                    onClick = { viewModel.onReservoirFilter(if (filter.reservoir == r) null else r) },
                    label = { Text(r.title) }
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LevelStatus.entries.forEach { s ->
                FilterChip(
                    selected = filter.status == s,
                    onClick = { viewModel.onStatusFilter(if (filter.status == s) null else s) },
                    label = { Text(s.title) }
                )
            }
        }
        Text("Сортування: ${filter.sort.title}", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StatsCard(stats: ReservoirStats, modifier: Modifier = Modifier) {
    val color = StateColors.forLevel(stats.latestStatus)
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${stats.reservoir.title} вдсх.", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                StatusPill(stats.latestStatus.title, color)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text("%.2f м".format(stats.latestLevel), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(
                    "НПУ %.1f · ФПУ %.1f".format(stats.reservoir.normalLevel, stats.reservoir.forcedLevel),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            LinearProgressIndicator(
                progress = { (stats.latestFillPercent / 100.0).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = color
            )
            Text("Корисне наповнення: %.0f%%".format(stats.latestFillPercent), style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledValue("Середній", "%.2f м".format(stats.avgLevel))
                LabeledValue("Мін/Макс", "%.2f/%.2f".format(stats.minLevel, stats.maxLevel))
                LabeledValue("Записів", "${stats.readingsCount}")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val trendIcon = when {
                    stats.trendMPerHour > 0.002 -> Icons.AutoMirrored.Filled.TrendingUp
                    stats.trendMPerHour < -0.002 -> Icons.AutoMirrored.Filled.TrendingDown
                    else -> Icons.AutoMirrored.Filled.TrendingFlat
                }
                Icon(trendIcon, null, tint = color)
                Spacer(Modifier.width(6.dp))
                Text(
                    "Тренд %+.1f см/год · баланс %+.0f м³/с".format(stats.trendMPerHour * 100, stats.avgBalanceM3s),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            val forecast = stats.hoursToNormalLevel
            Text(
                when {
                    forecast != null -> "Прогноз: досягнення НПУ через ≈ %.0f год".format(forecast)
                    stats.criticalCount > 0 -> "Перевищень НПУ/ФПУ у вибірці: ${stats.criticalCount}"
                    else -> "Перевищень НПУ не зафіксовано"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ReadingCard(reading: GaugeReading, onEdit: () -> Unit, onDelete: () -> Unit) {
    val color = StateColors.forLevel(reading.status)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (reading.isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    contentDescription = if (reading.isSynced) "Синхронізовано" else "Тільки локально",
                    tint = if (reading.isSynced) StateColors.Ok else StateColors.Warning,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("${reading.gaugeCode} · ${reading.reservoir.title}", fontWeight = FontWeight.Bold)
                    Text(timeFormat.format(Date(reading.timestamp)), style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Редагувати") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Видалити") }
            }
            HorizontalDivider(Modifier.padding(vertical = 6.dp, horizontal = 0.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LabeledValue("Рівень", "%.2f м".format(reading.waterLevelM), valueColor = color)
                LabeledValue("Приплив", "%.0f".format(reading.inflowM3s))
                LabeledValue("Скид", "%.0f".format(reading.outflowM3s))
                LabeledValue(
                    "Баланс",
                    "%+.0f".format(reading.balanceM3s),
                    valueColor = if (abs(reading.balanceM3s) < 1) StateColors.Neutral else if (reading.balanceM3s > 0) StateColors.Info else StateColors.Warning
                )
            }
            Spacer(Modifier.height(6.dp))
            StatusPill("${reading.status.title} · %.0f%%".format(reading.fillPercent), color)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadingEditorDialog(
    initial: GaugeReading?,
    onDismiss: () -> Unit,
    onSave: (Reservoir, String, Double, Double, Double) -> Unit
) {
    var reservoir by remember { mutableStateOf(initial?.reservoir ?: Reservoir.KYIV) }
    var code by remember { mutableStateOf(initial?.gaugeCode ?: "KYV-01") }
    var level by remember { mutableStateOf(initial?.waterLevelM?.toString() ?: "") }
    var inflow by remember { mutableStateOf(initial?.inflowM3s?.toString() ?: "") }
    var outflow by remember { mutableStateOf(initial?.outflowM3s?.toString() ?: "") }
    var errors by remember { mutableStateOf(ReadingFormErrors()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Новий запис гідропоста" else "Редагування ${initial.gaugeCode}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Reservoir.entries.forEach { r ->
                        FilterChip(
                            selected = reservoir == r,
                            onClick = {
                                reservoir = r
                                if (code.length >= 3) code = r.codePrefix + code.drop(3)
                            },
                            label = { Text(r.title) }
                        )
                    }
                }
                NumberField(code, { code = it.uppercase() }, "Код гідропоста", errors.gaugeCode, KeyboardType.Ascii)
                NumberField(level, { level = it }, "Рівень води, м (НПУ ${reservoir.normalLevel})", errors.level)
                NumberField(inflow, { inflow = it }, "Приплив, м³/с", errors.inflow)
                NumberField(outflow, { outflow = it }, "Скид (турбіни + водозлив), м³/с", errors.outflow)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                errors = GaugeReadingValidator.validate(reservoir, code, level, inflow, outflow)
                if (!errors.hasErrors) {
                    onSave(reservoir, code.trim(), level.toNum(), inflow.toNum(), outflow.toNum())
                }
            }) { Text("Зберегти") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}

@Composable
private fun NumberField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Decimal
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun String.toNum(): Double = replace(',', '.').toDouble()

private val timeFormat = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
