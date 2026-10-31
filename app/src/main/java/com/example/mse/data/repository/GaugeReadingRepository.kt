package com.example.mse.data.repository

import com.example.mse.data.source.GaugeLocalDataSource
import com.example.mse.data.source.GaugeRemoteDataSource
import com.example.mse.data.source.MemoryCache
import com.example.mse.logic.ReservoirAnalytics
import com.example.mse.model.DataOrigin
import com.example.mse.model.DataResult
import com.example.mse.model.GaugeReading
import com.example.mse.model.GaugeReadingEntity
import com.example.mse.model.ReadingFilter
import com.example.mse.model.RemoteGaugeReading
import com.example.mse.model.Reservoir
import com.example.mse.model.ReservoirDashboard
import com.example.mse.model.SourceMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class RefreshReport(
    val inserted: Int = 0,
    val updated: Int = 0,
    val removed: Int = 0,
    val pushed: Int = 0
)

enum class WriteOutcome { SYNCED, SAVED_LOCALLY }

// Offline-first: UI читає лише Room через Flow, мережа використовується тільки для синхронізації.
class GaugeReadingRepository(
    private val local: GaugeLocalDataSource,
    private val remote: GaugeRemoteDataSource,
    private val cache: MemoryCache<List<RemoteGaugeReading>> = MemoryCache(ttlMs = 30_000),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val maxAttempts: Int = 2,
    private val retryDelayMs: Long = 400
) {
    private val syncMutex = Mutex()

    fun observeDashboard(filter: ReadingFilter): Flow<ReservoirDashboard> =
        local.observe(filter.query)
            .map { entities ->
                val all = entities.map { it.toDomain() }
                val visible = ReservoirAnalytics.applyFilter(all, filter)
                ReservoirDashboard(
                    readings = visible,
                    stats = ReservoirAnalytics.aggregate(visible),
                    totalCount = all.size,
                    unsyncedCount = all.count { !it.isSynced }
                )
            }
            .flowOn(Dispatchers.Default)

    suspend fun refresh(mode: SourceMode): DataResult<RefreshReport> = withContext(ioDispatcher) {
        syncMutex.withLock {
            when (mode) {
                SourceMode.LOCAL -> DataResult.Success(RefreshReport(), DataOrigin.LOCAL_DB)
                SourceMode.SMART -> {
                    val cached = cache.getIfFresh()
                    if (cached != null) {
                        DataResult.Success(mergeIntoLocal(cached), DataOrigin.MEMORY_CACHE)
                    } else {
                        syncWithServer()
                    }
                }
                SourceMode.REMOTE -> {
                    cache.invalidate()
                    syncWithServer()
                }
            }
        }
    }

    private suspend fun syncWithServer(): DataResult<RefreshReport> = try {
        val pushed = pushPendingInternal()
        val serverData = withRetry { remote.fetchAll() }
        cache.put(serverData)
        DataResult.Success(mergeIntoLocal(serverData).copy(pushed = pushed), DataOrigin.NETWORK)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DataResult.Error(
            message = "Сервер недоступний: ${e.message}. Показано дані з локальної БД.",
            cause = e,
            fallbackData = RefreshReport()
        )
    }

    private suspend fun mergeIntoLocal(serverData: List<RemoteGaugeReading>): RefreshReport {
        var inserted = 0
        var updated = 0
        serverData.forEach { remoteItem ->
            val existing = local.getByRemoteId(remoteItem.remoteId)
            val entity = remoteItem.toEntity(localId = existing?.id ?: 0)
            when {
                existing == null -> { local.insert(entity); inserted++ }
                existing.copy(isSynced = true) != entity -> { local.update(entity); updated++ }
            }
        }
        // Несинхронізовані локальні записи не видаляються: вони ще не дійшли до сервера.
        val serverIds = serverData.map { it.remoteId }.toSet()
        val stale = local.observeSnapshot().filter { it.isSynced && it.remoteId != null && it.remoteId !in serverIds }
        stale.forEach { local.delete(it.id) }
        return RefreshReport(inserted = inserted, updated = updated, removed = stale.size)
    }

    suspend fun add(
        reservoir: Reservoir,
        gaugeCode: String,
        level: Double,
        inflow: Double,
        outflow: Double
    ): DataResult<WriteOutcome> = withContext(ioDispatcher) {
        runCatchingLocal {
            val id = local.insert(
                GaugeReadingEntity(
                    reservoir = reservoir.name,
                    gaugeCode = gaugeCode.trim().uppercase(),
                    waterLevelM = level,
                    inflowM3s = inflow,
                    outflowM3s = outflow,
                    isSynced = false
                )
            )
            pushOne(id)
        }
    }

    suspend fun update(reading: GaugeReading): DataResult<WriteOutcome> = withContext(ioDispatcher) {
        runCatchingLocal {
            val current = local.getById(reading.id) ?: error("Запис #${reading.id} не знайдено")
            local.update(
                current.copy(
                    reservoir = reading.reservoir.name,
                    gaugeCode = reading.gaugeCode.trim().uppercase(),
                    waterLevelM = reading.waterLevelM,
                    inflowM3s = reading.inflowM3s,
                    outflowM3s = reading.outflowM3s,
                    timestamp = System.currentTimeMillis(),
                    isSynced = false
                )
            )
            pushOne(reading.id)
        }
    }

    // Спершу видалення на сервері: інакше запис "воскресне" після наступної синхронізації.
    suspend fun delete(reading: GaugeReading): DataResult<WriteOutcome> = withContext(ioDispatcher) {
        try {
            reading.remoteId?.let { remoteId -> withRetry { remote.delete(remoteId) } }
            local.delete(reading.id)
            cache.invalidate()
            DataResult.Success(WriteOutcome.SYNCED, DataOrigin.NETWORK)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DataResult.Error("Не вдалося видалити запис на сервері: ${e.message}", e)
        }
    }

    suspend fun syncPending(): DataResult<Int> = withContext(ioDispatcher) {
        try {
            DataResult.Success(syncMutex.withLock { pushPendingInternal() }, DataOrigin.NETWORK)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DataResult.Error("Синхронізація не вдалася: ${e.message}", e)
        }
    }

    private suspend fun pushPendingInternal(): Int {
        val pending = local.getUnsynced()
        pending.forEach { pushEntity(it) }
        return pending.size
    }

    private suspend fun pushOne(localId: Long): WriteOutcome {
        val entity = local.getById(localId) ?: return WriteOutcome.SAVED_LOCALLY
        return try {
            pushEntity(entity)
            WriteOutcome.SYNCED
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            WriteOutcome.SAVED_LOCALLY
        }
    }

    private suspend fun pushEntity(entity: GaugeReadingEntity) {
        val stored = withRetry { remote.upsert(entity.toRemote()) }
        local.update(entity.copy(remoteId = stored.remoteId, isSynced = true))
        cache.invalidate()
    }

    private suspend fun <T> withRetry(block: suspend () -> T): T {
        var lastError: Exception? = null
        repeat(maxAttempts) { attempt ->
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                if (attempt < maxAttempts - 1) delay(retryDelayMs * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("Невідома помилка")
    }

    private suspend fun runCatchingLocal(block: suspend () -> WriteOutcome): DataResult<WriteOutcome> = try {
        val outcome = block()
        DataResult.Success(outcome, if (outcome == WriteOutcome.SYNCED) DataOrigin.NETWORK else DataOrigin.LOCAL_DB)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DataResult.Error("Помилка локальної БД: ${e.message}", e)
    }
}

private suspend fun GaugeLocalDataSource.observeSnapshot(): List<GaugeReadingEntity> =
    observe("").first()

fun GaugeReadingEntity.toDomain() = GaugeReading(
    id = id,
    remoteId = remoteId,
    reservoir = Reservoir.fromName(reservoir),
    gaugeCode = gaugeCode,
    waterLevelM = waterLevelM,
    inflowM3s = inflowM3s,
    outflowM3s = outflowM3s,
    timestamp = timestamp,
    isSynced = isSynced
)

private fun GaugeReadingEntity.toRemote() = RemoteGaugeReading(
    remoteId = remoteId.orEmpty(),
    reservoir = reservoir,
    gaugeCode = gaugeCode,
    waterLevelM = waterLevelM,
    inflowM3s = inflowM3s,
    outflowM3s = outflowM3s,
    timestamp = timestamp
)

private fun RemoteGaugeReading.toEntity(localId: Long) = GaugeReadingEntity(
    id = localId,
    remoteId = remoteId,
    reservoir = reservoir,
    gaugeCode = gaugeCode,
    waterLevelM = waterLevelM,
    inflowM3s = inflowM3s,
    outflowM3s = outflowM3s,
    timestamp = timestamp,
    isSynced = true
)
