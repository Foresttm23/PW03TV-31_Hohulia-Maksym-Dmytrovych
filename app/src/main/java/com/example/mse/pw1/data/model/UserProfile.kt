package com.example.mse.pw1.data.model

/**
 * Модель профілю оператора ГЕС.
 */
data class UserProfile(
    val fullName: String,
    val email: String,
    val role: String = "Оператор ГЕС",
    val stationName: String = "Гідроелектростанція №1"
)
