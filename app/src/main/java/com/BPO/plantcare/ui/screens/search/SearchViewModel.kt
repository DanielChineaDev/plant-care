package com.BPO.plantcare.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.CareDifficulty
import com.BPO.plantcare.domain.model.LightLevel
import com.BPO.plantcare.domain.model.PlantCareGuide
import com.BPO.plantcare.domain.repository.PlantCatalogRepository
import com.BPO.plantcare.domain.repository.WikipediaRepository
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

enum class LocationFilter { ALL, INDOOR, OUTDOOR }

data class SearchFilters(
    val query: String = "",
    val difficulty: CareDifficulty? = null,
    val light: LightLevel? = null,
    val location: LocationFilter = LocationFilter.ALL,
) {
    val hasActiveFilters: Boolean
        get() = difficulty != null || light != null || location != LocationFilter.ALL
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val catalog: PlantCatalogRepository,
    private val wikipediaRepository: WikipediaRepository,
) : ViewModel() {

    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters.asStateFlow()

    private val all = MutableStateFlow(catalog.all())

    val results: StateFlow<List<PlantCareGuide>> = combine(all, _filters) { list, f ->
        list.asSequence()
            .filter { matchesQuery(it, f.query) }
            .filter { f.difficulty == null || it.difficulty == f.difficulty }
            .filter { f.light == null || it.light == f.light }
            .filter {
                when (f.location) {
                    LocationFilter.ALL -> true
                    LocationFilter.INDOOR -> it.indoor
                    LocationFilter.OUTDOOR -> it.outdoor
                }
            }
            .toList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = catalog.all(),
    )

    // Mapa observable de scientificName -> thumbnailUrl. La key sin entrada
    // significa "no se ha intentado". null como valor significa "Wikipedia
    // no tiene foto". Asi distinguimos pendiente vs sin-foto sin re-fetch.
    private val _thumbnails = MutableStateFlow<Map<String, String?>>(emptyMap())
    val thumbnails: StateFlow<Map<String, String?>> = _thumbnails.asStateFlow()

    fun ensureThumbnail(scientificName: String) {
        if (_thumbnails.value.containsKey(scientificName)) return
        // Marcamos pendiente con una entrada placeholder para evitar duplicados.
        // Solo guardamos el valor real al terminar.
        viewModelScope.launch {
            val url = runCatching { wikipediaRepository.getThumbnailUrl(scientificName) }
                .getOrNull()
            _thumbnails.update { it + (scientificName to url) }
        }
    }

    fun onQueryChange(query: String) {
        _filters.update { it.copy(query = query) }
    }

    fun toggleDifficulty(d: CareDifficulty) {
        _filters.update { it.copy(difficulty = if (it.difficulty == d) null else d) }
    }

    fun toggleLight(l: LightLevel) {
        _filters.update { it.copy(light = if (it.light == l) null else l) }
    }

    fun setLocation(loc: LocationFilter) {
        _filters.update { it.copy(location = loc) }
    }

    fun clearAll() {
        _filters.update { SearchFilters() }
    }

    private fun matchesQuery(guide: PlantCareGuide, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.lowercase().trim()
        return guide.scientificName.lowercase().contains(q) ||
                guide.commonNames.any { it.lowercase().contains(q) }
    }
}
