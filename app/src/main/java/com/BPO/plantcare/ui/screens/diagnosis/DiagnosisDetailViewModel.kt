package com.BPO.plantcare.ui.screens.diagnosis

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.BPO.plantcare.domain.model.PlantDiagnosis
import com.BPO.plantcare.domain.repository.DiagnosisRepository
import com.BPO.plantcare.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DiagnosisDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: DiagnosisRepository,
) : ViewModel() {

    val diagnosis: PlantDiagnosis? = savedStateHandle.get<String>(NavArgs.DIAGNOSIS_ID)
        ?.let { repository.findById(it) }
}
