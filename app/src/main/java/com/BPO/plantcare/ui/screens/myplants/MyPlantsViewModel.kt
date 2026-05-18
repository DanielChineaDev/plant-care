package com.BPO.plantcare.ui.screens.myplants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.model.PlantStatus
import com.BPO.plantcare.domain.model.status
import com.BPO.plantcare.domain.usecase.MarkPlantWateredUseCase
import com.BPO.plantcare.domain.usecase.ObserveMyPlantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PlantsFilter { All, NeedsAttention, Healthy, NotWatered }

data class PlantsFilters(
    val query: String = "",
    val filter: PlantsFilter = PlantsFilter.All,
)

@HiltViewModel
class MyPlantsViewModel @Inject constructor(
    observeMyPlants: ObserveMyPlantsUseCase,
    private val markWatered: MarkPlantWateredUseCase,
) : ViewModel() {

    private val _filters = MutableStateFlow(PlantsFilters())
    val filters: StateFlow<PlantsFilters> = _filters.asStateFlow()

    private val source: StateFlow<List<Plant>> = observeMyPlants()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val plants: StateFlow<List<Plant>> = combine(source, _filters) { list, filters ->
        val q = filters.query.trim().lowercase()
        list
            .filter { plant ->
                if (q.isEmpty()) true
                else plant.displayName.lowercase().contains(q) ||
                    plant.scientificName.lowercase().contains(q)
            }
            .filter { plant ->
                when (filters.filter) {
                    PlantsFilter.All -> true
                    PlantsFilter.NeedsAttention -> {
                        val s = plant.status()
                        s == PlantStatus.Thirsty || s == PlantStatus.Attention
                    }
                    PlantsFilter.Healthy -> plant.status() == PlantStatus.Healthy
                    PlantsFilter.NotWatered -> plant.status() == PlantStatus.NotWatered
                }
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun onQueryChange(q: String) {
        _filters.value = _filters.value.copy(query = q)
    }

    fun setFilter(filter: PlantsFilter) {
        _filters.value = _filters.value.copy(filter = filter)
    }

    fun onWatered(plantId: Long) {
        viewModelScope.launch { markWatered(plantId) }
    }
}
