package com.example.mse.pw1.ui.screens.task3

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mse.pw1.data.model.HydroOperatingMode
import com.example.mse.pw1.ui.components.MetricCard
import java.util.Locale


@Preview
@Composable
fun HydroDashboardScreen(
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(HydroOperatingMode.NORMAL_FLOW) }

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
            HydroDashboardHeader()

            Spacer(modifier = Modifier.height(20.dp))

            HydroStatusBanner(selectedMode = selectedMode)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Оберіть режим затворів та генерації ГЕС:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            HydroModeSelector(
                selectedMode = selectedMode,
                onModeSelected = { selectedMode = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            HydroMetricsGrid(selectedMode = selectedMode)
        }
    }
}

@Composable
private fun HydroDashboardHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Завдання 3",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Дашборд Оператора ГЕС",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = "Адаптивний дашборд контролю рівня води та затворів плотини",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun HydroStatusBanner(selectedMode: HydroOperatingMode) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.TopStart)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Water,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    Text(
                        text = "Дніпровський Гідровузол «ГЕС-1»",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Активний режим: ${selectedMode.title}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = selectedMode.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF00897B))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HydroModeSelector(
    selectedMode: HydroOperatingMode,
    onModeSelected: (HydroOperatingMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HydroOperatingMode.entries.forEach { mode ->
            val isSelected = mode == selectedMode
            val containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            val icon: ImageVector = when (mode) {
                HydroOperatingMode.NORMAL_FLOW -> Icons.Default.Waves
                HydroOperatingMode.PEAK_LOAD -> Icons.Default.Speed
                HydroOperatingMode.FLOOD_CONTROL -> Icons.Default.Thunderstorm
                HydroOperatingMode.ECO_MINIMUM -> Icons.Default.Eco
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onModeSelected(mode) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = mode.title,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.padding(start = 12.dp))
                    Column {
                        Text(
                            text = mode.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                        Text(
                            text = mode.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HydroMetricsGrid(selectedMode: HydroOperatingMode) {
    val multiplier = when (selectedMode) {
        HydroOperatingMode.NORMAL_FLOW -> 1.0f
        HydroOperatingMode.PEAK_LOAD -> 1.4f
        HydroOperatingMode.FLOOD_CONTROL -> 1.8f
        HydroOperatingMode.ECO_MINIMUM -> 0.3f
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = "Витрата води Q",
                value = String.format(Locale.getDefault(), "%.0f", 350.0f * multiplier),
                unit = "м³/с",
                icon = Icons.Default.Waves,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Генерація ГЕС",
                value = String.format(Locale.getDefault(), "%.1f", 45.0f * multiplier),
                unit = "МВт",
                icon = Icons.Default.ElectricBolt,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
