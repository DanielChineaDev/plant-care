package com.BPO.plantcare.ui.screens.identify

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.PlantSuggestion
import com.BPO.plantcare.domain.usecase.IdentifyPlantUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        val suggestions: List<PlantSuggestion>,
    ) : IdentifyUiState
    data class Error(val photoUri: Uri?, val message: String) : IdentifyUiState
}

@HiltViewModel
class IdentifyViewModel @Inject constructor(
    private val identifyPlant: IdentifyPlantUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<IdentifyUiState>(IdentifyUiState.Idle)
    val state: StateFlow<IdentifyUiState> = _state.asStateFlow()

    fun onPhotoCaptured(uri: Uri, file: File) {
        _state.update { IdentifyUiState.Captured(uri, file) }
    }

    fun identify() {
        val current = _state.value
        if (current !is IdentifyUiState.Captured) return
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
                        _state.update { IdentifyUiState.Success(uri, suggestions) }
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

    fun retake() {
        _state.update { IdentifyUiState.Idle }
    }
}
