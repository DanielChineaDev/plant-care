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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val rawCommunities: StateFlow<List<Community>> =
        communityRepository.observeCommunities().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * Comunidades filtradas por el texto del buscador. Si el buscador esta
     * vacio, devuelve todas. Match case-insensitive sobre nombre y
     * descripcion.
     */
    val communities: StateFlow<List<Community>> = combine(rawCommunities, _searchQuery) { list, q ->
        val term = q.trim().lowercase()
        if (term.isEmpty()) list
        else list.filter { c ->
            c.name.lowercase().contains(term) ||
                c.description.lowercase().contains(term)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun onSearchQueryChange(value: String) {
        _searchQuery.value = value
    }

    /**
     * Pair de (populares, resto) calculado una sola vez por emision de
     * `communities`. Antes lo haciamos en dos `map`/`stateIn` independientes
     * y se hacia el sort dos veces.
     */
    private val partitionedCommunities = communities
        .map { list ->
            val sorted = list.sortedByDescending { it.memberCount }
            val popular = sorted.take(POPULAR_LIMIT)
            val popularIds = popular.map { it.id }.toSet()
            val others = list.filter { it.id !in popularIds }
                .sortedBy { it.name.lowercase() }
            popular to others
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList<Community>() to emptyList(),
        )

    /** Top 5 por miembros para el carrusel "Comunidades populares". */
    val popularCommunities: StateFlow<List<Community>> = partitionedCommunities
        .map { it.first }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Resto de comunidades (las que no estan en el carrusel popular). */
    val otherCommunities: StateFlow<List<Community>> = partitionedCommunities
        .map { it.second }
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
    val featuredPosts: StateFlow<List<FeaturedPost>> = rawCommunities
        .flatMapLatest { all ->
            val sample = all.sortedByDescending { it.memberCount }.take(FEATURED_SOURCE_COMMUNITIES)
            if (sample.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    sample.map { community ->
                        // Combinamos posts con el set de likes del user para
                        // que el corazon se vea rojo si ya le diste like.
                        combine(
                            communityRepository.observePosts(community.id, FEATURED_PER_COMMUNITY),
                            communityRepository.observeLikedPostsInCommunity(community.id),
                        ) { posts, liked ->
                            posts.map { p ->
                                FeaturedPost(p.copy(isLikedByMe = p.id in liked), community)
                            }
                        }
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

    /**
     * "Trending now": comunidades con mas publicaciones en las ultimas 24h.
     * Observamos los posts recientes de cada comunidad y las rankeamos por
     * numero de posts dentro de la ventana. Solo aparecen las que tienen al
     * menos un post reciente.
     */
    val trendingCommunities: StateFlow<List<Community>> = rawCommunities
        .flatMapLatest { all ->
            if (all.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    all.map { community ->
                        communityRepository.observePosts(community.id, TRENDING_PER_COMMUNITY)
                            .map { posts ->
                                val cutoff = System.currentTimeMillis() - DAY_MS
                                community to posts.count { it.createdAt >= cutoff }
                            }
                    },
                ) { pairs ->
                    pairs.toList()
                        .filter { it.second > 0 }
                        .sortedByDescending { it.second }
                        .take(TRENDING_LIMIT)
                        .map { it.first }
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
        private const val TRENDING_PER_COMMUNITY = 20
        private const val TRENDING_LIMIT = 8
        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
