package com.example.mse.model

/**
 * Модель даних показників Гідроелектростанції (ГЕС) з контролем рівня води.
 */
data class HydroStationData(
    val waterLevelMeters: Float = 120.5f,
    val targetWaterLevelMeters: Float = 122.0f,
    val waterFlowRateM3PerSec: Float = 350.0f,
    val generatedPowerMw: Float = 45.0f,
    val waterHeadMeters: Float = 15.0f
)

/**
 * Режими роботи Гідроелектростанції (ГЕС).
 */
enum class HydroOperatingMode(val title: String, val description: String) {
    NORMAL_FLOW("Нормальний приплив", "Стандартний режим підтримання нормального підпірного рівня (НПУ)"),
    PEAK_LOAD("Пікове навантаження", "Максимальне скидання води через турбіни для покриття піків споживання"),
    FLOOD_CONTROL("Пропуск паводку", "Відкриття холостих затворів для запобігання переповненню водосховища"),
    ECO_MINIMUM("Екологічний попуск", "Мінімальна витрата води для збереження екосистеми нижнього б'єфу")
}
