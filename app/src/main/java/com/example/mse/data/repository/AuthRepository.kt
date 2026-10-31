package com.example.mse.data.repository

import com.example.mse.data.auth.JwtTokenCodec
import com.example.mse.data.auth.TokenStorage
import com.example.mse.data.remote.AuthApi
import com.example.mse.model.ApiErrorDto
import com.example.mse.model.AuditEntryDto
import com.example.mse.model.AuthError
import com.example.mse.model.AuthResult
import com.example.mse.model.AuthSession
import com.example.mse.model.GateCommandRequest
import com.example.mse.model.GateCommandResponse
import com.example.mse.model.LoginRequest
import com.example.mse.model.SecureTelemetryDto
import com.example.mse.model.UserDto
import com.example.mse.model.UserRole
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class AuthRepository(
    private val api: AuthApi,
    private val tokenStorage: TokenStorage,
    private val gson: Gson = Gson(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val _session = MutableStateFlow<AuthSession?>(null)
    val session: StateFlow<AuthSession?> = _session.asStateFlow()

    suspend fun restoreSession(): AuthResult<AuthSession?> = withContext(ioDispatcher) {
        val token = tokenStorage.restore() ?: return@withContext AuthResult.Success(null)
        when (val session = parse(token)) {
            null -> { clearSession(); AuthResult.Failure(AuthError.InvalidToken("збережений токен пошкоджено")) }
            else -> if (session.isExpired(clock())) {
                clearSession(); AuthResult.Failure(AuthError.Expired)
            } else {
                _session.value = session; AuthResult.Success(session)
            }
        }
    }

    suspend fun login(username: String, password: String, ttlSeconds: Long): AuthResult<AuthSession> =
        withContext(ioDispatcher) {
            try {
                val response = api.login(LoginRequest(username.trim(), password, ttlSeconds))
                val session = parse(response.token)
                    ?: return@withContext AuthResult.Failure(AuthError.InvalidToken("сервер повернув некоректний токен"))
                tokenStorage.save(response.token)
                _session.value = session
                AuthResult.Success(session)
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                AuthResult.Failure(
                    when (e.code()) {
                        401 -> AuthError.InvalidCredentials
                        400 -> AuthError.Validation(errorBody(e)?.message ?: "Некоректні дані")
                        else -> AuthError.Network("HTTP ${e.code()}")
                    }
                )
            } catch (e: IOException) {
                AuthResult.Failure(AuthError.Network(e.message ?: "сервер недоступний"))
            }
        }

    suspend fun logout() = withContext(ioDispatcher) { clearSession() }

    suspend fun telemetry(): AuthResult<SecureTelemetryDto> = secureCall { api.telemetry() }
    suspend fun gateCommand(gateId: Int, opening: Int): AuthResult<GateCommandResponse> =
        secureCall { api.gateCommand(GateCommandRequest(gateId, opening)) }
    suspend fun users(): AuthResult<List<UserDto>> = secureCall { api.users() }
    suspend fun audit(): AuthResult<List<AuditEntryDto>> = secureCall { api.audit() }

    suspend fun telemetryWithoutToken(): AuthResult<SecureTelemetryDto> =
        secureCall(skipLocalCheck = true) { api.telemetryWithoutToken() }

    suspend fun forgeAdminToken(): AuthSession? = withContext(ioDispatcher) {
        val token = tokenStorage.currentToken() ?: return@withContext null
        val forged = JwtTokenCodec.forgeRole(token, UserRole.ADMIN.name)
        tokenStorage.save(forged)
        parse(forged)?.also { _session.value = it }
    }

    private suspend fun <T> secureCall(skipLocalCheck: Boolean = false, block: suspend () -> T): AuthResult<T> =
        withContext(ioDispatcher) {
            if (!skipLocalCheck) {
                val current = _session.value ?: return@withContext AuthResult.Failure(AuthError.NoToken)
                if (current.isExpired(clock())) {
                    clearSession()
                    return@withContext AuthResult.Failure(AuthError.Expired)
                }
            }
            try {
                AuthResult.Success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                val body = errorBody(e)
                when (e.code()) {
                    401 -> {
                        val error = when (body?.error) {
                            "token_expired" -> AuthError.Expired
                            "missing_token" -> AuthError.NoToken
                            else -> AuthError.InvalidToken(body?.message ?: "відхилено сервером")
                        }
                        // Запит без токена — демонстраційний, справжню сесію він не скидає.
                        if (error != AuthError.NoToken) clearSession()
                        AuthResult.Failure(error)
                    }
                    // 403: токен дійсний, бракує прав ролі — сесія зберігається.
                    403 -> AuthResult.Failure(AuthError.Forbidden(body?.message ?: "недостатньо прав"))
                    else -> AuthResult.Failure(AuthError.Network("HTTP ${e.code()}: ${body?.message.orEmpty()}"))
                }
            } catch (e: IOException) {
                AuthResult.Failure(AuthError.Network(e.message ?: "сервер недоступний"))
            }
        }

    private fun parse(token: String): AuthSession? {
        val claims = JwtTokenCodec.decodeUnverified(token) ?: return null
        val role = UserRole.fromClaim(claims.role) ?: return null
        return AuthSession(token, claims, role)
    }

    private suspend fun clearSession() {
        tokenStorage.clear()
        _session.value = null
    }

    private fun errorBody(e: HttpException): ApiErrorDto? = try {
        e.response()?.errorBody()?.string()?.let { gson.fromJson(it, ApiErrorDto::class.java) }
    } catch (ex: Exception) {
        null
    }
}
