package com.BPO.plantcare.ui.screens.myprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.PublicPlant
import com.BPO.plantcare.domain.model.UserProfile
import com.BPO.plantcare.domain.repository.AuthRepository
import com.BPO.plantcare.domain.repository.AuthState
import com.BPO.plantcare.domain.repository.PublicProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * VM de la pantalla "Mi perfil" del drawer. Reutiliza PublicProfileRepository
 * pero pasandole el uid del usuario logueado para que el dueno vea su propia
 * vista publica.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyProfileViewModel @Inject constructor(
    authRepository: AuthRepository,
    publicProfileRepository: PublicProfileRepository,
) : ViewModel() {

    private val signedInProfile: StateFlow<UserProfile?> = authRepository.authState
        .map { (it as? AuthState.SignedIn)?.profile }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val profile: StateFlow<UserProfile?> = signedInProfile
        .flatMapLatest { local ->
            val uid = local?.uid ?: return@flatMapLatest flowOf<UserProfile?>(null)
            publicProfileRepository.observeUserProfile(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val publicPlants: StateFlow<List<PublicPlant>> = signedInProfile
        .flatMapLatest { local ->
            val uid = local?.uid ?: return@flatMapLatest flowOf(emptyList())
            publicProfileRepository.observePublicPlants(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Loading explicito para mostrar spinner cuando aun no tenemos uid.
    val loading: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
}
