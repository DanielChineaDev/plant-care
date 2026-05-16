package com.BPO.plantcare.ui.screens.myplants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.usecase.MarkPlantWateredUseCase
import com.BPO.plantcare.domain.usecase.ObserveMyPlantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPlantsViewModel @Inject constructor(
    observeMyPlants: ObserveMyPlantsUseCase,
    private val markWatered: MarkPlantWateredUseCase,
) : ViewModel() {

    val plants: StateFlow<List<Plant>> = observeMyPlants()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun onWatered(plantId: Long) {
        viewModelScope.launch { markWatered(plantId) }
    }
}
