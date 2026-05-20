package com.BPO.plantcare.ui.screens.publicprofile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.Achievement
import com.BPO.plantcare.domain.model.PublicPlant
import com.BPO.plantcare.domain.model.UserProfile
import com.BPO.plantcare.domain.model.achievementsFromDates
import com.BPO.plantcare.domain.repository.PublicProfileRepository
import com.BPO.plantcare.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: PublicProfileRepository,
) : ViewModel() {

    val uid: String = checkNotNull(savedStateHandle.get<String>(NavArgs.OTHER_UID))

    val profile: StateFlow<UserProfile?> =
        repository.observeUserProfile(uid).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val publicPlants: StateFlow<List<PublicPlant>> =
        repository.observePublicPlants(uid).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /**
     * Logros conseguidos por este usuario (solo los desbloqueados, deducidos
     * de los registros en Firestore). La pantalla decide si mostrarlos segun
     * el flag badgesPublic del perfil.
     */
    val achievements: StateFlow<List<Achievement>> =
        repository.observeAchievements(uid)
            .map { dates -> achievementsFromDates(dates).filter { it.unlocked } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )
}
