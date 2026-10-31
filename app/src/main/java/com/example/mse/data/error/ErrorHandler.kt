package com.example.mse.data.error

import com.example.mse.model.AppError
import com.example.mse.model.ErrorContext
import com.example.mse.model.ErrorLogEntry
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import retrofit2.HttpException
import java.io.EOFException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class NoNetworkException : IOException("Пристрій не підключений до мережі")

class InvalidResponseException(message: String) : Exception(message)

class ErrorHandler(
    private val logger: ErrorLogger,
    private val clock: () -> Long = System::currentTimeMillis
) {
    fun classify(error: Throwable, resource: String = "—"): AppError = when (error) {
        is NoNetworkException, is UnknownHostException, is ConnectException -> AppError.NoConnection()
        is SocketTimeoutException -> AppError.Timeout()
        is MalformedJsonException, is JsonParseException, is EOFException -> AppError.Parsing(error.message?.take(80))
        is InvalidResponseException -> AppError.Parsing(error.message)
        is HttpException -> fromHttp(error, resource)
        is SSLException -> AppError.Unknown("помилка захищеного з'єднання")
        is InterruptedIOException -> AppError.Timeout()
        is IOException -> AppError.NoConnection()
        else -> AppError.Unknown(error.message ?: error.javaClass.simpleName)
    }

    fun handle(error: Throwable, context: ErrorContext, resource: String = "—"): AppError {
        val appError = classify(error, resource)
        logger.log(
            ErrorLogEntry(
                timestamp = clock(),
                type = appError.type,
                httpCode = appError.httpCode,
                endpoint = context.endpoint,
                attempt = context.attempt,
                maxAttempts = context.maxAttempts,
                willRetry = context.willRetry,
                technicalMessage = "${error.javaClass.simpleName}: ${error.message}"
            )
        )
        return appError
    }

    private fun fromHttp(e: HttpException, resource: String): AppError = when (val code = e.code()) {
        400 -> AppError.BadRequest(e.message().takeIf { it.isNotBlank() })
        401 -> AppError.Unauthorized()
        403 -> AppError.Forbidden()
        404 -> AppError.NotFound(resource)
        408 -> AppError.Timeout()
        429 -> AppError.RateLimited(retryAfterSeconds(e))
        in 500..599 -> AppError.ServerError(code)
        else -> AppError.Unknown("HTTP $code")
    }

    fun retryAfterSeconds(error: Throwable): Long? =
        (error as? HttpException)?.response()?.headers()?.get("Retry-After")?.toLongOrNull()
}
