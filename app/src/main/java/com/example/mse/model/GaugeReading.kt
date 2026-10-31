package com.example.mse.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Reservoir(
    val title: String,
    val codePrefix: String,
    val deadLevel: Double,
    val normalLevel: Double,
    val forcedLevel: Double
) {
    KYIV("Київське", "KYV", 101.0, 103.0, 104.0),
    KANIV("Канівське", "KNV", 90.5, 91.5, 92.5),
    KREMENCHUK("Кременчуцьке", "KRM", 75.0, 81.0, 82.0);

    fun fillPercent(level: Double): Double =
        ((level - deadLevel) / (normalLevel - deadLevel) * 100.0)

    fun statusOf(level: Double): LevelStatus = when {
        level >= forcedLevel - 0.3 -> LevelStatus.CRITICAL
        level > normalLevel -> LevelStatus.WARNING
        level < deadLevel + 0.3 -> LevelStatus.LOW
        else -> LevelStatus.NORMAL
    }

    companion object {
        fun fromName(name: String): Reservoir = entries.firstOrNull { it.name == name } ?: KYIV
    }
}

enum class LevelStatus(val title: String) {
    LOW("Наближення до РМО"),
    NORMAL("Норма"),
    WARNING("Вище НПУ"),
    CRITICAL("Загроза ФПУ")
}

@Entity(
    tableName = "gauge_readings",
    indices = [Index(value = ["remoteId"], unique = true)]
)
data class GaugeReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: String? = null,
    val reservoir: String,
    val gaugeCode: String,
    val waterLevelM: Double,
    val inflowM3s: Double,
    val outflowM3s: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

data class GaugeReading(
    val id: Long,
    val remoteId: String?,
    val reservoir: Reservoir,
    val gaugeCode: String,
    val waterLevelM: Double,
    val inflowM3s: Double,
    val outflowM3s: Double,
    val timestamp: Long,
    val isSynced: Boolean
) {
    val balanceM3s: Double get() = inflowM3s - outflowM3s
    val status: LevelStatus get() = reservoir.statusOf(waterLevelM)
    val fillPercent: Double get() = reservoir.fillPercent(waterLevelM)
}

data class RemoteGaugeReading(
    val remoteId: String,
    val reservoir: String,
    val gaugeCode: String,
    val waterLevelM: Double,
    val inflowM3s: Double,
    val outflowM3s: Double,
    val timestamp: Long
)

enum class ReadingSort(val title: String) {
    NEWEST("Спочатку нові"),
    OLDEST("Спочатку старі"),
    LEVEL_DESC("Рівень ↓"),
    LEVEL_ASC("Рівень ↑"),
    BALANCE_DESC("Баланс ↓")
}

data class ReadingFilter(
    val query: String = "",
    val reservoir: Reservoir? = null,
    val status: LevelStatus? = null,
    val sort: ReadingSort = ReadingSort.NEWEST
)

enum class SourceMode(val title: String, val description: String) {
    LOCAL("Локальне", "Тільки Room, без звернень до мережі"),
    REMOTE("Віддалене", "Завжди примусовий запит до SCADA-сервера"),
    SMART("Агреговане", "Repository сам обирає: кеш → сервер → Room")
}

data class ReservoirStats(
    val reservoir: Reservoir,
    val readingsCount: Int,
    val avgLevel: Double,
    val minLevel: Double,
    val maxLevel: Double,
    val latestLevel: Double,
    val avgBalanceM3s: Double,
    val trendMPerHour: Double,
    val criticalCount: Int,
    val latestStatus: LevelStatus
) {
    val latestFillPercent: Double get() = reservoir.fillPercent(latestLevel)

    val hoursToNormalLevel: Double?
        get() = if (trendMPerHour > 0.0001 && latestLevel < reservoir.normalLevel)
            (reservoir.normalLevel - latestLevel) / trendMPerHour else null
}

data class ReservoirDashboard(
    val readings: List<GaugeReading> = emptyList(),
    val stats: List<ReservoirStats> = emptyList(),
    val totalCount: Int = 0,
    val unsyncedCount: Int = 0
)
