package com.BPO.plantcare.ui.screens.plantdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.GuideMatch
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.model.PlantPhoto
import com.BPO.plantcare.domain.model.PlantTask
import com.BPO.plantcare.domain.model.PlantTaskType
import com.BPO.plantcare.domain.model.WateringLog
import com.BPO.plantcare.domain.usecase.AddPlantPhotoUseCase
import com.BPO.plantcare.domain.usecase.AddWateringLogUseCase
import com.BPO.plantcare.domain.usecase.DisableTaskUseCase
import com.BPO.plantcare.domain.usecase.EnableTaskUseCase
import com.BPO.plantcare.domain.usecase.MarkTaskDoneUseCase
import com.BPO.plantcare.domain.usecase.ObservePlantTasksUseCase
import com.BPO.plantcare.domain.usecase.UpdatePlantPhotoUseCase
import com.BPO.plantcare.domain.usecase.UpdateTaskIntervalUseCase
import com.BPO.plantcare.domain.usecase.DeletePlantPhotoUseCase
import com.BPO.plantcare.domain.usecase.DeletePlantUseCase
import com.BPO.plantcare.domain.usecase.DeleteWateringLogUseCase
import com.BPO.plantcare.domain.usecase.GetPlantCareGuideUseCase
import com.BPO.plantcare.domain.usecase.GetWikipediaSummaryUseCase
import com.BPO.plantcare.domain.usecase.MarkPlantWateredUseCase
import com.BPO.plantcare.domain.usecase.ObservePlantPhotosUseCase
import com.BPO.plantcare.domain.usecase.ObservePlantUseCase
import com.BPO.plantcare.domain.usecase.ObserveWateringHistoryUseCase
import com.BPO.plantcare.domain.usecase.UpdatePlantUseCase
import com.BPO.plantcare.ui.navigation.NavArgs
import com.BPO.plantcare.ui.screens.common.WikipediaUiState
import java.io.File
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

sealed interface PlantDetailEvent {
    data object Deleted : PlantDetailEvent
    data class WateringLogDeleted(val log: WateringLog) : PlantDetailEvent
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
    private val addWateringLog: AddWateringLogUseCase,
    private val getWikipediaSummary: GetWikipediaSummaryUseCase,
    private val getCareGuide: GetPlantCareGuideUseCase,
    observePhotos: ObservePlantPhotosUseCase,
    observeTasksForPlant: ObservePlantTasksUseCase,
    private val addPhoto: AddPlantPhotoUseCase,
    private val deletePhoto: DeletePlantPhotoUseCase,
    private val updatePlantPhoto: UpdatePlantPhotoUseCase,
    private val enableTask: EnableTaskUseCase,
    private val disableTask: DisableTaskUseCase,
    private val markTaskDone: MarkTaskDoneUseCase,
    private val updateTaskInterval: UpdateTaskIntervalUseCase,
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

    val careGuide: StateFlow<GuideMatch?> = plant
        .map { p -> p?.scientificName?.let(getCareGuide::invoke) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val photos: StateFlow<List<PlantPhoto>> = observePhotos(plantId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    /** Tareas configurables (no-riego) para esta planta. */
    val tasks: StateFlow<List<PlantTask>> = observeTasksForPlant(plantId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
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

    fun onDeleteWateringLog(log: WateringLog) {
        viewModelScope.launch {
            deleteWateringLog(log.id)
            _events.send(PlantDetailEvent.WateringLogDeleted(log))
        }
    }

    fun undoDeleteWateringLog(log: WateringLog) {
        viewModelScope.launch { addWateringLog(log) }
    }

    fun onAddDiaryPhoto(file: File) {
        viewModelScope.launch { addPhoto(plantId, file) }
    }

    fun onChangeMainPhoto(file: File) {
        val current = plant.value ?: return
        viewModelScope.launch { updatePlantPhoto(current, file) }
    }

    fun onDeleteDiaryPhoto(photoId: Long) {
        viewModelScope.launch { deletePhoto(photoId) }
    }

    fun onDelete() {
        val current = plant.value ?: return
        viewModelScope.launch {
            deletePlant(current)
            _events.send(PlantDetailEvent.Deleted)
        }
    }

    // ---- Tareas de cuidado ----
    fun onToggleTask(type: PlantTaskType, currentTaskId: Long?, enable: Boolean) {
        viewModelScope.launch {
            if (enable) {
                enableTask(plantId, type)
            } else if (currentTaskId != null) {
                disableTask(currentTaskId)
            }
        }
    }

    fun onMarkTaskDone(taskId: Long) {
        viewModelScope.launch { markTaskDone(taskId) }
    }

    fun onUpdateTaskInterval(taskId: Long, days: Int) {
        viewModelScope.launch { updateTaskInterval(taskId, days) }
    }
}
