package com.BPO.plantcare.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.PlantStatus
import com.BPO.plantcare.domain.model.status
import com.BPO.plantcare.domain.usecase.ObserveMyPlantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class BottomBarCounts(
    val plantsNeedAttention: Boolean = false,
)

@HiltViewModel
class BottomBarViewModel @Inject constructor(
    observeMyPlants: ObserveMyPlantsUseCase,
) : ViewModel() {

    // El badge en "Plantas" solo indica si HAY alguna planta que requiere
    // accion (sedienta o atenta). No nos importa cuantas son, solo el punto
    // como aviso visual.
    val counts: StateFlow<BottomBarCounts> = observeMyPlants()
        .map { list ->
            val needs = list.any { plant ->
                val s = plant.status()
                s == PlantStatus.Thirsty || s == PlantStatus.Attention
            }
            BottomBarCounts(plantsNeedAttention = needs)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BottomBarCounts(),
        )
}
