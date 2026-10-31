package com.example.mse.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mse.model.GaugeReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GaugeReadingDao {
    @Query("SELECT * FROM gauge_readings ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<GaugeReadingEntity>>

    @Query("SELECT * FROM gauge_readings WHERE gaugeCode LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<GaugeReadingEntity>>

    @Query("SELECT * FROM gauge_readings WHERE id = :id")
    suspend fun getById(id: Long): GaugeReadingEntity?

    @Query("SELECT * FROM gauge_readings WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): GaugeReadingEntity?

    @Query("SELECT * FROM gauge_readings WHERE isSynced = 0")
    suspend fun getUnsynced(): List<GaugeReadingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: GaugeReadingEntity): Long

    @Update
    suspend fun update(entity: GaugeReadingEntity)

    @Query("DELETE FROM gauge_readings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM gauge_readings")
    suspend fun clear()
}
