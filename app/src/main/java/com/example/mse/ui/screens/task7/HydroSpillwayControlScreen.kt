package com.example.mse.ui.screens.task7

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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mse.ui.theme.MSETheme
import java.util.Locale

/** Екран регулювання холостих затворів на основі MVVM з ViewModel та StateFlow (Task 7 - Практична 7) */
@Composable
fun HydroSpillwayControlScreen(
    modifier: Modifier = Modifier
) {
    val viewModel: HydroSpillwayViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    HydroSpillwayControlContent(
        modifier = modifier,
        gate1Opening = uiState.gate1Opening,
        gate2Opening = uiState.gate2Opening,
        gate3Opening = uiState.gate3Opening,
        gate4Opening = uiState.gate4Opening,
        isEmergencyActive = uiState.isEmergencyActive,
        totalSpillwayFlowM3 = uiState.totalSpillwayFlowM3,
        downstreamSafetyStatus = uiState.downstreamSafetyStatus,
        onGate1Change = { viewModel.updateGate1(it) },
        onGate2Change = { viewModel.updateGate2(it) },
        onGate3Change = { viewModel.updateGate3(it) },
        onGate4Change = { viewModel.updateGate4(it) },
        onEmergencyToggle = { viewModel.toggleEmergencyRelease() }
    )
}

@Composable
fun HydroSpillwayControlContent(
    gate1Opening: Float,
    gate2Opening: Float,
    gate3Opening: Float,
    gate4Opening: Float,
    isEmergencyActive: Boolean,
    totalSpillwayFlowM3: Double,
    downstreamSafetyStatus: String,
    onGate1Change: (Float) -> Unit,
    onGate2Change: (Float) -> Unit,
    onGate3Change: (Float) -> Unit,
    onGate4Change: (Float) -> Unit,
    onEmergencyToggle: () -> Unit,
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
            HydroSpillwayTitle()

            Spacer(modifier = Modifier.height(16.dp))

            DownstreamSafetyBanner(
                totalFlow = totalSpillwayFlowM3,
                safetyStatus = downstreamSafetyStatus
            )

            Spacer(modifier = Modifier.height(16.dp))

            SpillwayGateRegulatorCard(
                gate1 = gate1Opening, onGate1Change = onGate1Change,
                gate2 = gate2Opening, onGate2Change = onGate2Change,
                gate3 = gate3Opening, onGate3Change = onGate3Change,
                gate4 = gate4Opening, onGate4Change = onGate4Change
            )

            Spacer(modifier = Modifier.height(20.dp))

            EmergencyReleaseActionCard(
                isEmergencyActive = isEmergencyActive,
                onEmergencyToggle = onEmergencyToggle
            )
        }
    }
}

/** Заголовок завдання 7 */
@Composable
private fun HydroSpillwayTitle() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Практична 7: Архітектура MVVM",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Контролер Затворів ГЕС (ViewModel & StateFlow)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Управління станом UI через ViewModel та реактивний потік StateFlow",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Баннер безпеки нижнього б'єфу річки */
@Composable
private fun DownstreamSafetyBanner(
    totalFlow: Double,
    safetyStatus: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Water,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Сумарне холосте скидання: ${String.format(Locale.getDefault(), "%.0f", totalFlow)} м³/с",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Стан нижнього б'єфу: $safetyStatus",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
            )
        }
    }
}

/** Картка регулювання 4 затворів холостого скиду */
@Composable
private fun SpillwayGateRegulatorCard(
    gate1: Float, onGate1Change: (Float) -> Unit,
    gate2: Float, onGate2Change: (Float) -> Unit,
    gate3: Float, onGate3Change: (Float) -> Unit,
    gate4: Float, onGate4Change: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Регулювання ступеня відкриття затворів (StateFlow):",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (this.maxWidth > 550.dp) {
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            GateSliderItem(title = "Затвор №1", value = gate1, onValueChange = onGate1Change, modifier = Modifier.weight(1f))
                            GateSliderItem(title = "Затвор №2", value = gate2, onValueChange = onGate2Change, modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            GateSliderItem(title = "Затвор №3", value = gate3, onValueChange = onGate3Change, modifier = Modifier.weight(1f))
                            GateSliderItem(title = "Затвор №4", value = gate4, onValueChange = onGate4Change, modifier = Modifier.weight(1f))
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        GateSliderItem(title = "Затвор №1", value = gate1, onValueChange = onGate1Change, modifier = Modifier.fillMaxWidth())
                        GateSliderItem(title = "Затвор №2", value = gate2, onValueChange = onGate2Change, modifier = Modifier.fillMaxWidth())
                        GateSliderItem(title = "Затвор №3", value = gate3, onValueChange = onGate3Change, modifier = Modifier.fillMaxWidth())
                        GateSliderItem(title = "Затвор №4", value = gate4, onValueChange = onGate4Change, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

/** Елемент регулятора одного затвора зі слайдером */
@Composable
private fun GateSliderItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${value.toInt()}%",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            steps = 19
        )
    }
}

/** Картка активації аварійного максимального скиду */
@Composable
private fun EmergencyReleaseActionCard(
    isEmergencyActive: Boolean,
    onEmergencyToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEmergencyActive) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isEmergencyActive) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Аварійний протокол скидання води ГЕС",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isEmergencyActive) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onEmergencyToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEmergencyActive) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = if (isEmergencyActive) "СКИНИТИ АВАРІЙНИЙ РЕЖИМ (ЗАКРИТИ)" else "АКТИВУВАТИ МАКСИМАЛЬНЕ ХОЛОСТЕ СКИДАННЯ (100%)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HydroSpillwayControlScreenPreview() {
    MSETheme {
        HydroSpillwayControlContent(
            gate1Opening = 20f,
            gate2Opening = 0f,
            gate3Opening = 40f,
            gate4Opening = 0f,
            isEmergencyActive = false,
            totalSpillwayFlowM3 = 150.0,
            downstreamSafetyStatus = "Нормальний рівень річки в нижньому б'єфі",
            onGate1Change = {},
            onGate2Change = {},
            onGate3Change = {},
            onGate4Change = {},
            onEmergencyToggle = {}
        )
    }
}
