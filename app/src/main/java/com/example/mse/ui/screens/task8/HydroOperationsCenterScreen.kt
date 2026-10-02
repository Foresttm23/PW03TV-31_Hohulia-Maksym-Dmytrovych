package com.example.mse.ui.screens.task8

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mse.model.HydroOperatingMode
import com.example.mse.model.HydroRecordDisplayItem
import com.example.mse.ui.components.MetricCard
import com.example.mse.ui.theme.MSETheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Головний центр управління ГЕС з підключенням Room Database (Task 8 - Практична 8) */
@Composable
fun HydroOperationsCenterScreen(
    modifier: Modifier = Modifier
) {
    val viewModel: HydroOperationsViewModel = viewModel()
    val operatorName by remember { mutableStateOf("Максим Гогуля (Оператор ТВ-31)") }
    val stationMode by remember { mutableStateOf(HydroOperatingMode.NORMAL_FLOW) }
    var operatorNotesInput by remember { mutableStateOf("") }
    var generatedReport by remember { mutableStateOf<String?>(null) }

    val savedRecords by viewModel.savedRecords.collectAsState()

    val displayRecords = savedRecords.map { record ->
        val dateStr = SimpleDateFormat("HH:mm:ss dd.MM.yyyy", Locale.getDefault()).format(Date(record.timestamp))
        HydroRecordDisplayItem(
            id = record.id,
            timeFormatted = dateStr,
            levelText = "Рівень: ${record.waterLevelMeters} м | Потужність: ${record.generatedPowerMw} МВт",
            notesText = "Примітка: ${record.operatorNotes}"
        )
    }

    HydroOperationsCenterContent(
        modifier = modifier,
        operatorName = operatorName,
        stationMode = stationMode,
        operatorNotesInput = operatorNotesInput,
        onNotesChange = { operatorNotesInput = it },
        onSaveRecord = {
            viewModel.saveCurrentMetrics(
                waterLevel = 122.1f,
                powerMw = 48.5f,
                flowRate = 380.0f,
                mode = stationMode,
                notes = operatorNotesInput
            )
            operatorNotesInput = ""
        },
        savedRecords = displayRecords,
        onDeleteRecord = { viewModel.deleteRecord(it) },
        onClearAll = { viewModel.clearAllRecords() },
        generatedReport = generatedReport,
        onGenerateReport = {
            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            generatedReport = """
                --- ЗВІТ ДИСПЕТЧЕРА ГЕС ---
                Дата/Час: $timeStamp
                Черговий оператор: $operatorName
                Активний режим: ${stationMode.title}
                Рівень верхнього б'єфу: 122.1 м (НПУ: 122.0 м)
                Записів у Room DB: ${savedRecords.size}
                Індекс надійності: 98.5%
                --- КІНЕЦЬ ЗВІТУ ---
            """.trimIndent()
        }
    )
}

@Composable
fun HydroOperationsCenterContent(
    operatorName: String,
    stationMode: HydroOperatingMode,
    operatorNotesInput: String,
    onNotesChange: (String) -> Unit,
    onSaveRecord: () -> Unit,
    savedRecords: List<HydroRecordDisplayItem>,
    onDeleteRecord: (Long) -> Unit,
    onClearAll: () -> Unit,
    generatedReport: String?,
    onGenerateReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HydroOperationsTitle()

            Spacer(modifier = Modifier.height(16.dp))

            OperatorShiftCard(
                operatorName = operatorName,
                mode = stationMode
            )

            Spacer(modifier = Modifier.height(16.dp))

            StationHealthGaugeCard()

            Spacer(modifier = Modifier.height(16.dp))

            TelemetryOverviewGrid(mode = stationMode)

            Spacer(modifier = Modifier.height(20.dp))

            RoomDatabaseCard(
                notesInput = operatorNotesInput,
                onNotesChange = onNotesChange,
                onSaveRecord = onSaveRecord,
                savedRecords = savedRecords,
                onDeleteRecord = onDeleteRecord,
                onClearAll = onClearAll
            )

            Spacer(modifier = Modifier.height(20.dp))

            SafetyReportSection(
                reportText = generatedReport,
                onGenerateReport = onGenerateReport
            )
        }
    }
}

/** Заголовок завдання 8 */
@Composable
private fun HydroOperationsTitle() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Практична 8: Room Database",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Центр Диспетчеризації з Локальною БД",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Збереження та читання енергетичних параметрів ГЕС у Room SQLite DB",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Картка профілю та чергування оператора */
@Composable
private fun OperatorShiftCard(
    operatorName: String,
    mode: HydroOperatingMode
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Badge,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Черговий диспетчер:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = operatorName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Режим станції: ${mode.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/** Картка індексу надійності та здоров'я ГЕС */
@Composable
private fun StationHealthGaugeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.HealthAndSafety,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Індекс надійності обладнання ГЕС: 98.5%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
                Text(
                    text = "Всі 4 гідроагрегати та автовузли затворів працюють у нормі.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2E7D32)
                )
            }
        }
    }
}

/** Сітка телеметрії показників */
@Composable
private fun TelemetryOverviewGrid(mode: HydroOperatingMode) {
    val flowMultiplier = when (mode) {
        HydroOperatingMode.NORMAL_FLOW -> 1.0f
        HydroOperatingMode.PEAK_LOAD -> 1.4f
        HydroOperatingMode.FLOOD_CONTROL -> 1.8f
        HydroOperatingMode.ECO_MINIMUM -> 0.3f
    }
    val powerVal = String.format(Locale.getDefault(), "%.1f", 45.0f * flowMultiplier)
    val flowVal = String.format(Locale.getDefault(), "%.0f", 350.0f * flowMultiplier)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (this.maxWidth > 500.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Потужність генерації",
                    value = powerVal,
                    unit = "МВт",
                    icon = Icons.Default.ElectricBolt,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Витрата води",
                    value = flowVal,
                    unit = "м³/с",
                    icon = Icons.Default.Waves,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Потужність генерації",
                    value = powerVal,
                    unit = "МВт",
                    icon = Icons.Default.ElectricBolt,
                    modifier = Modifier.fillMaxWidth()
                )
                MetricCard(
                    title = "Витрата води",
                    value = flowVal,
                    unit = "м³/с",
                    icon = Icons.Default.Waves,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** Секція локальної бази даних Room Database */
@Composable
private fun RoomDatabaseCard(
    notesInput: String,
    onNotesChange: (String) -> Unit,
    onSaveRecord: () -> Unit,
    savedRecords: List<HydroRecordDisplayItem>,
    onDeleteRecord: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Room Database Сховище",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (savedRecords.isNotEmpty()) {
                    OutlinedButton(onClick = onClearAll) {
                        Text("Очистити БД", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notesInput,
                onValueChange = onNotesChange,
                label = { Text("Примітка диспетчера для збереження") },
                placeholder = { Text("Наприклад: Замір перед піковим навантаженням") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSaveRecord,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Зберегти поточні показники в Room DB")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Збережені записи у Room DB (${savedRecords.size}):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (savedRecords.isEmpty()) {
                Text(
                    text = "Записів у Room БД поки немає. Натисніть кнопку вище для збереження.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    savedRecords.forEach { record ->
                        RoomRecordItem(
                            record = record,
                            onDelete = { onDeleteRecord(record.id) }
                        )
                    }
                }
            }
        }
    }
}

/** Елемент збереженого запису Room БД */
@Composable
private fun RoomRecordItem(
    record: HydroRecordDisplayItem,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Запис #${record.id} (${record.timeFormatted})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = record.levelText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = record.notesText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Видалити",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/** Секція створення звітів безпеки */
@Composable
private fun SafetyReportSection(
    reportText: String?,
    onGenerateReport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Генератор Диспетчерського Звіту:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onGenerateReport,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Сформувати Звіт Безпеки ГЕС", fontSize = 15.sp)
            }

            AnimatedVisibility(
                visible = reportText != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Звіт успішно згенеровано:", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = reportText ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HydroOperationsCenterContentPreview() {
    MSETheme {
        HydroOperationsCenterContent(
            operatorName = "Максим Гогуля (Оператор ТВ-31)",
            stationMode = HydroOperatingMode.NORMAL_FLOW,
            operatorNotesInput = "",
            onNotesChange = {},
            onSaveRecord = {},
            savedRecords = listOf(
                HydroRecordDisplayItem(
                    id = 1,
                    timeFormatted = "12:00:00 24.02.2025",
                    levelText = "Рівень: 122.1 м | Потужність: 48.5 МВт",
                    notesText = "Примітка: Тестовий запис у Room DB"
                )
            ),
            onDeleteRecord = {},
            onClearAll = {},
            generatedReport = null,
            onGenerateReport = {}
        )
    }
}
