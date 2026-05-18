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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FeedSort { Recent, Top }

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

    private val _sort = MutableStateFlow(FeedSort.Recent)
    val sort: StateFlow<FeedSort> = _sort.asStateFlow()

    private val joinedCommunities: StateFlow<List<Community>> =
        communityRepository.observeCommunities()
            .map { list -> list.filter { it.isMember } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    private val aggregatedPosts: StateFlow<List<FeedItem>> = joinedCommunities
        .flatMapLatest { communities ->
            if (communities.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    communities.map { community ->
                        communityRepository.observePosts(community.id, FEED_LIMIT_PER_COMMUNITY)
                            .map { posts -> posts.map { FeedItem(it, community) } }
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

    val feed: StateFlow<List<FeedItem>> = combine(aggregatedPosts, _sort) { posts, sort ->
        when (sort) {
            FeedSort.Recent -> posts.sortedByDescending { it.post.createdAt }
            FeedSort.Top -> posts.sortedByDescending { it.post.likeCount * 2 + it.post.commentCount }
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

    fun setSort(sort: FeedSort) {
        _sort.value = sort
    }

    fun toggleLike(communityId: String, postId: String) {
        viewModelScope.launch { communityRepository.toggleLike(communityId, postId) }
    }

    companion object {
        const val MAX_RECENTS = 5
        const val FEED_LIMIT_PER_COMMUNITY = 20
        const val FEED_TOTAL_LIMIT = 50
    }
}
