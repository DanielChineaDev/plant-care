package com.BPO.plantcare.ui.screens.publicplant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.GuideMatch
import com.BPO.plantcare.domain.model.PublicPlant
import com.BPO.plantcare.domain.model.UserProfile
import com.BPO.plantcare.domain.repository.PublicProfileRepository
import com.BPO.plantcare.domain.usecase.GetPlantCareGuideUseCase
import com.BPO.plantcare.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * VM de la vista PUBLICA de una planta: lo que ve un usuario distinto al
 * propietario. Solo expone lo que el propietario ha decidido compartir
 * (notas, diario, info de cuidados) ademas de los datos basicos.
 */
@HiltViewModel
class PublicPlantDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: PublicProfileRepository,
    private val getCareGuide: GetPlantCareGuideUseCase,
) : ViewModel() {

    private val ownerUid: String = checkNotNull(savedStateHandle[NavArgs.OTHER_UID])
    private val plantId: String = checkNotNull(savedStateHandle[NavArgs.PUBLIC_PLANT_ID])

    val plant: StateFlow<PublicPlant?> =
        repository.observePublicPlant(ownerUid, plantId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val owner: StateFlow<UserProfile?> =
        repository.observeUserProfile(ownerUid).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    /** Guia de cuidados deducida de la especie (catalogo local, offline). */
    val careGuide: StateFlow<GuideMatch?> = plant
        .map { p -> p?.scientificName?.takeIf { it.isNotBlank() }?.let(getCareGuide::invoke) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
}
