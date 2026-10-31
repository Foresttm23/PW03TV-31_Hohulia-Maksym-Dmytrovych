package com.example.mse.data.repository

import com.example.mse.data.error.ErrorHandler
import com.example.mse.data.error.InvalidResponseException
import com.example.mse.data.error.NetworkMonitor
import com.example.mse.data.error.NoNetworkException
import com.example.mse.data.error.RetryEvent
import com.example.mse.data.error.RetryPolicy
import com.example.mse.data.error.retryWithBackoff
import com.example.mse.data.remote.TelemetryApi
import com.example.mse.model.AppError
import com.example.mse.model.ErrorContext
import com.example.mse.model.StationTelemetryResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface TelemetryResult {
    data class Success(val data: StationTelemetryResponse, val attempts: Int) : TelemetryResult
    data class Failure(val error: AppError, val attempts: Int) : TelemetryResult
}

class TelemetryRepository(
    private val api: TelemetryApi,
    private val errorHandler: ErrorHandler,
    private val networkMonitor: NetworkMonitor,
    private val policy: RetryPolicy = RetryPolicy(maxAttempts = 3, initialDelayMs = 700),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    @Volatile var forceOffline: Boolean = false

    suspend fun fetchTelemetry(
        stationCode: String,
        hours: Int,
        onRetry: suspend (RetryEvent, AppError) -> Unit = { _, _ -> }
    ): TelemetryResult = withContext(ioDispatcher) {
        val endpoint = "GET /api/telemetry/$stationCode?hours=$hours"

        if (forceOffline || !networkMonitor.isOnline()) {
            val error = errorHandler.handle(NoNetworkException(), ErrorContext(endpoint, 0, policy.maxAttempts, false))
            return@withContext TelemetryResult.Failure(error, attempts = 0)
        }

        var attemptsMade = 0
        try {
            val response = retryWithBackoff(
                policy = policy,
                shouldRetry = { errorHandler.classify(it, stationCode).isRetryable },
                delayOverride = { e -> errorHandler.retryAfterSeconds(e)?.let { it * 1000 } },
                onRetry = { event ->
                    val error = errorHandler.handle(
                        event.cause,
                        ErrorContext(endpoint, event.nextAttempt - 1, policy.maxAttempts, willRetry = true),
                        stationCode
                    )
                    onRetry(event, error)
                }
            ) { attempt ->
                attemptsMade = attempt
                api.getTelemetry(stationCode, hours)
            }
            TelemetryResult.Success(validate(response), attemptsMade)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            val error = errorHandler.handle(e, ErrorContext(endpoint, attemptsMade, policy.maxAttempts, willRetry = false), stationCode)
            TelemetryResult.Failure(error, attemptsMade)
        }
    }

    private fun validate(r: StationTelemetryResponse): StationTelemetryResponse {
        val reservoir = r.reservoir ?: throw InvalidResponseException("відсутній блок reservoir")
        if (r.station == null) throw InvalidResponseException("відсутній блок station")
        if (r.flows == null) throw InvalidResponseException("відсутній блок flows")
        if (reservoir.levelM !in 0.0..500.0) throw InvalidResponseException("рівень ${reservoir.levelM} м поза межами")
        return r
    }
}
