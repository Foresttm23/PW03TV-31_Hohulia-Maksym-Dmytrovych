package com.example.mse.ui.screens.task8

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mse.data.db.HydroDatabase
import com.example.mse.data.repository.HydroRecordRepository
import com.example.mse.data.repository.HydroRecordRepositoryImpl
import com.example.mse.model.HydroOperatingMode
import com.example.mse.model.HydroRecordEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel для управління збереженням та читанням показників через HydroRecordRepository (Clean Architecture).
 */
class HydroOperationsViewModel(
    application: Application,
    private val repository: HydroRecordRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        HydroRecordRepositoryImpl(HydroDatabase.getInstance(application).hydroRecordDao())
    )

    val savedRecords: StateFlow<List<HydroRecordEntity>> = repository.getAllRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveCurrentMetrics(
        waterLevel: Float,
        powerMw: Float,
        flowRate: Float,
        mode: HydroOperatingMode,
        notes: String
    ) {
        viewModelScope.launch {
            val record = HydroRecordEntity(
                waterLevelMeters = waterLevel,
                generatedPowerMw = powerMw,
                waterFlowRateM3PerSec = flowRate,
                operatingMode = mode.title,
                operatorNotes = notes.ifBlank { "Штатний моніторинг оператора" }
            )
            repository.insertRecord(record)
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteRecordById(id)
        }
    }

    fun clearAllRecords() {
        viewModelScope.launch {
            repository.clearAllRecords()
        }
    }
}
