package com.example.mse.logic

object LoginFormValidator {
    private val USERNAME_REGEX = Regex("^[A-Za-z0-9._-]+$")

    fun validateUsername(value: String): String? = when {
        value.isBlank() -> "Логін не може бути порожнім"
        value.trim().length < 3 -> "Логін — щонайменше 3 символи"
        !USERNAME_REGEX.matches(value.trim()) -> "Дозволені латинські літери, цифри, . _ -"
        else -> null
    }

    fun validatePassword(value: String): String? = when {
        value.isEmpty() -> "Пароль не може бути порожнім"
        value.length < 6 -> "Пароль — щонайменше 6 символів"
        else -> null
    }
}
