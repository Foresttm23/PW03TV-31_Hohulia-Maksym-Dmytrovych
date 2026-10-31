package com.example.mse.model

enum class Permission {
    VIEW_TELEMETRY,
    CONTROL_GATES,
    MANAGE_USERS,
    VIEW_AUDIT
}

enum class UserRole(val title: String, val permissions: Set<Permission>) {
    ADMIN(
        "Адміністратор ГЕС",
        setOf(Permission.VIEW_TELEMETRY, Permission.CONTROL_GATES, Permission.MANAGE_USERS, Permission.VIEW_AUDIT)
    ),
    ENGINEER("Черговий інженер", setOf(Permission.VIEW_TELEMETRY, Permission.CONTROL_GATES)),
    OBSERVER("Спостерігач", setOf(Permission.VIEW_TELEMETRY));

    fun has(permission: Permission): Boolean = permission in permissions

    companion object {
        fun fromClaim(value: String?): UserRole? = entries.firstOrNull { it.name == value }
    }
}

data class TokenClaims(
    val sub: String,
    val name: String,
    val role: String,
    val iat: Long,
    val exp: Long,
    val jti: String
)

data class AuthSession(
    val token: String,
    val claims: TokenClaims,
    val role: UserRole
) {
    fun secondsLeft(nowMs: Long = System.currentTimeMillis()): Long = claims.exp - nowMs / 1000
    fun isExpired(nowMs: Long = System.currentTimeMillis()): Boolean = secondsLeft(nowMs) <= 0
}

sealed class AuthError(val userMessage: String, val requiresRelogin: Boolean) {
    data object NoToken : AuthError("Токен відсутній: запит до захищеного ресурсу відхилено (HTTP 401)", true)
    data object Expired : AuthError("Термін дії токена минув. Увійдіть повторно.", true)
    data class InvalidToken(val reason: String) : AuthError("Недійсний токен: $reason. Сесію завершено.", true)
    data class Forbidden(val detail: String) : AuthError("Доступ заборонено (HTTP 403): $detail", false)
    data object InvalidCredentials : AuthError("Невірний логін або пароль", false)
    data class Validation(val detail: String) : AuthError(detail, false)
    data class Network(val detail: String) : AuthError("Помилка з'єднання: $detail", false)
}

sealed interface AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>
    data class Failure(val error: AuthError) : AuthResult<Nothing>
}

data class LoginRequest(val username: String, val password: String, val ttlSeconds: Long)

data class UserDto(val username: String, val fullName: String, val role: String, val shift: String)

data class LoginResponse(val token: String, val tokenType: String, val expiresIn: Long, val user: UserDto)

data class ApiErrorDto(val error: String, val message: String)

data class UnitStatusDto(val unit: String, val powerMw: Double, val state: String)

data class SecureTelemetryDto(
    val station: String,
    val upstreamLevelM: Double,
    val downstreamLevelM: Double,
    val inflowM3s: Double,
    val outflowM3s: Double,
    val totalPowerMw: Double,
    val units: List<UnitStatusDto>,
    val servedTo: String
)

data class GateCommandRequest(val gateId: Int, val openingPercent: Int)

data class GateCommandResponse(val gateId: Int, val openingPercent: Int, val acceptedBy: String, val message: String)

data class AuditEntryDto(val time: Long, val user: String, val endpoint: String, val status: Int)
