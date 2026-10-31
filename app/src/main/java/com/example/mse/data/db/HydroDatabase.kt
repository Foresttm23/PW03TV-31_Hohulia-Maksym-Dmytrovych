package com.example.mse.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mse.model.CachedForecastEntity
import com.example.mse.model.GaugeReadingEntity
import com.example.mse.model.HydroRecordEntity

@Database(
    entities = [HydroRecordEntity::class, GaugeReadingEntity::class, CachedForecastEntity::class],
    version = 2,
    exportSchema = false
)
abstract class HydroDatabase : RoomDatabase() {
    abstract fun hydroRecordDao(): HydroRecordDao
    abstract fun gaugeReadingDao(): GaugeReadingDao
    abstract fun cachedForecastDao(): CachedForecastDao

    companion object {
        @Volatile
        private var INSTANCE: HydroDatabase? = null

        fun getInstance(context: Context): HydroDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HydroDatabase::class.java,
                    "hydro_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
