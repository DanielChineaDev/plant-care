package com.BPO.plantcare.ui.screens.diagnosephoto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.DiagnosisAnalysis
import com.BPO.plantcare.domain.repository.DiagnosePhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DiagnosePhotoViewModel @Inject constructor(
    private val repository: DiagnosePhotoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DiagnoseState())
    val state: StateFlow<DiagnoseState> = _state.asStateFlow()

    fun analyze(file: File) {
        _state.update {
            it.copy(loading = true, photoPath = file.absolutePath, error = null, analysis = null)
        }
        viewModelScope.launch {
            repository.analyze(file)
                .onSuccess { result ->
                    _state.update { it.copy(loading = false, analysis = result) }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = err.localizedMessage ?: "No se pudo analizar la foto",
                        )
                    }
                }
        }
    }
}

data class DiagnoseState(
    val photoPath: String? = null,
    val loading: Boolean = false,
    val analysis: DiagnosisAnalysis? = null,
    val error: String? = null,
)
