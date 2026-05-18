package com.BPO.plantcare.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.repository.AuthRepository
import com.BPO.plantcare.domain.repository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * VM que observa el AuthState desde MainActivity para decidir si pinta el
 * AuthScreen o la app real. Vive a nivel de Activity y se comparte.
 */
@HiltViewModel
class AuthGateViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthState.Loading,
        )
}
