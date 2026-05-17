package com.BPO.plantcare.ui.screens.diagnosis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.DiagnosisCategory
import com.BPO.plantcare.domain.model.PlantDiagnosis
import com.BPO.plantcare.domain.repository.DiagnosisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class DiagnosisFilters(
    val query: String = "",
    val category: DiagnosisCategory? = null,
)

@HiltViewModel
class DiagnosisListViewModel @Inject constructor(
    private val repository: DiagnosisRepository,
) : ViewModel() {

    private val _filters = MutableStateFlow(DiagnosisFilters())
    val filters: StateFlow<DiagnosisFilters> = _filters.asStateFlow()

    private val all = MutableStateFlow(repository.all())

    val results: StateFlow<List<PlantDiagnosis>> = combine(all, _filters) { list, f ->
        list.asSequence()
            .filter { f.category == null || it.category == f.category }
            .filter { matchesQuery(it, f.query) }
            .toList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = repository.all(),
    )

    fun onQueryChange(query: String) {
        _filters.update { it.copy(query = query) }
    }

    fun toggleCategory(c: DiagnosisCategory) {
        _filters.update { it.copy(category = if (it.category == c) null else c) }
    }

    fun clearAll() {
        _filters.update { DiagnosisFilters() }
    }

    private fun matchesQuery(d: PlantDiagnosis, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.lowercase().trim()
        return d.name.lowercase().contains(q) ||
                d.summary.lowercase().contains(q) ||
                d.symptoms.any { it.lowercase().contains(q) } ||
                (d.scientificName?.lowercase()?.contains(q) ?: false)
    }
}
