package com.BPO.plantcare.ui.screens.globalsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.GlobalSearchResult
import com.BPO.plantcare.domain.repository.GlobalSearchRepository
import com.BPO.plantcare.domain.repository.PlantCatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Busqueda global: mezcla especies del catalogo local con comunidades y
 * usuarios de Firestore. Debouncing 300 ms entre cambios de texto.
 *
 * Para no exponer detalles del repo, la pantalla solo observa
 * [results], [loading] y [query].
 */
@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    private val searchRepository: GlobalSearchRepository,
    private val catalog: PlantCatalogRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<GlobalSearchResult>>(emptyList())
    val results: StateFlow<List<GlobalSearchResult>> = _results.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private var debouncedJob: Job? = null

    init {
        observeQuery()
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    @OptIn(FlowPreview::class)
    private fun observeQuery() {
        viewModelScope.launch {
            _query
                .debounce(300)
                .distinctUntilChanged()
                .collect { q -> runSearch(q) }
        }
    }

    private fun runSearch(q: String) {
        debouncedJob?.cancel()
        if (q.trim().length < 2) {
            _results.value = emptyList()
            _loading.value = false
            return
        }
        _loading.value = true
        debouncedJob = viewModelScope.launch {
            val species = catalog.all()
                .filter { guide ->
                    val term = q.lowercase()
                    guide.scientificName.lowercase().contains(term) ||
                        guide.commonNames.any { it.lowercase().contains(term) }
                }
                .take(SPECIES_LIMIT)
                .map { guide ->
                    GlobalSearchResult.Species(
                        id = guide.scientificName,
                        displayName = guide.commonNames.firstOrNull() ?: guide.scientificName,
                        subtitle = guide.scientificName,
                        imageUrl = null,
                        scientificName = guide.scientificName,
                    )
                }

            val remote = searchRepository.search(q).getOrDefault(emptyList())
            _results.update { species + remote }
            _loading.value = false
        }
    }

    companion object {
        private const val SPECIES_LIMIT = 10
    }
}
