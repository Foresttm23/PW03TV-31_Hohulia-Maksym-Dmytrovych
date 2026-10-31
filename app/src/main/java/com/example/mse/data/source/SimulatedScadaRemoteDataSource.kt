package com.example.mse.data.source

import com.example.mse.model.Reservoir
import com.example.mse.model.RemoteGaugeReading
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.UUID
import kotlin.random.Random

class SimulatedScadaRemoteDataSource(
    private val random: Random = Random.Default
) : GaugeRemoteDataSource {
    override suspend fun fetchAll(): List<RemoteGaugeReading> {
        simulateNetwork("GET /gauges")
        return lock.withLock { serverStorage.values.sortedByDescending { it.timestamp } }
    }

    override suspend fun upsert(reading: RemoteGaugeReading): RemoteGaugeReading {
        simulateNetwork("PUT /gauges")
        val stored = reading.copy(remoteId = reading.remoteId.ifBlank { "SRV-" + UUID.randomUUID().toString().take(8) })
        lock.withLock { serverStorage[stored.remoteId] = stored }
        return stored
    }

    override suspend fun delete(remoteId: String) {
        simulateNetwork("DELETE /gauges/$remoteId")
        lock.withLock { serverStorage.remove(remoteId) }
    }

    private suspend fun simulateNetwork(endpoint: String) {
        delay(latencyMs + random.nextLong(0, 400))
        requestCounter++
        if (random.nextDouble() < failureRate) {
            throw IOException("SCADA-сервер не відповідає ($endpoint): тайм-аут з'єднання")
        }
    }

    companion object {
        @Volatile var failureRate: Double = 0.0
        @Volatile var latencyMs: Long = 700
        @Volatile var requestCounter: Int = 0

        private val lock = Mutex()
        private val serverStorage: MutableMap<String, RemoteGaugeReading> = seed().associateBy { it.remoteId }.toMutableMap()

        private fun seed(): List<RemoteGaugeReading> {
            val now = System.currentTimeMillis()
            val hour = 3_600_000L
            val result = mutableListOf<RemoteGaugeReading>()
            data class Profile(val r: Reservoir, val startLevel: Double, val step: Double, val inflow: Double, val outflow: Double)
            val profiles = listOf(
                Profile(Reservoir.KYIV, 102.55, 0.09, 1250.0, 1100.0),
                Profile(Reservoir.KANIV, 91.35, 0.07, 1400.0, 1320.0),
                Profile(Reservoir.KREMENCHUK, 80.20, -0.12, 1500.0, 1780.0)
            )
            profiles.forEach { p ->
                for (i in 0 until 5) {
                    result += RemoteGaugeReading(
                        remoteId = "SRV-${p.r.codePrefix}-$i",
                        reservoir = p.r.name,
                        gaugeCode = "${p.r.codePrefix}-0${i % 2 + 1}",
                        waterLevelM = p.startLevel + p.step * i,
                        inflowM3s = p.inflow + i * 35,
                        outflowM3s = p.outflow + i * 10,
                        timestamp = now - (4 - i) * 6 * hour
                    )
                }
            }
            return result
        }
    }
}

class MemoryCache<T>(
    private val ttlMs: Long,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private var value: T? = null
    private var storedAt: Long = 0

    fun put(newValue: T) {
        value = newValue
        storedAt = clock()
    }

    fun getIfFresh(): T? = value?.takeIf { clock() - storedAt < ttlMs }

    fun ageMs(): Long? = if (value == null) null else clock() - storedAt

    fun invalidate() {
        value = null
    }
}
