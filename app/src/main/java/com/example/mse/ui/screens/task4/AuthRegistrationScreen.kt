package com.example.mse.ui.screens.task4

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mse.logic.AuthValidator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN, REGISTER
}

/**
 * Головний контейнерний екран авторизації та реєстрації оператора ГЕС (Task 4).
 */
@Composable
fun AuthRegistrationScreen(
    modifier: Modifier = Modifier
) {
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }

    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    fun resetErrors() {
        nameError = null
        emailError = null
        passwordError = null
        confirmPasswordError = null
        successMessage = null
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthHeader(authMode = authMode)

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AuthFormFields(
                        authMode = authMode,
                        nameInput = nameInput,
                        onNameChange = { nameInput = it; nameError = null },
                        nameError = nameError,
                        emailInput = emailInput,
                        onEmailChange = { emailInput = it; emailError = null },
                        emailError = emailError,
                        passwordInput = passwordInput,
                        onPasswordChange = { passwordInput = it; passwordError = null },
                        passwordError = passwordError,
                        confirmPasswordInput = confirmPasswordInput,
                        onConfirmPasswordChange = { confirmPasswordInput = it; confirmPasswordError = null },
                        confirmPasswordError = confirmPasswordError,
                        isPasswordVisible = isPasswordVisible,
                        onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AuthSubmitButton(
                        authMode = authMode,
                        isLoading = isLoading,
                        onSubmit = {
                            val emailVal = AuthValidator.validateEmail(emailInput)
                            val passVal = AuthValidator.validatePassword(passwordInput)

                            emailError = emailVal.errorMessage
                            passwordError = passVal.errorMessage

                            var isValid = emailVal.isValid && passVal.isValid

                            if (authMode == AuthMode.REGISTER) {
                                val nameVal = AuthValidator.validateFullName(nameInput)
                                val confirmVal = AuthValidator.validateConfirmPassword(passwordInput, confirmPasswordInput)

                                nameError = nameVal.errorMessage
                                confirmPasswordError = confirmVal.errorMessage

                                isValid = isValid && nameVal.isValid && confirmVal.isValid
                            }

                            if (isValid) {
                                isLoading = true
                                successMessage = null
                                coroutineScope.launch {
                                    delay(1200)
                                    isLoading = false
                                    successMessage = if (authMode == AuthMode.LOGIN) {
                                        "Успішна авторизація оператора ГЕС! $emailInput."
                                    } else {
                                        "Успішна реєстрація оператора $nameInput у системі ГЕС!"
                                    }
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = {
                            authMode = if (authMode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN
                            resetErrors()
                        }
                    ) {
                        Text(
                            text = if (authMode == AuthMode.LOGIN) "Створити профіль оператора" else "Вже є профіль? Увійти",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AuthSuccessCard(message = successMessage)
        }
    }
}

@Composable
private fun AuthHeader(authMode: AuthMode) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.Water,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = "Завдання 4",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = if (authMode == AuthMode.LOGIN) "Авторизація Оператора ГЕС" else "Реєстрація у Системі ГЕС",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun AuthFormFields(
    authMode: AuthMode,
    nameInput: String,
    onNameChange: (String) -> Unit,
    nameError: String?,
    emailInput: String,
    onEmailChange: (String) -> Unit,
    emailError: String?,
    passwordInput: String,
    onPasswordChange: (String) -> Unit,
    passwordError: String?,
    confirmPasswordInput: String,
    onConfirmPasswordChange: (String) -> Unit,
    confirmPasswordError: String?,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit
) {
    Column {
        if (authMode == AuthMode.REGISTER) {
            OutlinedTextField(
                value = nameInput,
                onValueChange = onNameChange,
                label = { Text("Ім'я оператора ГЕС") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                isError = nameError != null,
                supportingText = { nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = emailInput,
            onValueChange = onEmailChange,
            label = { Text("Електронна пошта (Email)") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = emailError != null,
            supportingText = { emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = passwordInput,
            onValueChange = onPasswordChange,
            label = { Text("Пароль") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Переключити видимість"
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = passwordError != null,
            supportingText = { passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (authMode == AuthMode.REGISTER) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmPasswordInput,
                onValueChange = onConfirmPasswordChange,
                label = { Text("Підтвердження пароля") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = confirmPasswordError != null,
                supportingText = { confirmPasswordError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun AuthSubmitButton(
    authMode: AuthMode,
    isLoading: Boolean,
    onSubmit: () -> Unit
) {
    Button(
        onClick = onSubmit,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                text = if (authMode == AuthMode.LOGIN) "Увійти у пульт ГЕС" else "Зареєструвати оператора ГЕС",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AuthSuccessCard(message: String?) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = message ?: "",
                color = Color(0xFF00695C),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}
