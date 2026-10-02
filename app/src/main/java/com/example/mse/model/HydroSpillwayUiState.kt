package com.example.mse.model

/**
 * Стан UI екрана регулювання затворів ГЕС (Практична 7 - MVVM).
 */
data class HydroSpillwayUiState(
    val gate1Opening: Float = 20f,
    val gate2Opening: Float = 0f,
    val gate3Opening: Float = 40f,
    val gate4Opening: Float = 0f,
    val isEmergencyActive: Boolean = false,
    val totalSpillwayFlowM3: Double = 150.0,
    val downstreamSafetyStatus: String = "Нормальний рівень річки в нижньому б'єфі"
)
