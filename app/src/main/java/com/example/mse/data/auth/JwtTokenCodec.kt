package com.example.mse.data.auth

import com.example.mse.model.TokenClaims
import com.google.gson.Gson
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object JwtTokenCodec {
    private val gson = Gson()
    private const val HEADER_JSON = """{"alg":"HS256","typ":"JWT"}"""

    sealed interface Verification {
        data class Valid(val claims: TokenClaims) : Verification
        data class Expired(val claims: TokenClaims) : Verification
        data object BadSignature : Verification
        data object Malformed : Verification
    }

    fun sign(claims: TokenClaims, secret: String): String {
        val header = encode(HEADER_JSON.toByteArray())
        val payload = encode(gson.toJson(claims).toByteArray())
        return "$header.$payload.${signature("$header.$payload", secret)}"
    }

    fun verify(token: String, secret: String, nowSeconds: Long): Verification {
        val parts = token.split('.')
        if (parts.size != 3) return Verification.Malformed
        val claims = decodeClaims(parts[1]) ?: return Verification.Malformed
        if (signature("${parts[0]}.${parts[1]}", secret) != parts[2]) return Verification.BadSignature
        return if (claims.exp <= nowSeconds) Verification.Expired(claims) else Verification.Valid(claims)
    }

    // Клієнт не знає секрету й не може перевірити підпис: claims використовуються лише для адаптації UI.
    fun decodeUnverified(token: String): TokenClaims? {
        val parts = token.split('.')
        return if (parts.size == 3) decodeClaims(parts[1]) else null
    }

    // Демонстрація атаки: змінена роль без нового підпису, такий токен відхиляє сервер.
    fun forgeRole(token: String, newRole: String): String {
        val parts = token.split('.')
        val claims = decodeUnverified(token) ?: return token
        val forgedPayload = encode(gson.toJson(claims.copy(role = newRole)).toByteArray())
        return "${parts[0]}.$forgedPayload.${parts.getOrElse(2) { "" }}"
    }

    private fun decodeClaims(part: String): TokenClaims? = try {
        gson.fromJson(String(decode(part)), TokenClaims::class.java)?.takeIf { it.sub.isNotBlank() }
    } catch (e: Exception) {
        null
    }

    private fun signature(data: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return encode(mac.doFinal(data.toByteArray()))
    }

    private fun encode(bytes: ByteArray): String = Base64.UrlSafe.encode(bytes).trimEnd('=')

    private fun decode(text: String): ByteArray {
        val padded = text + "=".repeat((4 - text.length % 4) % 4)
        return Base64.UrlSafe.decode(padded)
    }
}
