package com.BPO.plantcare.ui.screens.postdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.Comment
import com.BPO.plantcare.domain.model.CommunityPost
import com.BPO.plantcare.domain.model.ReportReason
import com.BPO.plantcare.domain.model.ReportedContentType
import com.BPO.plantcare.domain.repository.AuthRepository
import com.BPO.plantcare.domain.repository.AuthState
import com.BPO.plantcare.domain.repository.CommunityRepository
import com.BPO.plantcare.domain.repository.ReportRepository
import com.BPO.plantcare.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PostDetailEvent {
    data class Error(val message: String) : PostDetailEvent
    data object CommentPosted : PostDetailEvent
    data object Reported : PostDetailEvent
}

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val communityRepository: CommunityRepository,
    authRepository: AuthRepository,
    private val reportRepository: ReportRepository,
) : ViewModel() {

    private val communityId: String =
        checkNotNull(savedStateHandle.get<String>(NavArgs.COMMUNITY_ID))
    private val postId: String =
        checkNotNull(savedStateHandle.get<String>(NavArgs.POST_ID))

    val post: StateFlow<CommunityPost?> =
        communityRepository.observePost(communityId, postId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val comments: StateFlow<List<Comment>> =
        communityRepository.observeComments(communityId, postId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val authStateFlow = authRepository.authState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AuthState.Loading,
    )

    val isSignedIn: StateFlow<Boolean> = authStateFlow
        .map { it is AuthState.SignedIn }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    val currentUid: StateFlow<String?> = authStateFlow
        .map { (it as? AuthState.SignedIn)?.profile?.uid }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private val _events = Channel<PostDetailEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun toggleLike() {
        viewModelScope.launch {
            communityRepository.toggleLike(communityId, postId).onFailure {
                _events.send(PostDetailEvent.Error(it.localizedMessage.orEmpty()))
            }
        }
    }

    fun addComment(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            communityRepository.addComment(communityId, postId, text.trim()).fold(
                onSuccess = { _events.send(PostDetailEvent.CommentPosted) },
                onFailure = { _events.send(PostDetailEvent.Error(it.localizedMessage.orEmpty())) },
            )
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            communityRepository.deleteComment(communityId, postId, commentId).onFailure {
                _events.send(PostDetailEvent.Error(it.localizedMessage.orEmpty()))
            }
        }
    }

    fun voteOnPoll(optionId: String) {
        viewModelScope.launch {
            communityRepository.voteOnPoll(communityId, postId, optionId).onFailure {
                _events.send(PostDetailEvent.Error(it.localizedMessage.orEmpty()))
            }
        }
    }

    fun reportPost(reason: ReportReason, notes: String?) {
        viewModelScope.launch {
            reportRepository.submitReport(
                contentType = ReportedContentType.Post,
                communityId = communityId,
                postId = postId,
                commentId = null,
                reason = reason,
                notes = notes,
            ).onSuccess {
                _events.send(PostDetailEvent.Reported)
            }.onFailure {
                _events.send(PostDetailEvent.Error(it.localizedMessage.orEmpty()))
            }
        }
    }
}
