package com.example.mse.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сутність Room Database для збереження енергетичних показників ГЕС (Практична 8).
 */
@Entity(tableName = "hydro_records")
data class HydroRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val waterLevelMeters: Float,
    val generatedPowerMw: Float,
    val waterFlowRateM3PerSec: Float,
    val operatingMode: String,
    val operatorNotes: String
)
