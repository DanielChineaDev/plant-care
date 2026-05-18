package com.BPO.plantcare.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.Community
import com.BPO.plantcare.domain.model.UserProfile
import com.BPO.plantcare.domain.repository.AuthRepository
import com.BPO.plantcare.domain.repository.AuthState
import com.BPO.plantcare.domain.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DrawerState(
    val profile: UserProfile? = null,
    val joinedCommunities: List<Community> = emptyList(),
)

@HiltViewModel
class DrawerViewModel @Inject constructor(
    authRepository: AuthRepository,
    communityRepository: CommunityRepository,
) : ViewModel() {

    val state: StateFlow<DrawerState> = combine(
        authRepository.authState,
        communityRepository.observeCommunities(),
    ) { auth, communities ->
        val profile = (auth as? AuthState.SignedIn)?.profile
        DrawerState(
            profile = profile,
            joinedCommunities = communities.filter { it.isMember },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DrawerState(),
    )
}
