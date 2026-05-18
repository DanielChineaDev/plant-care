package com.BPO.plantcare.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.usecase.ObserveMyPlantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class BottomBarCounts(
    val myPlants: Int = 0,
)

@HiltViewModel
class BottomBarViewModel @Inject constructor(
    observeMyPlants: ObserveMyPlantsUseCase,
) : ViewModel() {

    // Por ahora solo mostramos badge en "Mis plantas". Los unread de mensajes
    // tendrian que venir de un campo unread en /conversations o de un counter
    // local — no esta implementado todavia.
    val counts: StateFlow<BottomBarCounts> = observeMyPlants()
        .map { BottomBarCounts(myPlants = it.size) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BottomBarCounts(),
        )
}
