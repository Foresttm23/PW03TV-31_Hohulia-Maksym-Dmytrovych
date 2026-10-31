package com.example.mse.data.source

import com.example.mse.data.db.GaugeReadingDao
import com.example.mse.model.GaugeReadingEntity
import com.example.mse.model.RemoteGaugeReading
import kotlinx.coroutines.flow.Flow

interface GaugeLocalDataSource {
    fun observe(query: String): Flow<List<GaugeReadingEntity>>
    suspend fun getById(id: Long): GaugeReadingEntity?
    suspend fun getByRemoteId(remoteId: String): GaugeReadingEntity?
    suspend fun getUnsynced(): List<GaugeReadingEntity>
    suspend fun insert(entity: GaugeReadingEntity): Long
    suspend fun update(entity: GaugeReadingEntity)
    suspend fun delete(id: Long)
}

interface GaugeRemoteDataSource {
    suspend fun fetchAll(): List<RemoteGaugeReading>
    suspend fun upsert(reading: RemoteGaugeReading): RemoteGaugeReading
    suspend fun delete(remoteId: String)
}

class RoomGaugeLocalDataSource(private val dao: GaugeReadingDao) : GaugeLocalDataSource {
    override fun observe(query: String): Flow<List<GaugeReadingEntity>> =
        if (query.isBlank()) dao.observeAll() else dao.search(query.trim())

    override suspend fun getById(id: Long) = dao.getById(id)
    override suspend fun getByRemoteId(remoteId: String) = dao.getByRemoteId(remoteId)
    override suspend fun getUnsynced() = dao.getUnsynced()
    override suspend fun insert(entity: GaugeReadingEntity) = dao.insert(entity)
    override suspend fun update(entity: GaugeReadingEntity) = dao.update(entity)
    override suspend fun delete(id: Long) = dao.deleteById(id)
}
