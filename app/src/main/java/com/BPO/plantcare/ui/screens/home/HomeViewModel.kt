package com.BPO.plantcare.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.Community
import com.BPO.plantcare.domain.model.CommunityPost
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.repository.CommunityRepository
import com.BPO.plantcare.domain.usecase.ObserveMyPlantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pestanas del feed de Inicio:
 *  - ForYou: posts de las comunidades a las que el user esta unido.
 *  - Recent: posts de TODAS las comunidades, ordenados por reciente.
 *  - Top: posts de TODAS las comunidades, ordenados por engagement.
 */
enum class FeedTab { ForYou, Recent, Top }

data class FeedItem(
    val post: CommunityPost,
    val community: Community,
)

/**
 * VM del Home (Inicio). Cuelga:
 *   - recentPlants: las ultimas plantas anadidas (para la fila inferior).
 *   - feed: posts agregados de TODAS las comunidades a las que el user
 *     esta unido, ordenados por reciente o por engagement (likes + comments).
 *
 * El feed combina dinamicamente N flows (uno por comunidad). Para muchas
 * comunidades esto puede ser pesado; con joinedCommunities < 20 va sobrado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeMyPlants: ObserveMyPlantsUseCase,
    private val communityRepository: CommunityRepository,
) : ViewModel() {

    val recentPlants: StateFlow<List<Plant>> = observeMyPlants()
        .map { list -> list.take(MAX_RECENTS) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _tab = MutableStateFlow(FeedTab.ForYou)
    val tab: StateFlow<FeedTab> = _tab.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val allCommunities: StateFlow<List<Community>> =
        communityRepository.observeCommunities()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    private val joinedCommunities: StateFlow<List<Community>> = allCommunities
        .map { list -> list.filter { it.isMember } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Hasta 5 comunidades NO unidas, ordenadas por miembros, para el
     * empty state "Unete a...". */
    val suggestedCommunities: StateFlow<List<Community>> = allCommunities
        .map { list ->
            list.filter { !it.isMember }
                .sortedByDescending { it.memberCount }
                .take(5)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    // Posts agregados de TODAS las comunidades disponibles. La pestana
    // "Para ti" filtra despues a las unidas; "Recientes"/"Mejor valoradas"
    // usan el conjunto completo como descubrimiento.
    private val aggregatedPosts: StateFlow<List<FeedItem>> = allCommunities
        // Solo reconstruimos los N listeners si cambia el SET de comunidades.
        .distinctUntilChanged { old, new -> old.map { it.id }.toSet() == new.map { it.id }.toSet() }
        .flatMapLatest { communities ->
            if (communities.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    communities.map { community ->
                        // Combinamos posts con el set de likes del usuario para
                        // que el corazon se vea rojo si ya le diste like.
                        combine(
                            communityRepository.observePosts(community.id, FEED_LIMIT_PER_COMMUNITY),
                            communityRepository.observeLikedPostsInCommunity(community.id),
                        ) { posts, liked ->
                            posts.map { FeedItem(it.copy(isLikedByMe = it.id in liked), community) }
                        }
                    },
                ) { lists ->
                    lists.toList().flatMap { it.toList() }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val feed: StateFlow<List<FeedItem>> =
        combine(aggregatedPosts, joinedCommunities, _tab) { posts, joined, tab ->
            val joinedIds = joined.map { it.id }.toSet()
            when (tab) {
                FeedTab.ForYou -> posts
                    .filter { it.community.id in joinedIds }
                    .sortedByDescending { it.post.createdAt }
                FeedTab.Recent -> posts.sortedByDescending { it.post.createdAt }
                FeedTab.Top -> posts.sortedByDescending {
                    it.post.likeCount * 2 + it.post.commentCount
                }
            }.take(FEED_TOTAL_LIMIT)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val hasJoinedCommunities: StateFlow<Boolean> = joinedCommunities
        .map { it.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    /**
     * True hasta que llega la primera emision REAL del feed agregado.
     * Sirve para mostrar skeleton/shimmer en lugar de pantalla vacia
     * mientras Firestore responde por primera vez.
     */
    val feedLoading: StateFlow<Boolean> = aggregatedPosts
        .map { false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    fun setTab(tab: FeedTab) {
        _tab.value = tab
    }

    fun toggleLike(communityId: String, postId: String) {
        viewModelScope.launch { communityRepository.toggleLike(communityId, postId) }
    }

    fun joinCommunity(communityId: String) {
        viewModelScope.launch { communityRepository.joinCommunity(communityId) }
    }

    /**
     * Pull-to-refresh. El feed ya es real-time via snapshotListener, asi
     * que esto es sobre todo feedback: mostramos el spinner un instante
     * mientras Firestore confirma. Si hay cambios pendientes, llegan solos.
     */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(800)
            _isRefreshing.value = false
        }
    }

    companion object {
        const val MAX_RECENTS = 5
        const val FEED_LIMIT_PER_COMMUNITY = 20
        const val FEED_TOTAL_LIMIT = 50
    }
}
