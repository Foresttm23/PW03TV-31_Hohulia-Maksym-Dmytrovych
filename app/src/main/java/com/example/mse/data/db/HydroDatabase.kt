package com.example.mse.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mse.model.HydroRecordEntity

/**
 * Головний клас Room Database локального сховища ГЕС (Практична 8).
 */
@Database(entities = [HydroRecordEntity::class], version = 1, exportSchema = false)
abstract class HydroDatabase : RoomDatabase() {

    abstract fun hydroRecordDao(): HydroRecordDao

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
