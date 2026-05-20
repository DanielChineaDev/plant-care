package com.BPO.plantcare.ui.screens.communityfeed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.Community
import com.BPO.plantcare.domain.model.CommunityMember
import com.BPO.plantcare.domain.model.CommunityPost
import com.BPO.plantcare.domain.model.PostTag
import com.BPO.plantcare.domain.repository.AuthRepository
import com.BPO.plantcare.domain.repository.AuthState
import com.BPO.plantcare.domain.repository.CommunityRepository
import com.BPO.plantcare.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
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

    /** Filtro de etiqueta activo en el feed (null = todas). */
    private val _tagFilter = MutableStateFlow<PostTag?>(null)
    val tagFilter: StateFlow<PostTag?> = _tagFilter.asStateFlow()

    /** Posts ya filtrados por la etiqueta seleccionada. */
    val filteredPosts: StateFlow<List<CommunityPost>> = combine(posts, _tagFilter) { list, tag ->
        if (tag == null) list else list.filter { it.tag == tag }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    /** Miembros de la comunidad (pestana "Miembros"). */
    val members: StateFlow<List<CommunityMember>> =
        communityRepository.observeMembers(communityId).stateIn(
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

    /** Solo los admins ven los controles de gestion de la comunidad. */
    val isAdmin: StateFlow<Boolean> = authRepository.authState
        .map { (it as? AuthState.SignedIn)?.profile?.isAdmin ?: false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    fun setTagFilter(tag: PostTag?) {
        _tagFilter.value = tag
    }

    private val _events = Channel<FeedEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun createPost(
        text: String,
        photoFile: File?,
        pollOptions: List<com.BPO.plantcare.domain.model.PollOption>? = null,
        tag: PostTag? = null,
    ) {
        val isPoll = pollOptions != null && pollOptions.size >= 2
        if (text.isBlank() && photoFile == null && !isPoll) return
        viewModelScope.launch {
            communityRepository.createPost(
                communityId = communityId,
                text = text.trim(),
                photoFile = photoFile,
                pollOptions = pollOptions,
                tag = tag,
            ).fold(
                onSuccess = { _events.send(FeedEvent.PostCreated) },
                onFailure = { _events.send(FeedEvent.Error(it.localizedMessage.orEmpty())) },
            )
        }
    }

    // ---- Acciones de admin ----
    fun updateCommunity(name: String, description: String, photoFile: File?) {
        viewModelScope.launch {
            communityRepository.updateCommunity(
                communityId = communityId,
                name = name.trim(),
                description = description.trim(),
                photoFile = photoFile,
            ).onFailure { _events.send(FeedEvent.Error(it.localizedMessage.orEmpty())) }
        }
    }

    fun removeMember(memberUid: String) {
        viewModelScope.launch {
            communityRepository.removeMember(communityId, memberUid)
                .onFailure { _events.send(FeedEvent.Error(it.localizedMessage.orEmpty())) }
        }
    }

    fun toggleFeatured(post: CommunityPost) {
        viewModelScope.launch {
            communityRepository.setPostFeatured(communityId, post.id, !post.featured)
                .onFailure { _events.send(FeedEvent.Error(it.localizedMessage.orEmpty())) }
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
