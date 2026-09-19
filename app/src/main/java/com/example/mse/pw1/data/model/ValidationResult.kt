package com.example.mse.pw1.data.model

/**
 * Результат валідації полів.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)
