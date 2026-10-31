package com.example.mse

import com.example.mse.data.error.ErrorHandler
import com.example.mse.data.error.ErrorLogger
import com.example.mse.data.error.NetworkMonitor
import com.example.mse.data.error.RetryPolicy
import com.example.mse.data.remote.TelemetryNetwork
import com.example.mse.data.remote.mock.FaultInjectionScadaServer
import com.example.mse.data.repository.TelemetryRepository
import com.example.mse.data.repository.TelemetryResult
import com.example.mse.model.AppError
import com.example.mse.model.ErrorLogEntry
import com.example.mse.model.ErrorPresentation
import com.example.mse.model.FaultScenario
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingLogger : ErrorLogger {
    val entries = mutableListOf<ErrorLogEntry>()
    override fun log(entry: ErrorLogEntry) { synchronized(entries) { entries += entry } }
}

class NetworkErrorHandlingTest {
    private class Setup {
        val logger = RecordingLogger()
        val server = FaultInjectionScadaServer(latencyMs = 0, timeoutDelayMs = 0)
        val monitor = object : NetworkMonitor {
            override fun isOnline() = true
            override val onlineFlow = flowOf(true)
        }
        val repo = TelemetryRepository(
            TelemetryNetwork.createApi(server), ErrorHandler(logger), monitor,
            RetryPolicy(maxAttempts = 3, initialDelayMs = 5)
        )
    }

    @Test
    fun transientFailureIsRecoveredByRetry() = runBlocking {
        val s = Setup().apply { server.scenario = FaultScenario.FLAKY_503 }
        val result = s.repo.fetchTelemetry("KYV-01", 12) as TelemetryResult.Success
        assertEquals("503, 503 → 200: успіх з третьої спроби", 3, result.attempts)
        assertEquals("Київська ГЕС", result.data.station?.name)
        assertEquals("Обидві невдалі спроби зареєстровано", 2, s.logger.entries.count { it.willRetry && it.httpCode == 503 })
    }

    @Test
    fun notFoundIsNotRetriedAndShownInDialog() = runBlocking {
        val s = Setup()
        val result = s.repo.fetchTelemetry("KYV-99", 12) as TelemetryResult.Failure
        assertTrue(result.error is AppError.NotFound)
        assertTrue(result.error.message.contains("KYV-99"))
        assertFalse(result.error.isRetryable)
        assertEquals(ErrorPresentation.DIALOG, result.error.presentation)
        assertEquals("Постійна помилка — без повторів", 1, result.attempts)
        assertFalse(s.logger.entries.single().willRetry)
    }
}
