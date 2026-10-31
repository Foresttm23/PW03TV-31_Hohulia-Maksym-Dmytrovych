package com.example.mse.data.remote.mock

import com.example.mse.data.auth.JwtTokenCodec
import com.example.mse.model.ApiErrorDto
import com.example.mse.model.AuditEntryDto
import com.example.mse.model.GateCommandRequest
import com.example.mse.model.GateCommandResponse
import com.example.mse.model.LoginRequest
import com.example.mse.model.LoginResponse
import com.example.mse.model.Permission
import com.example.mse.model.SecureTelemetryDto
import com.example.mse.model.TokenClaims
import com.example.mse.model.UnitStatusDto
import com.example.mse.model.UserDto
import com.example.mse.model.UserRole
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.util.Collections
import java.util.UUID
import kotlin.random.Random

data class MockUser(val username: String, val password: String, val fullName: String, val role: UserRole, val shift: String)

// Останній Interceptor у ланцюжку відповідає замість сервера: Retrofit отримує справжні HTTP-коди та JSON.
class MockAuthServer(
    private val gson: Gson = Gson(),
    private val latencyMs: Long = 350,
    private val clock: () -> Long = System::currentTimeMillis
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (latencyMs > 0) Thread.sleep(latencyMs)
        val path = request.url.encodedPath
        val response = when {
            path == "/api/auth/login" && request.method == "POST" -> login(request)
            path == "/api/secure/telemetry" -> secured(request, Permission.VIEW_TELEMETRY) { telemetry(request, it) }
            path == "/api/secure/gates/command" -> secured(request, Permission.CONTROL_GATES) { gateCommand(request, it) }
            path == "/api/secure/admin/users" -> secured(request, Permission.MANAGE_USERS) { json(request, 200, USERS.map { u -> u.toDto() }) }
            path == "/api/secure/admin/audit" -> secured(request, Permission.VIEW_AUDIT) { json(request, 200, synchronized(audit) { audit.toList() }.takeLast(30).reversed()) }
            else -> error(request, 404, "not_found", "Ресурс $path не існує")
        }
        val user = request.header("Authorization")?.let { JwtTokenCodec.decodeUnverified(it.removePrefix("Bearer "))?.sub } ?: "anonymous"
        audit += AuditEntryDto(clock(), user, "${request.method} $path", response.code)
        return response
    }

    private fun login(request: Request): Response {
        val body = request.bodyAsString()
        val login = runCatching { gson.fromJson(body, LoginRequest::class.java) }.getOrNull()
            ?: return error(request, 400, "bad_request", "Некоректне тіло запиту")
        if (login.username.isNullOrBlank() || login.password.isNullOrBlank()) {
            return error(request, 400, "validation_error", "Логін і пароль обов'язкові")
        }
        val user = USERS.firstOrNull { it.username.equals(login.username.trim(), ignoreCase = true) && it.password == login.password }
            ?: return error(request, 401, "invalid_credentials", "Невірний логін або пароль")

        val now = clock() / 1000
        val ttl = login.ttlSeconds.coerceIn(10, 3600)
        val claims = TokenClaims(
            sub = user.username,
            name = user.fullName,
            role = user.role.name,
            iat = now,
            exp = now + ttl,
            jti = UUID.randomUUID().toString().take(8)
        )
        return json(request, 200, LoginResponse(JwtTokenCodec.sign(claims, SERVER_SECRET), "Bearer", ttl, user.toDto()))
    }

    private fun secured(request: Request, permission: Permission, handler: (TokenClaims) -> Response): Response {
        val header = request.header("Authorization")
            ?: return error(request, 401, "missing_token", "Заголовок Authorization відсутній")
        if (!header.startsWith("Bearer ")) return error(request, 401, "invalid_token", "Очікується схема Bearer")
        val claims = when (val v = JwtTokenCodec.verify(header.removePrefix("Bearer "), SERVER_SECRET, clock() / 1000)) {
            is JwtTokenCodec.Verification.Valid -> v.claims
            is JwtTokenCodec.Verification.Expired -> return error(request, 401, "token_expired", "Термін дії токена минув")
            JwtTokenCodec.Verification.BadSignature -> return error(request, 401, "invalid_signature", "Підпис токена не збігається — токен підроблено")
            JwtTokenCodec.Verification.Malformed -> return error(request, 401, "invalid_token", "Токен має некоректний формат")
        }
        val role = UserRole.fromClaim(claims.role) ?: return error(request, 401, "invalid_token", "Невідома роль у токені")
        if (!role.has(permission)) {
            return error(request, 403, "forbidden", "Роль «${role.title}» не має права ${permission.name}")
        }
        return handler(claims)
    }

    private fun telemetry(request: Request, claims: TokenClaims): Response {
        val units = (1..4).map { i ->
            val running = i != 4
            UnitStatusDto("ГА-$i", if (running) 18.0 + Random.nextDouble(0.0, 2.5) else 0.0, if (running) "В роботі" else "Резерв")
        }
        val dto = SecureTelemetryDto(
            station = "Київська ГЕС",
            upstreamLevelM = 102.8 + Random.nextDouble(-0.05, 0.05),
            downstreamLevelM = 91.4 + Random.nextDouble(-0.05, 0.05),
            inflowM3s = 1180.0 + Random.nextDouble(-40.0, 40.0),
            outflowM3s = 1120.0 + Random.nextDouble(-30.0, 30.0),
            totalPowerMw = units.sumOf { it.powerMw },
            units = units,
            servedTo = "${claims.name} (${claims.role})"
        )
        return json(request, 200, dto)
    }

    private fun gateCommand(request: Request, claims: TokenClaims): Response {
        val cmd = runCatching { gson.fromJson(request.bodyAsString(), GateCommandRequest::class.java) }.getOrNull()
            ?: return error(request, 400, "bad_request", "Некоректна команда")
        if (cmd.gateId !in 1..6 || cmd.openingPercent !in 0..100) {
            return error(request, 400, "validation_error", "Затвор 1–6, відкриття 0–100%")
        }
        return json(
            request, 200,
            GateCommandResponse(cmd.gateId, cmd.openingPercent, claims.name, "Команду прийнято: затвор №${cmd.gateId} → ${cmd.openingPercent}%")
        )
    }

    private fun json(request: Request, code: Int, body: Any): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message(if (code == 200) "OK" else "Error")
        .body(gson.toJson(body).toResponseBody(JSON))
        .build()

    private fun error(request: Request, code: Int, errorCode: String, message: String): Response =
        json(request, code, ApiErrorDto(errorCode, message))

    private fun Request.bodyAsString(): String = body?.let { b -> Buffer().also { b.writeTo(it) }.readUtf8() }.orEmpty()

    private fun MockUser.toDto() = UserDto(username, fullName, role.name, shift)

    companion object {
        // Секрет підпису існує лише на "сервері"; клієнтський код його не використовує.
        private const val SERVER_SECRET = "hydro-ges-mock-server-secret-2026"
        private val JSON = "application/json; charset=utf-8".toMediaType()

        val USERS = listOf(
            MockUser("admin", "Admin#2026", "Гогуля Максим (адміністратор)", UserRole.ADMIN, "Денна"),
            MockUser("engineer", "Engineer#2026", "Черговий інженер Коваль І.", UserRole.ENGINEER, "Нічна"),
            MockUser("observer", "Observer#2026", "Спостерігач Мельник О.", UserRole.OBSERVER, "—")
        )

        private val audit: MutableList<AuditEntryDto> = Collections.synchronizedList(mutableListOf())
    }
}
