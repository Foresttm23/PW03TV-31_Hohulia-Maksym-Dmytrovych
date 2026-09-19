package com.example.mse.ui.screens.task1

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mse.logic.HydroCalculator
import com.example.mse.ui.components.MetricCard
import java.util.Locale

@Composable
fun HydroForecastScreen(
    modifier: Modifier = Modifier
) {
    var waterLevelMeters by remember { mutableFloatStateOf(120.8f) }
    var generatedPowerMw by remember { mutableFloatStateOf(48.5f) }
    var waterFlowRate by remember { mutableFloatStateOf(380.0f) }

    val animatedWaterLevel by animateFloatAsState(
        targetValue = waterLevelMeters,
        animationSpec = tween(durationMillis = 800),
        label = "WaterLevelAnimation"
    )

    val animatedPowerMw by animateFloatAsState(
        targetValue = generatedPowerMw,
        animationSpec = tween(durationMillis = 800),
        label = "PowerAnimation"
    )

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
            HydroTitle()

            Spacer(modifier = Modifier.height(20.dp))

            WaterLevelGaugeCard(
                waterLevel = animatedWaterLevel,
                targetLevel = 122.0f
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Потужність ГЕС",
                    value = String.format(Locale.getDefault(), "%.1f", animatedPowerMw),
                    unit = "МВт",
                    icon = Icons.Default.ElectricBolt,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Витрата води",
                    value = String.format(Locale.getDefault(), "%.0f", waterFlowRate),
                    unit = "м³/с",
                    icon = Icons.Default.Waves,
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    val newData = HydroCalculator.generateRandomMetrics()
                    waterLevelMeters = newData.waterLevelMeters
                    generatedPowerMw = newData.generatedPowerMw
                    waterFlowRate = newData.waterFlowRateM3PerSec
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Оновити показники"
                )
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text(
                    text = "Оновити показники датчиків ГЕС",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun HydroTitle() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Завдання 1",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "ГЕС з контролем рівня води",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = "Моніторинг верхнього б'єфу та генерації гідроелектростанції",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun WaterLevelGaugeCard(
    waterLevel: Float,
    targetLevel: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Water,
                    contentDescription = "Рівень води",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Рівень води (Верхній Б'єф)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = String.format(Locale.getDefault(), "%.1f", waterLevel),
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "метрів (Норма НПУ: $targetLevel м)",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            val fillProgress = ((waterLevel - 100f) / 25f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillProgress)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
