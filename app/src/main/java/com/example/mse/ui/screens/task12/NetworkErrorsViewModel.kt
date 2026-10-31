package com.example.mse.ui.screens.task12

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mse.data.error.AndroidNetworkMonitor
import com.example.mse.data.error.CompositeErrorLogger
import com.example.mse.data.error.ErrorHandler
import com.example.mse.data.error.ErrorLogFormat
import com.example.mse.data.error.FileErrorLogger
import com.example.mse.data.error.LogcatErrorLogger
import com.example.mse.data.error.NetworkMonitor
import com.example.mse.data.remote.TelemetryNetwork
import com.example.mse.data.remote.mock.FaultInjectionScadaServer
import com.example.mse.data.repository.TelemetryRepository
import com.example.mse.data.repository.TelemetryResult
import com.example.mse.model.AppError
import com.example.mse.model.ErrorContext
import com.example.mse.model.ErrorPresentation
import com.example.mse.model.FaultScenario
import com.example.mse.model.StationTelemetryResponse
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface RequestState {
    data object Idle : RequestState
    data class Loading(val attempt: Int, val maxAttempts: Int, val retryInfo: String?) : RequestState
    data class Success(val data: StationTelemetryResponse, val attempts: Int) : RequestState
    data class Failed(val error: AppError, val attempts: Int) : RequestState
}

data class SnackbarEvent(val message: String, val retryable: Boolean)

data class NetworkErrorsUiState(
    val stationCode: String = "KYV-01",
    val hours: String = "12",
    val stationCodeError: String? = null,
    val hoursError: String? = null,
    val scenario: FaultScenario = FaultScenario.SUCCESS,
    val isDeviceOnline: Boolean = true,
    val simulateOffline: Boolean = false,
    val request: RequestState = RequestState.Idle,
    val dialogError: AppError? = null,
    val logLines: List<String> = emptyList(),
    val logPath: String = ""
) {
    val errorStats: List<Pair<String, Int>>
        get() = logLines.mapNotNull { ErrorLogFormat.typeOf(it) }
            .groupingBy { it }.eachCount().toList().sortedByDescending { it.second }
}

class NetworkErrorsViewModel(
    application: Application,
    private val repository: TelemetryRepository,
    private val server: FaultInjectionScadaServer,
    private val errorHandler: ErrorHandler,
    private val fileLogger: FileErrorLogger,
    networkMonitor: NetworkMonitor
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(NetworkErrorsUiState(logPath = fileLogger.path))
    val uiState: StateFlow<NetworkErrorsUiState> = _uiState.asStateFlow()

    private val snackbarEvents = Channel<SnackbarEvent>(Channel.BUFFERED)
    val snackbarFlow = snackbarEvents.receiveAsFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        val error = errorHandler.handle(throwable, ErrorContext("viewModelScope", 0, 0, false))
        _uiState.update { it.copy(request = RequestState.Failed(error, 0), dialogError = error) }
        refreshLog()
    }

    private var requestJob: Job? = null

    init {
        viewModelScope.launch(exceptionHandler) {
            networkMonitor.onlineFlow.collect { online -> _uiState.update { it.copy(isDeviceOnline = online) } }
        }
        refreshLog()
    }

    fun onStationCodeChange(v: String) = _uiState.update { it.copy(stationCode = v.uppercase(), stationCodeError = null) }
    fun onHoursChange(v: String) = _uiState.update { it.copy(hours = v.filter(Char::isDigit), hoursError = null) }
    fun onScenarioChange(s: FaultScenario) {
        server.scenario = s
        _uiState.update { it.copy(scenario = s) }
    }

    fun onSimulateOfflineChange(value: Boolean) {
        repository.forceOffline = value
        _uiState.update { it.copy(simulateOffline = value) }
    }

    fun dismissDialog() = _uiState.update { it.copy(dialogError = null) }

    fun fetch() {
        val state = _uiState.value
        val codeError = when {
            state.stationCode.isBlank() -> "Вкажіть код гідропоста"
            !Regex("^[A-Z]{3}-\\d{2}$").matches(state.stationCode) -> "Формат коду: KYV-01"
            else -> null
        }
        val hoursValue = state.hours.toIntOrNull()
        val hoursError = if (hoursValue == null || hoursValue !in 1..72) "Період 1–72 год" else null
        if (codeError != null || hoursError != null) {
            _uiState.update { it.copy(stationCodeError = codeError, hoursError = hoursError) }
            return
        }

        requestJob?.cancel()
        requestJob = viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(request = RequestState.Loading(1, 3, null)) }
            val result = repository.fetchTelemetry(state.stationCode, hoursValue!!) { event, error ->
                _uiState.update {
                    it.copy(
                        request = RequestState.Loading(
                            attempt = event.nextAttempt,
                            maxAttempts = event.maxAttempts,
                            retryInfo = "${error.title}: повтор ${event.nextAttempt}/${event.maxAttempts} через ${event.delayMs} мс"
                        )
                    )
                }
                refreshLog()
            }
            when (result) {
                is TelemetryResult.Success -> {
                    _uiState.update { it.copy(request = RequestState.Success(result.data, result.attempts)) }
                    if (result.attempts > 1) {
                        snackbarEvents.send(SnackbarEvent("Дані отримано з ${result.attempts}-ї спроби — тимчасовий збій оброблено автоматично", false))
                    }
                }
                is TelemetryResult.Failure -> showError(result.error, result.attempts)
            }
            refreshLog()
        }
    }

    private suspend fun showError(error: AppError, attempts: Int) {
        _uiState.update { it.copy(request = RequestState.Failed(error, attempts)) }
        when (error.presentation) {
            ErrorPresentation.SNACKBAR -> snackbarEvents.send(SnackbarEvent(error.message, error.isRetryable))
            ErrorPresentation.DIALOG -> _uiState.update { it.copy(dialogError = error) }
            ErrorPresentation.INLINE -> _uiState.update { it.copy(stationCodeError = error.message) }
        }
    }

    fun refreshLog() {
        viewModelScope.launch {
            val lines = withContext(Dispatchers.IO) { fileLogger.readLines() }
            _uiState.update { it.copy(logLines = lines.reversed()) }
        }
    }

    fun clearLog() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { fileLogger.clear() }
            refreshLog()
        }
    }

    companion object {
        fun create(application: Application): NetworkErrorsViewModel {
            val fileLogger = FileErrorLogger(application)
            val errorHandler = ErrorHandler(CompositeErrorLogger(LogcatErrorLogger(), fileLogger))
            val server = FaultInjectionScadaServer()
            val monitor = AndroidNetworkMonitor(application)
            val repository = TelemetryRepository(TelemetryNetwork.createApi(server), errorHandler, monitor)
            return NetworkErrorsViewModel(application, repository, server, errorHandler, fileLogger, monitor)
        }
    }
}
