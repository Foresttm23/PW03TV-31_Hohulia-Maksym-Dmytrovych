package com.example.mse

import com.example.mse.data.repository.GaugeReadingRepository
import com.example.mse.data.repository.WriteOutcome
import com.example.mse.data.source.GaugeLocalDataSource
import com.example.mse.data.source.GaugeRemoteDataSource
import com.example.mse.data.source.MemoryCache
import com.example.mse.model.DataOrigin
import com.example.mse.model.DataResult
import com.example.mse.model.GaugeReadingEntity
import com.example.mse.model.RemoteGaugeReading
import com.example.mse.model.Reservoir
import com.example.mse.model.SourceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class FakeGaugeLocalDataSource : GaugeLocalDataSource {
    val rows = MutableStateFlow<List<GaugeReadingEntity>>(emptyList())
    private var nextId = 1L

    override fun observe(query: String): Flow<List<GaugeReadingEntity>> =
        rows.map { list -> list.filter { query.isBlank() || it.gaugeCode.contains(query, ignoreCase = true) } }

    override suspend fun getById(id: Long) = rows.value.firstOrNull { it.id == id }
    override suspend fun getByRemoteId(remoteId: String) = rows.value.firstOrNull { it.remoteId == remoteId }
    override suspend fun getUnsynced() = rows.value.filter { !it.isSynced }
    override suspend fun insert(entity: GaugeReadingEntity): Long {
        val id = if (entity.id == 0L) nextId++ else entity.id
        rows.value = rows.value.filterNot { it.id == id } + entity.copy(id = id)
        return id
    }
    override suspend fun update(entity: GaugeReadingEntity) {
        rows.value = rows.value.map { if (it.id == entity.id) entity else it }
    }
    override suspend fun delete(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }
}

class FakeGaugeRemoteDataSource(initial: List<RemoteGaugeReading> = emptyList()) : GaugeRemoteDataSource {
    val storage = initial.associateBy { it.remoteId }.toMutableMap()
    var failing = false
    var fetchCalls = 0
    private var counter = 0

    override suspend fun fetchAll(): List<RemoteGaugeReading> {
        fetchCalls++
        if (failing) throw IOException("offline")
        return storage.values.toList()
    }
    override suspend fun upsert(reading: RemoteGaugeReading): RemoteGaugeReading {
        if (failing) throw IOException("offline")
        val stored = reading.copy(remoteId = reading.remoteId.ifBlank { "R${++counter}" })
        storage[stored.remoteId] = stored
        return stored
    }
    override suspend fun delete(remoteId: String) {
        if (failing) throw IOException("offline")
        storage.remove(remoteId)
    }
}

class GaugeReadingRepositoryTest {
    private fun remoteReading(id: String, level: Double) =
        RemoteGaugeReading(id, Reservoir.KYIV.name, "KYV-01", level, 1200.0, 1100.0, 1_000)

    @Test
    fun smartModeUsesNetworkFirstThenMemoryCache() = runTest {
        val local = FakeGaugeLocalDataSource()
        val remote = FakeGaugeRemoteDataSource(listOf(remoteReading("A", 102.5), remoteReading("B", 102.7)))
        val repo = GaugeReadingRepository(local, remote, MemoryCache(60_000), StandardTestDispatcher(testScheduler), retryDelayMs = 1)

        val first = repo.refresh(SourceMode.SMART) as DataResult.Success
        assertEquals(DataOrigin.NETWORK, first.source)
        assertEquals(2, first.data.inserted)

        val second = repo.refresh(SourceMode.SMART) as DataResult.Success
        assertEquals(DataOrigin.MEMORY_CACHE, second.source)
        assertEquals("Другий запит має обслуговуватись кешем", 1, remote.fetchCalls)
        assertEquals(2, local.rows.value.size)
    }

    @Test
    fun offlineAddIsSavedLocallyAndSyncedLater() = runTest {
        val local = FakeGaugeLocalDataSource()
        val remote = FakeGaugeRemoteDataSource().apply { failing = true }
        val repo = GaugeReadingRepository(local, remote, ioDispatcher = StandardTestDispatcher(testScheduler), retryDelayMs = 1)

        val added = repo.add(Reservoir.KANIV, "knv-02", 91.2, 1300.0, 1250.0) as DataResult.Success
        assertEquals(WriteOutcome.SAVED_LOCALLY, added.data)
        assertFalse(local.rows.value.single().isSynced)
        assertEquals("KNV-02", local.rows.value.single().gaugeCode)

        remote.failing = false
        val synced = repo.syncPending() as DataResult.Success
        assertEquals(1, synced.data)
        assertTrue(local.rows.value.single().isSynced)
        assertEquals(1, remote.storage.size)
    }
}
