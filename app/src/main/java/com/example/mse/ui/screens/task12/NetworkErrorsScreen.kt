package com.example.mse.ui.screens.task12

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mse.model.FaultScenario
import com.example.mse.model.StationTelemetryResponse
import com.example.mse.ui.components.LabeledValue
import com.example.mse.ui.components.SectionTitle
import com.example.mse.ui.components.StateColors
import com.example.mse.ui.components.StatusPill

@Composable
fun NetworkErrorsScreen(
    viewModel: NetworkErrorsViewModel = viewModel(
        factory = viewModelFactory { initializer { NetworkErrorsViewModel.create(this[APPLICATION_KEY]!!) } }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarFlow.collect { event ->
            val result = snackbar.showSnackbar(
                message = event.message,
                actionLabel = if (event.retryable) "Повторити" else null,
                duration = if (event.retryable) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.fetch()
        }
    }

    // Системні відступи вже враховує зовнішній Scaffold у MainActivity.
    Scaffold(contentWindowInsets = WindowInsets(0), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            if (maxWidth >= 720.dp) {
                Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(0.55f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        RequestCard(state, viewModel)
                        ResultCard(state, viewModel)
                    }
                    Column(Modifier.weight(0.45f).verticalScroll(rememberScrollState())) { LogCard(state, viewModel) }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RequestCard(state, viewModel)
                    ResultCard(state, viewModel)
                    LogCard(state, viewModel)
                }
            }
        }
    }

    state.dialogError?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            icon = { Icon(Icons.Default.ErrorOutline, null, tint = StateColors.Danger) },
            title = { Text(error.title) },
            text = {
                Column {
                    Text(error.message)
                    Spacer(Modifier.height(8.dp))
                    Text("Тип: ${error.type}" + (error.httpCode?.let { " · HTTP $it" } ?: "") + " · повтор не допоможе", style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = { TextButton(onClick = viewModel::dismissDialog) { Text("Зрозуміло") } }
        )
    }
}

@Composable
private fun RequestCard(state: NetworkErrorsUiState, viewModel: NetworkErrorsViewModel) {
    var menuOpen by remember { mutableStateOf(false) }
    val online = state.isDeviceOnline && !state.simulateOffline
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Діагностика запитів SCADA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(
                    if (online) "Мережа доступна" else "Немає мережі",
                    if (online) StateColors.Ok else StateColors.Danger,
                    icon = if (online) Icons.Default.Wifi else Icons.Default.SignalWifiOff
                )
                Spacer(Modifier.weight(1f))
                Text("Імітувати офлайн", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(6.dp))
                Switch(checked = state.simulateOffline, onCheckedChange = viewModel::onSimulateOfflineChange)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.stationCode,
                    onValueChange = viewModel::onStationCodeChange,
                    label = { Text("Код гідропоста") },
                    isError = state.stationCodeError != null,
                    supportingText = { Text(state.stationCodeError ?: "KYV-01, KNV-01, KRM-01, DNS-01") },
                    singleLine = true,
                    modifier = Modifier.weight(0.6f)
                )
                OutlinedTextField(
                    value = state.hours,
                    onValueChange = viewModel::onHoursChange,
                    label = { Text("Період, год") },
                    isError = state.hoursError != null,
                    supportingText = { Text(state.hoursError ?: "1–72") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.4f)
                )
            }
            Text("Сценарій mocked-сервера", style = MaterialTheme.typography.labelMedium)
            Column {
                OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(state.scenario.title, Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    FaultScenario.entries.forEach { s ->
                        DropdownMenuItem(text = { Text(s.title) }, onClick = { viewModel.onScenarioChange(s); menuOpen = false })
                    }
                }
            }
            Button(onClick = viewModel::fetch, enabled = state.request !is RequestState.Loading, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CloudDownload, null)
                Spacer(Modifier.width(6.dp))
                Text("Отримати телеметрію")
            }
        }
    }
}

@Composable
private fun ResultCard(state: NetworkErrorsUiState, viewModel: NetworkErrorsViewModel) {
    when (val request = state.request) {
        RequestState.Idle -> Unit
        is RequestState.Loading -> Card {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.width(28.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Виконується запит… спроба ${request.attempt}/${request.maxAttempts}", fontWeight = FontWeight.SemiBold)
                    request.retryInfo?.let { Text(it, color = StateColors.Warning, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        is RequestState.Failed -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, null, tint = StateColors.Danger)
                    Spacer(Modifier.width(8.dp))
                    Text(request.error.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    request.error.httpCode?.let { StatusPill("HTTP $it", StateColors.Danger) }
                }
                Text(request.error.message)
                Text(
                    "Спроб виконано: ${request.attempts} · " + if (request.error.isRetryable) "тимчасова помилка" else "постійна помилка — повтори не виконувались",
                    style = MaterialTheme.typography.labelSmall
                )
                if (request.error.isRetryable) {
                    OutlinedButton(onClick = viewModel::fetch) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Повторити") }
                }
            }
        }
        is RequestState.Success -> TelemetryCard(request.data, request.attempts)
    }
}

@Composable
private fun TelemetryCard(data: StationTelemetryResponse, attempts: Int) {
    val station = data.station!!
    val reservoir = data.reservoir!!
    val flows = data.flows!!
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = StateColors.Ok)
                Spacer(Modifier.width(8.dp))
                Text("${station.name} (${station.code}) · ${station.river}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                StatusPill(if (attempts > 1) "з $attempts-ї спроби" else "1 спроба", if (attempts > 1) StateColors.Warning else StateColors.Ok)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LabeledValue("Рівень ВБ", "%.2f м".format(reservoir.levelM), valueColor = if (reservoir.levelM > reservoir.normalLevelM) StateColors.Warning else StateColors.Ok)
                LabeledValue("НПУ", "%.1f м".format(reservoir.normalLevelM))
                LabeledValue("Об'єм", "%.0f млн м³".format(reservoir.volumeMcm))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LabeledValue("Приплив", "%.0f м³/с".format(flows.inflowM3s))
                LabeledValue("Турбіни", "%.0f м³/с".format(flows.turbineM3s))
                LabeledValue("Водозлив", "%.0f м³/с".format(flows.spillwayM3s))
            }
            val history = data.history.orEmpty()
            if (history.size > 1) {
                Text("Рівень за останні ${history.last().hoursAgo} год", style = MaterialTheme.typography.labelSmall)
                val minL = history.minOf { it.levelM }
                val maxL = history.maxOf { it.levelM }.coerceAtLeast(minL + 0.01)
                val lineColor = StateColors.Info
                Canvas(Modifier.fillMaxWidth().height(56.dp)) {
                    val points = history.reversed().mapIndexed { i, p ->
                        Offset(i * size.width / (history.size - 1), (size.height * (1 - (p.levelM - minL) / (maxL - minL))).toFloat())
                    }
                    points.zipWithNext().forEach { (a, b) -> drawLine(lineColor, a, b, strokeWidth = 5f, cap = StrokeCap.Round) }
                }
            }
            data.units.orEmpty().forEach { u ->
                Row {
                    Text(u.unit, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text("%.1f МВт".format(u.powerMw), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text(u.state, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogCard(state: NetworkErrorsUiState, viewModel: NetworkErrorsViewModel) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Журнал помилок (файл + Logcat)", "Записів: ${state.logLines.size}", Modifier.weight(1f))
                IconButton(onClick = viewModel::refreshLog) { Icon(Icons.Default.Refresh, "Оновити журнал") }
                IconButton(onClick = viewModel::clearLog) { Icon(Icons.Default.DeleteSweep, "Очистити журнал") }
            }
            Text(state.logPath, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
            if (state.errorStats.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.errorStats.forEach { (type, count) -> StatusPill("$type × $count", StateColors.Warning) }
                }
            }
            HorizontalDivider()
            if (state.logLines.isEmpty()) Text("Помилок не зареєстровано", style = MaterialTheme.typography.bodySmall)
            state.logLines.take(15).forEach { line ->
                Text(
                    line.substringAfter(' '),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (line.contains("| RETRY |")) StateColors.Warning else StateColors.Danger,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
