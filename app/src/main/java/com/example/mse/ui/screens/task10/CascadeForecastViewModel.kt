package com.example.mse.ui.screens.task10

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mse.data.db.HydroDatabase
import com.example.mse.data.repository.AssetStationCatalogSource
import com.example.mse.data.repository.ForecastProgress
import com.example.mse.data.repository.HydroForecastRepository
import com.example.mse.data.remote.OpenMeteoClient
import com.example.mse.logic.HydroForecastCalculator
import com.example.mse.model.CascadeForecast
import com.example.mse.model.DataResult
import com.example.mse.model.FloodRisk
import com.example.mse.model.StationForecast
import com.example.mse.model.StationSort
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CascadeForecastUiState(
    val isLoading: Boolean = false,
    val progressText: String? = null,
    val forecast: CascadeForecast? = null,
    val errorMessage: String? = null,
    val riverFilter: String? = null,
    val riskFilter: FloodRisk? = null,
    val sort: StationSort = StationSort.RISK,
    val expandedIds: Set<String> = emptySet()
) {
    val visibleStations: List<StationForecast>
        get() {
            val all = forecast?.stations.orEmpty()
            val filtered = all.filter {
                (riverFilter == null || it.station.river == riverFilter) &&
                    (riskFilter == null || it.risk == riskFilter)
            }
            return HydroForecastCalculator.sort(filtered, sort)
        }

    val rivers: List<String> get() = forecast?.stations.orEmpty().map { it.station.river }.distinct()
}

class CascadeForecastViewModel(
    application: Application,
    private val repository: HydroForecastRepository
) : AndroidViewModel(application) {
    constructor(application: Application) : this(
        application,
        HydroForecastRepository(
            floodApi = OpenMeteoClient.floodApi,
            weatherApi = OpenMeteoClient.weatherApi,
            cacheDao = HydroDatabase.getInstance(application).cachedForecastDao(),
            catalogSource = AssetStationCatalogSource(application, OpenMeteoClient.gson),
            gson = OpenMeteoClient.gson
        )
    )

    private val _uiState = MutableStateFlow(CascadeForecastUiState())
    val uiState: StateFlow<CascadeForecastUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load(forceRefresh = false)
    }

    fun load(forceRefresh: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, progressText = "Підготовка запитів…") }
            val result = repository.loadForecast(forceRefresh) { progress ->
                val text = when (progress) {
                    is ForecastProgress.Loading -> "Отримано станцій: ${progress.done}/${progress.total}"
                    is ForecastProgress.Retrying ->
                        "${progress.stationName}: повтор ${progress.event.nextAttempt}/${progress.event.maxAttempts} через ${progress.event.delayMs} мс"
                }
                _uiState.update { it.copy(progressText = text) }
            }
            _uiState.update {
                when (result) {
                    is DataResult.Success -> it.copy(isLoading = false, forecast = result.data, progressText = null)
                    is DataResult.Error -> it.copy(isLoading = false, errorMessage = result.message, progressText = null)
                }
            }
        }
    }

    fun onRiverFilter(river: String?) = _uiState.update { it.copy(riverFilter = river) }
    fun onRiskFilter(risk: FloodRisk?) = _uiState.update { it.copy(riskFilter = risk) }
    fun onSort(sort: StationSort) = _uiState.update { it.copy(sort = sort) }
    fun toggleExpanded(id: String) = _uiState.update {
        it.copy(expandedIds = if (id in it.expandedIds) it.expandedIds - id else it.expandedIds + id)
    }
}
