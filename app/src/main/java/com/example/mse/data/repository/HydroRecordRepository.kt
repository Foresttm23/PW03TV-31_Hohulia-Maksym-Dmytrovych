package com.example.mse.data.repository

import com.example.mse.data.db.HydroRecordDao
import com.example.mse.model.HydroRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторій для роботи з даними ГЕС (Clean Architecture).
 * Абстрагує джерело даних (Room DB, remote API) від ViewModel.
 */
interface HydroRecordRepository {
    fun getAllRecords(): Flow<List<HydroRecordEntity>>
    suspend fun insertRecord(record: HydroRecordEntity)
    suspend fun deleteRecordById(id: Long)
    suspend fun clearAllRecords()
}

/**
 * Реалізація репозиторію на основі Room DAO.
 */
class HydroRecordRepositoryImpl(
    private val dao: HydroRecordDao
) : HydroRecordRepository {

    override fun getAllRecords(): Flow<List<HydroRecordEntity>> = dao.getAllRecords()

    override suspend fun insertRecord(record: HydroRecordEntity) {
        dao.insertRecord(record)
    }

    override suspend fun deleteRecordById(id: Long) {
        dao.deleteRecordById(id)
    }

    override suspend fun clearAllRecords() {
        dao.clearAllRecords()
    }
}
