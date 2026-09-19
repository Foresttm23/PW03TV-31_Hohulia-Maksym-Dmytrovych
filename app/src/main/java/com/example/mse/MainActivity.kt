package com.example.mse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.mse.pw1.ui.screens.task1.HydroForecastScreen
import com.example.mse.pw1.ui.screens.task2.HydroConfigScreen
import com.example.mse.pw1.ui.screens.task3.HydroDashboardScreen
import com.example.mse.pw1.ui.screens.task4.AuthRegistrationScreen
import com.example.mse.pw1.ui.theme.MSETheme

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
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
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
