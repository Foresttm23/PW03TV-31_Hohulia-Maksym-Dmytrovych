package com.example.mse.ui.screens.task6

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mse.logic.HydroOperationsLogic
import com.example.mse.logic.WaterBalanceResult
import com.example.mse.ui.components.MetricCard
import com.example.mse.ui.theme.MSETheme
import java.util.Locale

/** Екран розробки форм із валідацією введених даних та підказками (Task 6 - Практична 6) */
@Composable
fun HydroWaterControlScreen(
    modifier: Modifier = Modifier
) {
    var inflowInput by remember { mutableStateOf("520.0") }
    var turbineInput by remember { mutableStateOf("320.0") }
    var spillwayInput by remember { mutableStateOf("50.0") }
    var currentLevelInput by remember { mutableStateOf("122.8") }

    var inflowError by remember { mutableStateOf<String?>(null) }
    var turbineError by remember { mutableStateOf<String?>(null) }
    var spillwayError by remember { mutableStateOf<String?>(null) }
    var levelError by remember { mutableStateOf<String?>(null) }

    var balanceResult by remember {
        mutableStateOf(
            HydroOperationsLogic.calculateWaterBalance(
                inflowM3PerSec = 520.0,
                turbineFlowM3PerSec = 320.0,
                spillwayFlowM3PerSec = 50.0,
                currentWaterLevelMeters = 122.8
            )
        )
    }

    fun validateAndCalculate() {
        val inVal = inflowInput.toDoubleOrNull()
        val turbVal = turbineInput.toDoubleOrNull()
        val spillVal = spillwayInput.toDoubleOrNull()
        val lvlVal = currentLevelInput.toDoubleOrNull()

        inflowError = when {
            inVal == null -> "Введіть числове значення припливу"
            inVal < 0 -> "Приплив не може бути від'ємним"
            else -> null
        }

        turbineError = when {
            turbVal == null -> "Введіть числове значення витрати турбін"
            turbVal < 0 -> "Витрата турбін не може бути від'ємною"
            else -> null
        }

        spillwayError = when {
            spillVal == null -> "Введіть числове значення холостого скиду"
            spillVal < 0 -> "Скидання не може бути від'ємним"
            else -> null
        }

        levelError = when {
            lvlVal == null -> "Введіть рівень водосховища"
            lvlVal !in 100.0..130.0 -> "Рівень має бути в межах 100-130 м"
            else -> null
        }

        if (inflowError == null && turbineError == null && spillwayError == null && levelError == null) {
            balanceResult = HydroOperationsLogic.calculateWaterBalance(
                inflowM3PerSec = inVal!!,
                turbineFlowM3PerSec = turbVal!!,
                spillwayFlowM3PerSec = spillVal!!,
                currentWaterLevelMeters = lvlVal!!
            )
        }
    }

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
            HydroWaterControlTitle()

            Spacer(modifier = Modifier.height(16.dp))

            EmergencyWarningBanner(isEmergency = balanceResult.isEmergencyDischargeRecommended)

            Spacer(modifier = Modifier.height(16.dp))

            WaterBalanceInputForm(
                inflow = inflowInput,
                onInflowChange = { inflowInput = it; inflowError = null },
                inflowError = inflowError,
                turbine = turbineInput,
                onTurbineChange = { turbineInput = it; turbineError = null },
                turbineError = turbineError,
                spillway = spillwayInput,
                onSpillwayChange = { spillwayInput = it; spillwayError = null },
                spillwayError = spillwayError,
                level = currentLevelInput,
                onLevelChange = { currentLevelInput = it; levelError = null },
                levelError = levelError,
                onCalculateClick = { validateAndCalculate() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            WaterBalanceMetricsGrid(result = balanceResult)
        }
    }
}

/** Заголовок завдання 6 */
@Composable
private fun HydroWaterControlTitle() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Практична 6: Форми та Валідація",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Форма Контролю Водного Балансу ГЕС",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Інтерактивна форма введення з валідацією, помилками та підказками",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Аварійний баннер загрози переповнення */
@Composable
private fun EmergencyWarningBanner(isEmergency: Boolean) {
    AnimatedVisibility(
        visible = isEmergency,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFC62828)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = "УВАГА: Рекомендовано додаткове скидання води!",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                    Text(
                        text = "Приплив перевищує сумарний відплив при високому рівні водосховища.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB71C1C)
                    )
                }
            }
        }
    }
}

/** Форма згрупованих полів введення параметрів із підказками та повідомленнями про помилки */
@Composable
private fun WaterBalanceInputForm(
    inflow: String,
    onInflowChange: (String) -> Unit,
    inflowError: String?,
    turbine: String,
    onTurbineChange: (String) -> Unit,
    turbineError: String?,
    spillway: String,
    onSpillwayChange: (String) -> Unit,
    spillwayError: String?,
    level: String,
    onLevelChange: (String) -> Unit,
    levelError: String?,
    onCalculateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Форма введення показників гідровузла:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = inflow,
                onValueChange = onInflowChange,
                label = { Text("Приплив води Q_in (м³/с)") },
                placeholder = { Text("Наприклад: 500") },
                isError = inflowError != null,
                supportingText = {
                    Text(inflowError ?: "Підказка: вкажіть нормальний приплив (м³/с)")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = turbine,
                onValueChange = onTurbineChange,
                label = { Text("Витрата турбін Q_turb (м³/с)") },
                placeholder = { Text("Наприклад: 300") },
                isError = turbineError != null,
                supportingText = {
                    Text(turbineError ?: "Підказка: сумарний пропуск через гідроагрегати")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = spillway,
                onValueChange = onSpillwayChange,
                label = { Text("Холосте скидання Q_spill (м³/с)") },
                placeholder = { Text("Наприклад: 50") },
                isError = spillwayError != null,
                supportingText = {
                    Text(spillwayError ?: "Підказка: холостий пропуск затворів")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = level,
                onValueChange = onLevelChange,
                label = { Text("Поточний рівень H (м)") },
                placeholder = { Text("Наприклад: 122.5") },
                isError = levelError != null,
                supportingText = {
                    Text(levelError ?: "Підказка: виміряний рівень верхнього б'єфу")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCalculateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null)
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text("Перевірити та розрахувати баланс", fontSize = 16.sp)
            }
        }
    }
}

/** Відображення результатів аналізу у картках MetricCard */
@Composable
private fun WaterBalanceMetricsGrid(result: WaterBalanceResult) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Результати розрахунку водного балансу:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = "Чистий приплив ΔQ",
                value = String.format(Locale.getDefault(), "%+.1f", result.netFlowM3PerSec),
                unit = "м³/с",
                icon = Icons.Default.Waves,
                modifier = Modifier.weight(1f),
                containerColor = if (result.netFlowM3PerSec > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            )

            MetricCard(
                title = "Динаміка рівня ΔH",
                value = String.format(Locale.getDefault(), "%+.3f", result.hourlyLevelChangeMeters),
                unit = "м/год",
                icon = Icons.Default.Speed,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val hoursText = if (result.hoursToCriticalLevel < 0) "Стабільно" else String.format(Locale.getDefault(), "%.1f", result.hoursToCriticalLevel)
            val hoursUnit = if (result.hoursToCriticalLevel < 0) "" else "год"

            MetricCard(
                title = "Час до порогу НПУ",
                value = hoursText,
                unit = hoursUnit,
                icon = Icons.Default.Timer,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconTint = MaterialTheme.colorScheme.tertiary
            )

            MetricCard(
                title = "Статус системи",
                value = if (result.isEmergencyDischargeRecommended) "УВАГА" else "НОРМА",
                unit = "",
                icon = Icons.Default.Water,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HydroWaterControlScreenPreview() {
    MSETheme {
        HydroWaterControlScreen()
    }
}
