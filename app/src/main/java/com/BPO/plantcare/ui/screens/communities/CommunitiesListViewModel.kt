package com.BPO.plantcare.ui.screens.communities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.Community
import com.BPO.plantcare.domain.repository.AuthRepository
import com.BPO.plantcare.domain.repository.AuthState
import com.BPO.plantcare.domain.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CommunitiesEvent {
    data class Error(val message: String) : CommunitiesEvent
    data class CommunityCreated(val id: String) : CommunitiesEvent
}

@HiltViewModel
class CommunitiesListViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    val communities: StateFlow<List<Community>> =
        communityRepository.observeCommunities().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val isSignedIn: StateFlow<Boolean> = authRepository.authState
        .map { it is AuthState.SignedIn }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    private val _events = Channel<CommunitiesEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun toggleMembership(community: Community) {
        viewModelScope.launch {
            val result = if (community.isMember) {
                communityRepository.leaveCommunity(community.id)
            } else {
                communityRepository.joinCommunity(community.id)
            }
            result.onFailure { _events.send(CommunitiesEvent.Error(it.localizedMessage.orEmpty())) }
        }
    }

    fun createCommunity(name: String, description: String, emoji: String) {
        viewModelScope.launch {
            communityRepository.createCommunity(name, description, emoji).fold(
                onSuccess = { _events.send(CommunitiesEvent.CommunityCreated(it)) },
                onFailure = { _events.send(CommunitiesEvent.Error(it.localizedMessage.orEmpty())) },
            )
        }
    }
}
