package com.example.mse.model

/**
 * UI модель для відображення запису у списках без анотацій Room Database.
 */
data class HydroRecordDisplayItem(
    val id: Long,
    val timeFormatted: String,
    val levelText: String,
    val notesText: String
)
