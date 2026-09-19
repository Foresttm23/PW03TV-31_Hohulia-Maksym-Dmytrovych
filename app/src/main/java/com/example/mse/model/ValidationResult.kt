package com.example.mse.model

/**
 * Результат валідації полів.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)
