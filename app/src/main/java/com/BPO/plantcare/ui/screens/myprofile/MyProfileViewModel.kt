package com.BPO.plantcare.ui.screens.myprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.Achievement
import com.BPO.plantcare.domain.model.ProfileStats
import com.BPO.plantcare.domain.model.PublicPlant
import com.BPO.plantcare.domain.model.UserProfile
import com.BPO.plantcare.domain.model.achievements
import com.BPO.plantcare.domain.repository.AuthRepository
import com.BPO.plantcare.domain.repository.AuthState
import com.BPO.plantcare.domain.repository.CommunityRepository
import com.BPO.plantcare.domain.repository.PublicProfileRepository
import com.BPO.plantcare.domain.repository.WateringLogRepository
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
 * VM de la pantalla "Mi perfil" del drawer. Muestra el perfil publico del
 * usuario actual + sus plantas publicas, sus estadisticas y sus logros.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyProfileViewModel @Inject constructor(
    authRepository: AuthRepository,
    publicProfileRepository: PublicProfileRepository,
    observeMyPlants: ObserveMyPlantsUseCase,
    wateringLogRepository: WateringLogRepository,
    private val communityRepository: CommunityRepository,
) : ViewModel() {

    private val signedInProfile: StateFlow<UserProfile?> = authRepository.authState
        .map { (it as? AuthState.SignedIn)?.profile }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val profile: StateFlow<UserProfile?> = signedInProfile
        .flatMapLatest { local ->
            val uid = local?.uid ?: return@flatMapLatest flowOf<UserProfile?>(null)
            publicProfileRepository.observeUserProfile(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val publicPlants: StateFlow<List<PublicPlant>> = signedInProfile
        .flatMapLatest { local ->
            val uid = local?.uid ?: return@flatMapLatest flowOf(emptyList())
            publicProfileRepository.observePublicPlants(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Conteo de posts/comentarios del usuario (one-shot, agregacion server).
    // Se refresca cuando cambia el uid logueado.
    private val _socialCounts = MutableStateFlow(0 to 0)

    init {
        viewModelScope.launch {
            signedInProfile
                .map { it?.uid }
                .distinctUntilChanged()
                .collect { uid ->
                    if (uid == null) {
                        _socialCounts.value = 0 to 0
                    } else {
                        val posts = communityRepository.countUserPosts(uid).getOrDefault(0)
                        val comments = communityRepository.countUserComments(uid).getOrDefault(0)
                        _socialCounts.value = posts to comments
                    }
                }
        }
    }

    val stats: StateFlow<ProfileStats> = combine(
        observeMyPlants(),
        wateringLogRepository.observeAll(),
        profile,
        _socialCounts,
    ) { plants, logs, prof, counts ->
        ProfileStats(
            plantCount = plants.size,
            postCount = counts.first,
            commentCount = counts.second,
            totalWaterings = logs.size,
            memberSinceDays = daysSince(prof?.createdAt ?: 0L),
            wateringStreak = wateringStreak(logs.map { it.timestamp }),
            karma = (prof?.karma ?: 0L).coerceAtLeast(0L),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileStats())

    val achievements: StateFlow<List<Achievement>> = stats
        .map { it.achievements() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val loading: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    private fun daysSince(createdAt: Long): Int {
        if (createdAt <= 0L) return 0
        val diff = System.currentTimeMillis() - createdAt
        return (diff / DAY_MS).toInt().coerceAtLeast(0)
    }

    /**
     * Dias consecutivos (terminando hoy o ayer) con al menos un riego.
     * Si el ultimo riego fue hace mas de un dia, la racha esta rota (0).
     */
    private fun wateringStreak(timestamps: List<Long>): Int {
        if (timestamps.isEmpty()) return 0
        val days = timestamps.map { it / DAY_MS }.toSortedSet().reversed()
        val today = System.currentTimeMillis() / DAY_MS
        var expected = when {
            days.first() == today -> today
            days.first() == today - 1 -> today - 1
            else -> return 0
        }
        var streak = 0
        for (day in days) {
            if (day == expected) {
                streak++
                expected--
            } else if (day < expected) {
                break
            }
        }
        return streak
    }

    companion object {
        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
