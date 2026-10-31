package com.example.mse.data.error

import android.content.Context
import android.util.Log
import com.example.mse.model.ErrorLogEntry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface ErrorLogger {
    fun log(entry: ErrorLogEntry)
}

class LogcatErrorLogger(private val tag: String = "HydroNetworkError") : ErrorLogger {
    override fun log(entry: ErrorLogEntry) {
        val text = ErrorLogFormat.format(entry)
        if (entry.willRetry) Log.w(tag, text) else Log.e(tag, text)
    }
}

class FileErrorLogger(private val file: File, private val maxLines: Int = 500) : ErrorLogger {
    constructor(context: Context) : this(File(context.filesDir, "network_errors.log"))

    val path: String get() = file.absolutePath

    @Synchronized
    override fun log(entry: ErrorLogEntry) {
        file.appendText(ErrorLogFormat.format(entry) + "\n")
        val lines = file.readLines()
        if (lines.size > maxLines) file.writeText(lines.takeLast(maxLines).joinToString("\n", postfix = "\n"))
    }

    @Synchronized
    fun readLines(): List<String> = if (file.exists()) file.readLines().filter { it.isNotBlank() } else emptyList()

    @Synchronized
    fun clear() {
        if (file.exists()) file.writeText("")
    }
}

class CompositeErrorLogger(private vararg val loggers: ErrorLogger) : ErrorLogger {
    override fun log(entry: ErrorLogEntry) {
        loggers.forEach { runCatching { it.log(entry) } }
    }
}

object ErrorLogFormat {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun format(e: ErrorLogEntry): String = listOf(
        synchronized(dateFormat) { dateFormat.format(Date(e.timestamp)) },
        e.type,
        e.httpCode?.toString() ?: "-",
        e.endpoint,
        "спроба ${e.attempt}/${e.maxAttempts}",
        if (e.willRetry) "RETRY" else "FINAL",
        e.technicalMessage.replace('\n', ' ')
    ).joinToString(" | ")

    fun typeOf(line: String): String? = line.split(" | ").getOrNull(1)
}
