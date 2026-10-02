package com.example.mse.ui.screens.task7

import androidx.lifecycle.ViewModel
import com.example.mse.logic.HydroOperationsLogic
import com.example.mse.model.HydroSpillwayUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel архітектури MVVM для регулювання холостих затворів на базі StateFlow.
 */
class HydroSpillwayViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HydroSpillwayUiState())
    val uiState: StateFlow<HydroSpillwayUiState> = _uiState.asStateFlow()

    fun updateGate1(opening: Float) {
        _uiState.update { it.copy(gate1Opening = opening) }
        recalculateFlow()
    }

    fun updateGate2(opening: Float) {
        _uiState.update { it.copy(gate2Opening = opening) }
        recalculateFlow()
    }

    fun updateGate3(opening: Float) {
        _uiState.update { it.copy(gate3Opening = opening) }
        recalculateFlow()
    }

    fun updateGate4(opening: Float) {
        _uiState.update { it.copy(gate4Opening = opening) }
        recalculateFlow()
    }

    fun toggleEmergencyRelease() {
        _uiState.update { current ->
            val newEmergencyState = !current.isEmergencyActive
            if (newEmergencyState) {
                current.copy(
                    isEmergencyActive = true,
                    gate1Opening = 100f,
                    gate2Opening = 100f,
                    gate3Opening = 100f,
                    gate4Opening = 100f
                )
            } else {
                current.copy(
                    isEmergencyActive = false,
                    gate1Opening = 0f,
                    gate2Opening = 0f,
                    gate3Opening = 0f,
                    gate4Opening = 0f
                )
            }
        }
        recalculateFlow()
    }

    private fun recalculateFlow() {
        _uiState.update { current ->
            val totalFlow = (
                    (current.gate1Opening + current.gate2Opening + current.gate3Opening + current.gate4Opening) / 100.0 * 250.0
                    )
            val safety = HydroOperationsLogic.calculateDownstreamRiverLevel(totalFlow)
            current.copy(
                totalSpillwayFlowM3 = totalFlow,
                downstreamSafetyStatus = safety
            )
        }
    }
}
