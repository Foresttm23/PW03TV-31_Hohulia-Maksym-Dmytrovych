package com.example.mse.model

sealed interface DataResult<out T> {
    data class Success<T>(val data: T, val source: DataOrigin = DataOrigin.LOCAL_DB) : DataResult<T>

    data class Error<T>(
        val message: String,
        val cause: Throwable? = null,
        val fallbackData: T? = null
    ) : DataResult<T>
}

enum class DataOrigin(val title: String) {
    MEMORY_CACHE("Кеш у пам'яті"),
    LOCAL_DB("Локальна БД Room"),
    NETWORK("Віддалений сервер"),
    OFFLINE_CACHE("Офлайн-кеш Room")
}
