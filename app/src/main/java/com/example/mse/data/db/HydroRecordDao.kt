package com.example.mse.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mse.model.HydroRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) для Room Database (Практична 8).
 */
@Dao
interface HydroRecordDao {

    @Query("SELECT * FROM hydro_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<HydroRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: HydroRecordEntity)

    @Query("DELETE FROM hydro_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM hydro_records")
    suspend fun clearAllRecords()
}
