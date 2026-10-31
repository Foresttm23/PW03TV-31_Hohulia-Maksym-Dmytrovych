package com.example.mse.ui.screens.task9

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mse.data.db.HydroDatabase
import com.example.mse.data.repository.GaugeReadingRepository
import com.example.mse.data.repository.RefreshReport
import com.example.mse.data.repository.WriteOutcome
import com.example.mse.data.source.RoomGaugeLocalDataSource
import com.example.mse.data.source.SimulatedScadaRemoteDataSource
import com.example.mse.model.DataOrigin
import com.example.mse.model.DataResult
import com.example.mse.model.GaugeReading
import com.example.mse.model.LevelStatus
import com.example.mse.model.ReadingFilter
import com.example.mse.model.ReadingSort
import com.example.mse.model.Reservoir
import com.example.mse.model.ReservoirDashboard
import com.example.mse.model.SourceMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JournalControlState(
    val mode: SourceMode = SourceMode.SMART,
    val isRefreshing: Boolean = false,
    val lastOrigin: DataOrigin? = null,
    val lastReport: RefreshReport? = null,
    val lastRefreshAt: Long? = null,
    val networkUnstable: Boolean = SimulatedScadaRemoteDataSource.failureRate > 0,
    val errorBanner: String? = null
)

data class ReservoirJournalUiState(
    val dashboard: ReservoirDashboard = ReservoirDashboard(),
    val filter: ReadingFilter = ReadingFilter(),
    val control: JournalControlState = JournalControlState(),
    val networkRequests: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ReservoirJournalViewModel(
    application: Application,
    private val repository: GaugeReadingRepository
) : AndroidViewModel(application) {
    constructor(application: Application) : this(
        application,
        GaugeReadingRepository(
            local = RoomGaugeLocalDataSource(HydroDatabase.getInstance(application).gaugeReadingDao()),
            remote = SimulatedScadaRemoteDataSource()
        )
    )

    private val filter = MutableStateFlow(ReadingFilter())
    private val control = MutableStateFlow(JournalControlState())

    private val messages = Channel<String>(Channel.BUFFERED)
    val messageFlow = messages.receiveAsFlow()

    private val dashboard = filter
        .debounce(150)
        .flatMapLatest { repository.observeDashboard(it) }

    val uiState: StateFlow<ReservoirJournalUiState> =
        combine(dashboard, filter, control) { data, currentFilter, controlState ->
            ReservoirJournalUiState(
                dashboard = data,
                filter = currentFilter,
                control = controlState,
                networkRequests = SimulatedScadaRemoteDataSource.requestCounter
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReservoirJournalUiState())

    init {
        refresh()
    }

    fun onQueryChange(query: String) = filter.update { it.copy(query = query) }
    fun onReservoirFilter(reservoir: Reservoir?) = filter.update { it.copy(reservoir = reservoir) }
    fun onStatusFilter(status: LevelStatus?) = filter.update { it.copy(status = status) }
    fun onSortChange(sort: ReadingSort) = filter.update { it.copy(sort = sort) }

    fun onModeChange(mode: SourceMode) {
        control.update { it.copy(mode = mode) }
        refresh()
    }

    fun onNetworkUnstableChange(unstable: Boolean) {
        SimulatedScadaRemoteDataSource.failureRate = if (unstable) 0.7 else 0.0
        control.update { it.copy(networkUnstable = unstable) }
    }

    fun refresh() {
        if (control.value.isRefreshing) return
        viewModelScope.launch {
            control.update { it.copy(isRefreshing = true, errorBanner = null) }
            when (val result = repository.refresh(control.value.mode)) {
                is DataResult.Success -> {
                    control.update {
                        it.copy(
                            isRefreshing = false,
                            lastOrigin = result.source,
                            lastReport = result.data,
                            lastRefreshAt = System.currentTimeMillis()
                        )
                    }
                    val r = result.data
                    if (result.source != DataOrigin.LOCAL_DB) {
                        messages.send("${result.source.title}: +${r.inserted} нових, ${r.updated} оновлено, ${r.removed} видалено")
                    }
                }
                is DataResult.Error -> {
                    control.update {
                        it.copy(isRefreshing = false, lastOrigin = DataOrigin.LOCAL_DB, errorBanner = result.message)
                    }
                    messages.send("Помилка мережі — працюємо з локальною БД")
                }
            }
        }
    }

    fun syncPending() {
        viewModelScope.launch {
            when (val result = repository.syncPending()) {
                is DataResult.Success -> messages.send("Відправлено на сервер: ${result.data} запис(ів)")
                is DataResult.Error -> messages.send(result.message)
            }
        }
    }

    fun addReading(reservoir: Reservoir, code: String, level: Double, inflow: Double, outflow: Double) {
        viewModelScope.launch {
            reportWrite(repository.add(reservoir, code, level, inflow, outflow), "Запис додано")
        }
    }

    fun updateReading(reading: GaugeReading) {
        viewModelScope.launch { reportWrite(repository.update(reading), "Запис оновлено") }
    }

    fun deleteReading(reading: GaugeReading) {
        viewModelScope.launch { reportWrite(repository.delete(reading), "Запис ${reading.gaugeCode} видалено") }
    }

    private suspend fun reportWrite(result: DataResult<WriteOutcome>, successText: String) {
        val text = when (result) {
            is DataResult.Success -> when (result.data) {
                WriteOutcome.SYNCED -> "$successText і синхронізовано із сервером"
                WriteOutcome.SAVED_LOCALLY -> "$successText локально; сервер недоступний — синхронізація пізніше"
            }
            is DataResult.Error -> result.message
        }
        messages.send(text)
    }
}
