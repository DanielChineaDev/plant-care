package com.BPO.plantcare.ui.screens.communityfeed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.Community
import com.BPO.plantcare.domain.model.CommunityPost
import com.BPO.plantcare.domain.repository.AuthRepository
import com.BPO.plantcare.domain.repository.AuthState
import com.BPO.plantcare.domain.repository.CommunityRepository
import com.BPO.plantcare.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FeedEvent {
    data class Error(val message: String) : FeedEvent
    data object PostCreated : FeedEvent
}

@HiltViewModel
class CommunityFeedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val communityRepository: CommunityRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    val communityId: String =
        checkNotNull(savedStateHandle.get<String>(NavArgs.COMMUNITY_ID))

    val community: StateFlow<Community?> =
        communityRepository.observeCommunity(communityId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    /** Posts del feed enriquecidos con isLikedByMe a partir del mirror del user. */
    val posts: StateFlow<List<CommunityPost>> = combine(
        communityRepository.observePosts(communityId),
        communityRepository.observeLikedPostsInCommunity(communityId),
    ) { posts, liked ->
        posts.map { it.copy(isLikedByMe = it.id in liked) }
    }.stateIn(
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

    private val _events = Channel<FeedEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun createPost(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            communityRepository.createPost(communityId, text.trim()).fold(
                onSuccess = { _events.send(FeedEvent.PostCreated) },
                onFailure = { _events.send(FeedEvent.Error(it.localizedMessage.orEmpty())) },
            )
        }
    }

    fun toggleMembership() {
        val current = community.value ?: return
        viewModelScope.launch {
            val result = if (current.isMember) {
                communityRepository.leaveCommunity(communityId)
            } else {
                communityRepository.joinCommunity(communityId)
            }
            result.onFailure { _events.send(FeedEvent.Error(it.localizedMessage.orEmpty())) }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            communityRepository.toggleLike(communityId, postId).onFailure {
                _events.send(FeedEvent.Error(it.localizedMessage.orEmpty()))
            }
        }
    }
}
