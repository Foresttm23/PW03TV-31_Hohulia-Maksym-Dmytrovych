package com.example.mse.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mse.model.CachedForecastEntity

@Dao
interface CachedForecastDao {
    @Query("SELECT * FROM forecast_cache WHERE stationId = :stationId")
    suspend fun get(stationId: String): CachedForecastEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedForecastEntity)

    @Query("DELETE FROM forecast_cache")
    suspend fun clear()
}
