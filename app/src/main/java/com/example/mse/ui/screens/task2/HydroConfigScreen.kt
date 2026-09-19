package com.example.mse.ui.screens.task2

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ElectricBolt
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mse.logic.HydroCalculationResult
import com.example.mse.logic.HydroCalculator
import com.example.mse.logic.HydroCalculatorInput
import com.example.mse.ui.components.MetricCard
import java.util.Locale

@Composable
fun HydroConfigScreen(
    modifier: Modifier = Modifier
) {
    var headInput by remember { mutableStateOf("15.0") }
    var flowInput by remember { mutableStateOf("350.0") }
    var efficiencyInput by remember { mutableStateOf("88.0") }
    var hoursInput by remember { mutableStateOf("24.0") }

    var calculationResult by remember {
        mutableStateOf(
            HydroCalculator.calculateEnergy(
                HydroCalculatorInput(
                    waterHeadMeters = 15.0,
                    waterFlowM3PerSec = 350.0,
                    efficiencyPercentage = 88.0,
                    operatingHoursPerDay = 24.0
                )
            )
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HydroConfigTitle()

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Вхідні параметри гідровузла",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = headInput,
                        onValueChange = { headInput = it },
                        label = { Text("Напір води H (метри)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = flowInput,
                        onValueChange = { flowInput = it },
                        label = { Text("Витрата води Q (м³/с)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = efficiencyInput,
                        onValueChange = { efficiencyInput = it },
                        label = { Text("ККД гідроагрегату η (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = hoursInput,
                        onValueChange = { hoursInput = it },
                        label = { Text("Робочі години за добу (год)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val head = headInput.toDoubleOrNull() ?: 0.0
                            val flow = flowInput.toDoubleOrNull() ?: 0.0
                            val eff = efficiencyInput.toDoubleOrNull() ?: 88.0
                            val hours = hoursInput.toDoubleOrNull() ?: 24.0

                            calculationResult = HydroCalculator.calculateEnergy(
                                HydroCalculatorInput(
                                    waterHeadMeters = head,
                                    waterFlowM3PerSec = flow,
                                    efficiencyPercentage = eff,
                                    operatingHoursPerDay = hours
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Calculate, contentDescription = null)
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text("Розрахувати потужність ГЕС", fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            HydroCalculationResultsBlock(result = calculationResult)
        }
    }
}

@Composable
private fun HydroConfigTitle() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Завдання 2",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Калькулятор Генерації ГЕС",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = "Розрахунок за допомогою фізичної логіки HydroCalculator P = η·ρ·g·Q·H",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun HydroCalculationResultsBlock(result: HydroCalculationResult) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Результати обчислення:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = "Потужність ГЕС",
                value = String.format(Locale.getDefault(), "%.2f", result.powerMw),
                unit = "МВт",
                icon = Icons.Default.ElectricBolt,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Денний виробіток",
                value = String.format(Locale.getDefault(), "%.1f", result.dailyGenerationMwh),
                unit = "МВт·год",
                icon = Icons.Default.Waves,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.secondary
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = "Місячний виробіток",
                value = String.format(Locale.getDefault(), "%.0f", result.monthlyGenerationMwh),
                unit = "МВт·год",
                icon = Icons.Default.Calculate,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconTint = MaterialTheme.colorScheme.tertiary
            )

            MetricCard(
                title = "Об'єм води / добу",
                value = String.format(Locale.getDefault(), "%.0f", result.totalWaterVolumeM3 / 1000.0),
                unit = "тис. м³",
                icon = Icons.Default.Water,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
