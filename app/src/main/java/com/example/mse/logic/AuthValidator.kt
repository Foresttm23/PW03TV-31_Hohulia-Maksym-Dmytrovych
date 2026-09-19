package com.example.mse.logic

import com.example.mse.model.ValidationResult

object AuthValidator {

    private val EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()

    fun validateEmail(email: String): ValidationResult {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> ValidationResult(
                false,
                "Електронна пошта не може бути порожньою"
            )
            !trimmed.matches(EMAIL_REGEX) -> ValidationResult(
                false,
                "Некоректний формат email (наприклад: operator@hydro.com)"
            )
            else -> ValidationResult(true)
        }
    }

    fun validateFullName(name: String): ValidationResult {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> ValidationResult(
                false,
                "Будь ласка, вкажіть ваше ім'я та прізвище"
            )
            trimmed.length < 3 -> ValidationResult(
                false,
                "Ім'я повинно містити щонайменше 3 символи"
            )
            else -> ValidationResult(true)
        }
    }

    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isEmpty() -> ValidationResult(
                false,
                "Пароль не може бути порожнім"
            )
            password.length < 6 -> ValidationResult(
                false,
                "Пароль повинен містити не менше 6 символів"
            )
            !password.any { it.isDigit() } -> ValidationResult(
                false,
                "Пароль повинен містити хоча б одну цифру"
            )
            else -> ValidationResult(true)
        }
    }

    fun validateConfirmPassword(password: String, confirm: String): ValidationResult {
        return when {
            confirm.isEmpty() -> ValidationResult(
                false,
                "Підтвердіть введення пароля"
            )
            password != confirm -> ValidationResult(
                false,
                "Введені паролі не збігаються"
            )
            else -> ValidationResult(true)
        }
    }
}
