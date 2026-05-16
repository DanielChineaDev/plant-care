package com.BPO.plantcare.ui.screens.identify

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.PlantSuggestion
import com.BPO.plantcare.domain.usecase.AddPlantFromSuggestionUseCase
import com.BPO.plantcare.domain.usecase.IdentifyPlantUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface IdentifyUiState {
    data object Idle : IdentifyUiState
    data class Captured(val photoUri: Uri, val photoFile: File) : IdentifyUiState
    data class Loading(val photoUri: Uri) : IdentifyUiState
    data class Success(
        val photoUri: Uri,
        val photoFile: File,
        val suggestions: List<PlantSuggestion>,
    ) : IdentifyUiState
    data class Error(val photoUri: Uri?, val message: String) : IdentifyUiState
}

sealed interface IdentifyEvent {
    data class PlantAdded(val displayName: String) : IdentifyEvent
    data class AddFailed(val message: String) : IdentifyEvent
}

@HiltViewModel
class IdentifyViewModel @Inject constructor(
    private val identifyPlant: IdentifyPlantUseCase,
    private val addPlant: AddPlantFromSuggestionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<IdentifyUiState>(IdentifyUiState.Idle)
    val state: StateFlow<IdentifyUiState> = _state.asStateFlow()

    private val _events = Channel<IdentifyEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onPhotoCaptured(uri: Uri, file: File) {
        _state.update { IdentifyUiState.Captured(uri, file) }
    }

    fun identify() {
        val current = _state.value as? IdentifyUiState.Captured ?: return
        val uri = current.photoUri
        val file = current.photoFile
        _state.update { IdentifyUiState.Loading(uri) }
        viewModelScope.launch {
            identifyPlant(file).fold(
                onSuccess = { suggestions ->
                    if (suggestions.isEmpty()) {
                        _state.update {
                            IdentifyUiState.Error(uri, "No hemos podido identificar la planta. Prueba con otra foto.")
                        }
                    } else {
                        _state.update { IdentifyUiState.Success(uri, file, suggestions) }
                    }
                },
                onFailure = { e ->
                    _state.update {
                        IdentifyUiState.Error(uri, e.localizedMessage ?: "Error al identificar la planta.")
                    }
                },
            )
        }
    }

    fun addSuggestionToMyPlants(suggestion: PlantSuggestion) {
        val current = _state.value as? IdentifyUiState.Success ?: return
        viewModelScope.launch {
            addPlant(suggestion = suggestion, capturedPhoto = current.photoFile).fold(
                onSuccess = {
                    val name = suggestion.commonNames.firstOrNull() ?: suggestion.scientificName
                    _events.send(IdentifyEvent.PlantAdded(name))
                },
                onFailure = { e ->
                    _events.send(IdentifyEvent.AddFailed(e.localizedMessage ?: "Error al guardar la planta."))
                },
            )
        }
    }

    fun retake() {
        _state.update { IdentifyUiState.Idle }
    }
}
