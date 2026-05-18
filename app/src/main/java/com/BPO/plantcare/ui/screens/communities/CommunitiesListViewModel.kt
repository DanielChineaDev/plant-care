package com.BPO.plantcare.ui.screens.communities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.Community
import com.BPO.plantcare.domain.model.CommunityPost
import com.BPO.plantcare.domain.repository.AuthRepository
import com.BPO.plantcare.domain.repository.AuthState
import com.BPO.plantcare.domain.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CommunitiesEvent {
    data class Error(val message: String) : CommunitiesEvent
    data class CommunityCreated(val id: String) : CommunitiesEvent
}

data class FeaturedPost(
    val post: CommunityPost,
    val community: Community,
)

@OptIn(ExperimentalCoroutinesApi::class)
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

    /** Top 5 por miembros para el carrusel "Espacios populares". */
    val popularCommunities: StateFlow<List<Community>> = communities
        .map { list -> list.sortedByDescending { it.memberCount }.take(POPULAR_LIMIT) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Resto de comunidades (las que no estan en el carrusel popular). */
    val otherCommunities: StateFlow<List<Community>> = communities
        .map { list ->
            val populars = list.sortedByDescending { it.memberCount }.take(POPULAR_LIMIT).map { it.id }
            list.filter { it.id !in populars }.sortedBy { it.name.lowercase() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /**
     * Posts mas populares de TODAS las comunidades disponibles, no solo las
     * unidas. Se usa para el bloque "Publicaciones destacadas" al fondo de
     * Comunidades como descubrimiento.
     */
    val featuredPosts: StateFlow<List<FeaturedPost>> = communities
        .flatMapLatest { all ->
            val sample = all.sortedByDescending { it.memberCount }.take(FEATURED_SOURCE_COMMUNITIES)
            if (sample.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    sample.map { community ->
                        communityRepository.observePosts(community.id, FEATURED_PER_COMMUNITY)
                            .map { posts -> posts.map { FeaturedPost(it, community) } }
                    },
                ) { lists ->
                    lists.toList().flatMap { it.toList() }
                        .sortedByDescending { it.post.likeCount * 2 + it.post.commentCount }
                        .take(FEATURED_TOTAL)
                }
            }
        }
        .stateIn(
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

    /** Solo los admins pueden crear comunidades. */
    val isAdmin: StateFlow<Boolean> = authRepository.authState
        .map { (it as? AuthState.SignedIn)?.profile?.isAdmin ?: false }
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

    fun toggleLike(communityId: String, postId: String) {
        viewModelScope.launch { communityRepository.toggleLike(communityId, postId) }
    }

    fun createCommunity(
        name: String,
        description: String,
        emoji: String,
        photoFile: File? = null,
    ) {
        viewModelScope.launch {
            communityRepository.createCommunity(name, description, emoji, photoFile).fold(
                onSuccess = { _events.send(CommunitiesEvent.CommunityCreated(it)) },
                onFailure = { _events.send(CommunitiesEvent.Error(it.localizedMessage.orEmpty())) },
            )
        }
    }

    companion object {
        private const val POPULAR_LIMIT = 5
        private const val FEATURED_SOURCE_COMMUNITIES = 5
        private const val FEATURED_PER_COMMUNITY = 10
        private const val FEATURED_TOTAL = 10
    }
}
