package com.example.mse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DoorSliding
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.mse.ui.screens.task1.HydroForecastScreen
import com.example.mse.ui.screens.task2.HydroConfigScreen
import com.example.mse.ui.screens.task3.HydroDashboardScreen
import com.example.mse.ui.screens.task4.AuthRegistrationScreen
import com.example.mse.ui.screens.task5.HydroNavMenuScreen
import com.example.mse.ui.screens.task6.HydroWaterControlScreen
import com.example.mse.ui.screens.task7.HydroSpillwayControlScreen
import com.example.mse.ui.screens.task8.HydroOperationsCenterScreen
import com.example.mse.ui.theme.MSETheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MSETheme {
                MainAppScreen()
            }
        }
    }
}

data class NavigationTab(
    val title: String,
    val icon: ImageVector,
    val screen: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        NavigationTab(
            title = "Task 1: Рівень",
            icon = Icons.Default.Water,
            screen = { HydroForecastScreen() }
        ),
        NavigationTab(
            title = "Task 2: Калькулятор",
            icon = Icons.Default.Calculate,
            screen = { HydroConfigScreen() }
        ),
        NavigationTab(
            title = "Task 3: Дашборд",
            icon = Icons.Default.Dashboard,
            screen = { HydroDashboardScreen() }
        ),
        NavigationTab(
            title = "Task 4: Авторизація",
            icon = Icons.Default.Lock,
            screen = { AuthRegistrationScreen() }
        ),
        NavigationTab(
            title = "Task 5: Навігація",
            icon = Icons.Default.Explore,
            screen = { HydroNavMenuScreen() }
        ),
        NavigationTab(
            title = "Task 6: Баланс води",
            icon = Icons.Default.Analytics,
            screen = { HydroWaterControlScreen() }
        ),
        NavigationTab(
            title = "Task 7: Затвори",
            icon = Icons.Default.DoorSliding,
            screen = { HydroSpillwayControlScreen() }
        ),
        NavigationTab(
            title = "Task 8: Диспетчер",
            icon = Icons.Default.Computer,
            screen = { HydroOperationsCenterScreen() }
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(tab.title) },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            tabs[selectedTabIndex].screen()
        }
    }
}
