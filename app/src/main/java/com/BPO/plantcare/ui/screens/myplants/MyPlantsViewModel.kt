package com.BPO.plantcare.ui.screens.myplants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.model.PlantStatus
import com.BPO.plantcare.domain.model.nextWateringAt
import com.BPO.plantcare.domain.model.status
import com.BPO.plantcare.domain.usecase.DeletePlantUseCase
import com.BPO.plantcare.domain.usecase.MarkPlantWateredUseCase
import com.BPO.plantcare.domain.usecase.ObserveMyPlantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PlantsFilter { All, NeedsAttention, Healthy, NotWatered }

enum class PlantsSort(@androidx.annotation.StringRes val labelRes: Int) {
    RecentlyAdded(com.BPO.plantcare.R.string.sort_recently_added),
    Alphabetical(com.BPO.plantcare.R.string.sort_alphabetical),
    NextWatering(com.BPO.plantcare.R.string.sort_next_watering),
    Status(com.BPO.plantcare.R.string.sort_status),
}

enum class PlantsViewMode { Grid, List }

data class PlantsFilters(
    val query: String = "",
    val filter: PlantsFilter = PlantsFilter.All,
    val sort: PlantsSort = PlantsSort.RecentlyAdded,
    val groupByRoom: Boolean = false,
)

@HiltViewModel
class MyPlantsViewModel @Inject constructor(
    observeMyPlants: ObserveMyPlantsUseCase,
    private val markWatered: MarkPlantWateredUseCase,
    private val deletePlant: DeletePlantUseCase,
) : ViewModel() {

    private val _filters = MutableStateFlow(PlantsFilters())
    val filters: StateFlow<PlantsFilters> = _filters.asStateFlow()

    private val _viewMode = MutableStateFlow(PlantsViewMode.Grid)
    val viewMode: StateFlow<PlantsViewMode> = _viewMode.asStateFlow()

    /** Modo seleccion multiple + set de ids seleccionados. */
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

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
            .sortedWith(sortComparator(filters.sort))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    /** Plantas agrupadas por habitacion (cuando groupByRoom esta activo). */
    val groupedPlants: StateFlow<Map<String, List<Plant>>> =
        combine(plants, _filters) { list, filters ->
            if (!filters.groupByRoom) emptyMap()
            else list.groupBy { it.room?.takeIf { r -> r.isNotBlank() } ?: "Sin ubicacion" }
                .toSortedMap()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap(),
        )

    private fun sortComparator(sort: PlantsSort): Comparator<Plant> = when (sort) {
        PlantsSort.RecentlyAdded -> compareByDescending { it.addedAt }
        PlantsSort.Alphabetical -> compareBy { it.displayName.lowercase() }
        PlantsSort.NextWatering -> compareBy { it.nextWateringAt() }
        PlantsSort.Status -> compareBy { statusOrder(it.status()) }
    }

    // Orden de urgencia para "Estado": primero lo que necesita atencion.
    private fun statusOrder(status: PlantStatus): Int = when (status) {
        PlantStatus.Thirsty -> 0
        PlantStatus.Attention -> 1
        PlantStatus.NotWatered -> 2
        PlantStatus.Healthy -> 3
    }

    fun onQueryChange(q: String) {
        _filters.update { it.copy(query = q) }
    }

    fun setFilter(filter: PlantsFilter) {
        _filters.update { it.copy(filter = filter) }
    }

    fun setSort(sort: PlantsSort) {
        _filters.update { it.copy(sort = sort) }
    }

    fun toggleGroupByRoom() {
        _filters.update { it.copy(groupByRoom = !it.groupByRoom) }
    }

    fun toggleViewMode() {
        _viewMode.update { if (it == PlantsViewMode.Grid) PlantsViewMode.List else PlantsViewMode.Grid }
    }

    fun onWatered(plantId: Long) {
        viewModelScope.launch { markWatered(plantId) }
    }

    // ---- Seleccion multiple ----
    fun startSelection(plantId: Long) {
        _selectionMode.value = true
        _selectedIds.value = setOf(plantId)
    }

    fun toggleSelected(plantId: Long) {
        _selectedIds.update { current ->
            if (plantId in current) current - plantId else current + plantId
        }
        if (_selectedIds.value.isEmpty()) _selectionMode.value = false
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
        _selectionMode.value = false
    }

    fun waterSelected() {
        val ids = _selectedIds.value
        viewModelScope.launch {
            ids.forEach { markWatered(it) }
            clearSelection()
        }
    }

    fun deleteSelected() {
        val ids = _selectedIds.value
        val toDelete = source.value.filter { it.id in ids }
        viewModelScope.launch {
            toDelete.forEach { deletePlant(it) }
            clearSelection()
        }
    }
}
