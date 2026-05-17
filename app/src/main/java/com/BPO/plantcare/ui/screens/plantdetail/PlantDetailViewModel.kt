package com.BPO.plantcare.ui.screens.plantdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.model.PlantCareGuide
import com.BPO.plantcare.domain.model.WateringLog
import com.BPO.plantcare.domain.model.WikipediaSummary
import com.BPO.plantcare.domain.usecase.DeletePlantUseCase
import com.BPO.plantcare.domain.usecase.DeleteWateringLogUseCase
import com.BPO.plantcare.domain.usecase.GetPlantCareGuideUseCase
import com.BPO.plantcare.domain.usecase.GetWikipediaSummaryUseCase
import com.BPO.plantcare.domain.usecase.MarkPlantWateredUseCase
import com.BPO.plantcare.domain.usecase.ObservePlantUseCase
import com.BPO.plantcare.domain.usecase.ObserveWateringHistoryUseCase
import com.BPO.plantcare.domain.usecase.UpdatePlantUseCase
import com.BPO.plantcare.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WikipediaUiState {
    data object Loading : WikipediaUiState
    data class Loaded(val summary: WikipediaSummary) : WikipediaUiState
    data object NotFound : WikipediaUiState
    data class Error(val message: String) : WikipediaUiState
}

sealed interface PlantDetailEvent {
    data object Deleted : PlantDetailEvent
}

@HiltViewModel
class PlantDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observePlant: ObservePlantUseCase,
    observeWateringHistory: ObserveWateringHistoryUseCase,
    private val updatePlant: UpdatePlantUseCase,
    private val deletePlant: DeletePlantUseCase,
    private val markWatered: MarkPlantWateredUseCase,
    private val deleteWateringLog: DeleteWateringLogUseCase,
    private val getWikipediaSummary: GetWikipediaSummaryUseCase,
    private val getCareGuide: GetPlantCareGuideUseCase,
) : ViewModel() {

    private val plantId: Long = checkNotNull(savedStateHandle.get<Long>(NavArgs.PLANT_ID))

    val plant: StateFlow<Plant?> = observePlant(plantId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val history: StateFlow<List<WateringLog>> = observeWateringHistory(plantId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val careGuide: StateFlow<PlantCareGuide?> = plant
        .map { p -> p?.scientificName?.let(getCareGuide::invoke) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private val _wikipedia = MutableStateFlow<WikipediaUiState>(WikipediaUiState.Loading)
    val wikipedia: StateFlow<WikipediaUiState> = _wikipedia.asStateFlow()

    private val _events = Channel<PlantDetailEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var lastFetchedScientific: String? = null

    init {
        viewModelScope.launch {
            plant.collect { p ->
                val name = p?.scientificName ?: return@collect
                if (name != lastFetchedScientific) {
                    lastFetchedScientific = name
                    fetchWikipedia(name)
                }
            }
        }
    }

    private fun fetchWikipedia(scientificName: String) {
        viewModelScope.launch {
            _wikipedia.update { WikipediaUiState.Loading }
            getWikipediaSummary(scientificName).fold(
                onSuccess = { summary ->
                    _wikipedia.update {
                        if (summary == null) WikipediaUiState.NotFound
                        else WikipediaUiState.Loaded(summary)
                    }
                },
                onFailure = { e ->
                    _wikipedia.update { WikipediaUiState.Error(e.localizedMessage ?: "Error") }
                },
            )
        }
    }

    fun onNicknameChange(nickname: String) {
        val current = plant.value ?: return
        viewModelScope.launch {
            updatePlant(current.copy(nickname = nickname.ifBlank { null }))
        }
    }

    fun onIntervalChange(newInterval: Int) {
        val current = plant.value ?: return
        val safe = newInterval.coerceIn(1, 60)
        viewModelScope.launch {
            updatePlant(current.copy(wateringIntervalDays = safe))
        }
    }

    fun onMarkWatered() {
        viewModelScope.launch { markWatered(plantId) }
    }

    fun onDeleteWateringLog(logId: Long) {
        viewModelScope.launch { deleteWateringLog(logId) }
    }

    fun onDelete() {
        val current = plant.value ?: return
        viewModelScope.launch {
            deletePlant(current)
            _events.send(PlantDetailEvent.Deleted)
        }
    }
}
