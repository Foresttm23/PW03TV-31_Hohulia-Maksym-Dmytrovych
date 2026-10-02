package com.example.mse.ui.screens.task5

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DoorSliding
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mse.logic.GateInfo
import com.example.mse.logic.HydroOperationsLogic
import com.example.mse.logic.RiskLevel
import com.example.mse.ui.theme.MSETheme
import java.util.Locale

/** Головний модуль навігації екрана управління шлюзами ГЕС (Task 5) */
@Composable
fun HydroNavMenuScreen(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    var alertThreshold by remember { mutableFloatStateOf(122.0f) }

    val gates = remember {
        listOf(
            GateInfo(1, "Шлюз №1 (Основний скид)", 121.2f, true, 75),
            GateInfo(2, "Шлюз №2 (Резервний)", 123.8f, false, 0),
            GateInfo(3, "Шлюз №3 (Аварійний)", 122.4f, true, 40)
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = "main_menu"
        ) {
            composable("main_menu") {
                HydroMainMenuScreen(
                    navController = navController,
                    gates = gates,
                    alertThreshold = alertThreshold
                )
            }

            composable(
                route = "gate_detail/{gateId}/{waterLevel}/{isOpen}/{openingPct}",
                arguments = listOf(
                    navArgument("gateId") { type = NavType.IntType },
                    navArgument("waterLevel") { type = NavType.FloatType },
                    navArgument("isOpen") { type = NavType.BoolType },
                    navArgument("openingPct") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val gateId = backStackEntry.arguments?.getInt("gateId") ?: 1
                val waterLevel = backStackEntry.arguments?.getFloat("waterLevel") ?: 120.0f
                val isOpen = backStackEntry.arguments?.getBoolean("isOpen") ?: false
                val openingPct = backStackEntry.arguments?.getInt("openingPct") ?: 0

                GateDetailScreen(
                    navController = navController,
                    gateId = gateId,
                    waterLevel = waterLevel,
                    isOpen = isOpen,
                    openingPercentage = openingPct
                )
            }

            composable(
                route = "reservoir_config/{currentThreshold}",
                arguments = listOf(
                    navArgument("currentThreshold") { type = NavType.FloatType }
                )
            ) { backStackEntry ->
                val current = backStackEntry.arguments?.getFloat("currentThreshold") ?: 122.0f
                ReservoirConfigScreen(
                    navController = navController,
                    initialThreshold = current,
                    onSaveThreshold = { alertThreshold = it }
                )
            }
        }
    }
}

/** Composable головного меню з адаптивним розміщенням елементів */
@Composable
private fun HydroMainMenuScreen(
    navController: NavController,
    gates: List<GateInfo>,
    alertThreshold: Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderTitleSection()

        Spacer(modifier = Modifier.height(16.dp))

        ThresholdStatusCard(
            alertThreshold = alertThreshold,
            onConfigureClick = {
                navController.navigate("reservoir_config/$alertThreshold")
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Оберіть затвор ГЕС для детального моніторингу:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (this.maxWidth > 600.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    gates.forEach { gate ->
                        GateStatusCard(
                            gate = gate,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                navController.navigate(
                                    "gate_detail/${gate.id}/${gate.waterLevel}/${gate.isOpen}/${gate.openingPercentage}"
                                )
                            }
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    gates.forEach { gate ->
                        GateStatusCard(
                            gate = gate,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                navController.navigate(
                                    "gate_detail/${gate.id}/${gate.waterLevel}/${gate.isOpen}/${gate.openingPercentage}"
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Заголовок екрана завдання 5 */
@Composable
private fun HeaderTitleSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Завдання 5",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Навігаційний Диспетчер Шлюзів ГЕС",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Інтерактивна навігація NavController/NavHost з аргументами",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Картка стану поточного порогу безпеки */
@Composable
private fun ThresholdStatusCard(
    alertThreshold: Float,
    onConfigureClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Поріг тривоги водосховища",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = String.format(Locale.getDefault(), "%.1f м", alertThreshold),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Button(
                onClick = onConfigureClick,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Налаштувати")
            }
        }
    }
}

/** Перевикористовувана картка затвора шлюзу */
@Composable
private fun GateStatusCard(
    gate: GateInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val risk = HydroOperationsLogic.assessGateRisk(gate.waterLevel, gate.isOpen)

    val badgeColor = when (risk) {
        RiskLevel.SAFE -> Color(0xFF2E7D32)
        RiskLevel.WARNING -> Color(0xFFEF6C00)
        RiskLevel.DANGER -> Color(0xFFC62828)
    }

    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DoorSliding,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gate.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Рівень: ${gate.waterLevel} м | Стан: ${if (gate.isOpen) "ВІДКРИТО (${gate.openingPercentage}%)" else "ЗАКРИТО"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = risk.title,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Детальніше",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Екран детальної інформації про затвор з переданими аргументами */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GateDetailScreen(
    navController: NavController,
    gateId: Int,
    waterLevel: Float,
    isOpen: Boolean,
    openingPercentage: Int
) {
    val risk = HydroOperationsLogic.assessGateRisk(waterLevel, isOpen)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Деталі Шлюзу №$gateId") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Параметри Шлюзу №$gateId",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    DetailRow(label = "Висота води верхнього б'єфу:", value = "$waterLevel м")
                    DetailRow(label = "Стан затвора:", value = if (isOpen) "Відкрито" else "Закрито")
                    DetailRow(label = "Ступінь відкриття:", value = "$openingPercentage%")
                    DetailRow(label = "Оцінка ризику:", value = risk.title)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = risk.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Повернутися до Головного Меню")
            }
        }
    }
}

/** Екран налаштування порогу тривоги водосховища */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReservoirConfigScreen(
    navController: NavController,
    initialThreshold: Float,
    onSaveThreshold: (Float) -> Unit
) {
    var thresholdValue by remember { mutableFloatStateOf(initialThreshold) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Налаштування Порогу Тривоги") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Встановлення Порогу Попередження НПУ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = String.format(Locale.getDefault(), "Обраний поріг: %.1f м", thresholdValue),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = thresholdValue,
                onValueChange = { thresholdValue = it },
                valueRange = 118.0f..126.0f,
                steps = 15,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSaveThreshold(thresholdValue)
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Зберегти та оновити UI")
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true)
@Composable
fun HydroNavMenuScreenPreview() {
    MSETheme {
        HydroNavMenuScreen()
    }
}
