package com.example.mse.model

enum class ErrorPresentation {
    SNACKBAR,
    DIALOG,
    INLINE
}

sealed class AppError(
    val type: String,
    val title: String,
    val message: String,
    val httpCode: Int? = null,
    val isRetryable: Boolean,
    val presentation: ErrorPresentation
) {
    class NoConnection : AppError(
        "NO_CONNECTION", "Немає з'єднання",
        "Відсутнє з'єднання з Інтернетом. Перевірте Wi-Fi або мобільні дані.",
        isRetryable = true, presentation = ErrorPresentation.SNACKBAR
    )

    class Timeout : AppError(
        "TIMEOUT", "Тайм-аут",
        "Сервер SCADA не відповів вчасно. Спробуйте ще раз.",
        isRetryable = true, presentation = ErrorPresentation.SNACKBAR
    )

    class BadRequest(detail: String?) : AppError(
        "HTTP_400", "Некоректний запит (400)",
        "Сервер відхилив параметри запиту${detail?.let { ": $it" } ?: ""}.",
        400, false, ErrorPresentation.DIALOG
    )

    class Unauthorized : AppError(
        "HTTP_401", "Потрібна автентифікація (401)",
        "Токен доступу відсутній або протермінований. Увійдіть у систему повторно.",
        401, false, ErrorPresentation.DIALOG
    )

    class Forbidden : AppError(
        "HTTP_403", "Доступ заборонено (403)",
        "У вашого облікового запису немає прав на телеметрію цієї станції.",
        403, false, ErrorPresentation.DIALOG
    )

    class NotFound(resource: String) : AppError(
        "HTTP_404", "Ресурс не знайдено (404)",
        "Станцію «$resource» не знайдено на сервері. Перевірте код гідропоста.",
        404, false, ErrorPresentation.DIALOG
    )

    class RateLimited(val retryAfterSec: Long?) : AppError(
        "HTTP_429", "Забагато запитів (429)",
        "Перевищено ліміт запитів до SCADA. Повтор через ${retryAfterSec ?: "кілька"} с.",
        429, true, ErrorPresentation.SNACKBAR
    )

    class ServerError(code: Int) : AppError(
        "HTTP_$code", "Помилка сервера ($code)",
        when (code) {
            502 -> "Шлюз SCADA отримав некоректну відповідь від сервера телеметрії."
            503 -> "Сервіс тимчасово недоступний (технічне обслуговування). Спробуйте пізніше."
            504 -> "Шлюз не дочекався відповіді сервера телеметрії."
            else -> "Сталася помилка на сервері, спробуйте пізніше."
        },
        code, true, ErrorPresentation.SNACKBAR
    )

    class Parsing(detail: String?) : AppError(
        "PARSING", "Некоректні дані",
        "Сервер повернув дані у неочікуваному форматі${detail?.let { " ($it)" } ?: ""}.",
        isRetryable = false, presentation = ErrorPresentation.DIALOG
    )

    class Validation(val field: String, detail: String) : AppError(
        "VALIDATION", "Помилка введення", detail,
        isRetryable = false, presentation = ErrorPresentation.INLINE
    )

    class Unknown(detail: String?) : AppError(
        "UNKNOWN", "Невідома помилка",
        "Неочікувана помилка: ${detail ?: "без опису"}.",
        isRetryable = false, presentation = ErrorPresentation.DIALOG
    )
}

data class ErrorContext(
    val endpoint: String,
    val attempt: Int,
    val maxAttempts: Int,
    val willRetry: Boolean
)

data class ErrorLogEntry(
    val timestamp: Long,
    val type: String,
    val httpCode: Int?,
    val endpoint: String,
    val attempt: Int,
    val maxAttempts: Int,
    val willRetry: Boolean,
    val technicalMessage: String
)

enum class FaultScenario(val title: String) {
    SUCCESS("200 OK — успішна відповідь"),
    FLAKY_503("Тимчасовий збій: 503, 503 → 200"),
    FLAKY_TIMEOUT("Тимчасовий тайм-аут → 200"),
    NO_INTERNET("Немає мережі (UnknownHost)"),
    TIMEOUT("Постійний тайм-аут"),
    HTTP_400("400 Bad Request"),
    HTTP_401("401 Unauthorized"),
    HTTP_403("403 Forbidden"),
    HTTP_404("404 Not Found"),
    HTTP_429("429 Too Many Requests + Retry-After"),
    HTTP_500("500 Internal Server Error"),
    HTTP_502("502 Bad Gateway"),
    HTTP_503("503 Service Unavailable"),
    MALFORMED_JSON("200, але зламаний JSON"),
    RANDOM("Випадковий збій")
}

data class TelemetryStationDto(val code: String, val name: String, val river: String)
data class TelemetryReservoirDto(val levelM: Double, val normalLevelM: Double, val volumeMcm: Double)
data class TelemetryFlowsDto(val inflowM3s: Double, val turbineM3s: Double, val spillwayM3s: Double)
data class LevelPointDto(val hoursAgo: Int, val levelM: Double)

data class StationTelemetryResponse(
    val station: TelemetryStationDto?,
    val reservoir: TelemetryReservoirDto?,
    val flows: TelemetryFlowsDto?,
    val units: List<UnitStatusDto>?,
    val history: List<LevelPointDto>?,
    val generatedAt: Long?
)
