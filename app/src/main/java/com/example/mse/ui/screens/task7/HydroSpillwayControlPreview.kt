package com.example.mse.ui.screens.task7

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.mse.ui.theme.MSETheme

@Preview(showBackground = true)
@Composable
fun HydroSpillwayPreviewStandalone() {
    MSETheme {
        HydroSpillwayControlContent(
            gate1Opening = 25f,
            gate2Opening = 10f,
            gate3Opening = 50f,
            gate4Opening = 0f,
            isEmergencyActive = false,
            totalSpillwayFlowM3 = 212.5,
            downstreamSafetyStatus = "Увага: помірне підвищення рівня в нижньому б'єфі",
            onGate1Change = {},
            onGate2Change = {},
            onGate3Change = {},
            onGate4Change = {},
            onEmergencyToggle = {}
        )
    }
}
