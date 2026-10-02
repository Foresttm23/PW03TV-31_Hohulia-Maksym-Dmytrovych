package com.example.mse

import com.example.mse.data.db.HydroRecordDao
import com.example.mse.data.repository.HydroRecordRepositoryImpl
import com.example.mse.model.HydroRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeHydroRecordDao : HydroRecordDao {
    private val records = mutableListOf<HydroRecordEntity>()

    override fun getAllRecords(): Flow<List<HydroRecordEntity>> {
        return flowOf(records.toList())
    }

    override suspend fun insertRecord(record: HydroRecordEntity) {
        records.add(record)
    }

    override suspend fun deleteRecordById(id: Long) {
        records.removeAll { it.id == id }
    }

    override suspend fun clearAllRecords() {
        records.clear()
    }
}

class HydroRecordRepositoryTest {

    @Test
    fun testRepositoryInsertAndFetch() = runBlocking {
        val fakeDao = FakeHydroRecordDao()
        val repository = HydroRecordRepositoryImpl(fakeDao)

        val entity = HydroRecordEntity(
            id = 100,
            waterLevelMeters = 122.5f,
            generatedPowerMw = 50.0f,
            waterFlowRateM3PerSec = 400.0f,
            operatingMode = "Нормальний приплив",
            operatorNotes = "Тест репозиторію"
        )

        repository.insertRecord(entity)

        val resultList = repository.getAllRecords().first()
        assertEquals(1, resultList.size)
        assertEquals(122.5f, resultList[0].waterLevelMeters)
    }

    @Test
    fun testRepositoryDeleteAndClear() = runBlocking {
        val fakeDao = FakeHydroRecordDao()
        val repository = HydroRecordRepositoryImpl(fakeDao)

        val entity1 = HydroRecordEntity(id = 1, waterLevelMeters = 120f, generatedPowerMw = 40f, waterFlowRateM3PerSec = 300f, operatingMode = "НПУ", operatorNotes = "1")
        val entity2 = HydroRecordEntity(id = 2, waterLevelMeters = 121f, generatedPowerMw = 42f, waterFlowRateM3PerSec = 310f, operatingMode = "НПУ", operatorNotes = "2")

        repository.insertRecord(entity1)
        repository.insertRecord(entity2)

        repository.deleteRecordById(1)
        var resultList = repository.getAllRecords().first()
        assertEquals(1, resultList.size)
        assertEquals(2L, resultList[0].id)

        repository.clearAllRecords()
        resultList = repository.getAllRecords().first()
        assertEquals(0, resultList.size)
    }
}
