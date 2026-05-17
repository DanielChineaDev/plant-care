package com.BPO.plantcare.ui.screens.catalogdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.PlantCareGuide
import com.BPO.plantcare.domain.model.PlantSuggestion
import com.BPO.plantcare.domain.usecase.AddPlantFromSuggestionUseCase
import com.BPO.plantcare.domain.usecase.GetPlantCareGuideUseCase
import com.BPO.plantcare.domain.usecase.GetWikipediaSummaryUseCase
import com.BPO.plantcare.ui.navigation.NavArgs
import com.BPO.plantcare.ui.screens.common.WikipediaUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CatalogDetailEvent {
    data class Added(val displayName: String) : CatalogDetailEvent
    data class Failed(val message: String) : CatalogDetailEvent
}

@HiltViewModel
class CatalogPlantDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getCareGuide: GetPlantCareGuideUseCase,
    private val getWikipediaSummary: GetWikipediaSummaryUseCase,
    private val addPlant: AddPlantFromSuggestionUseCase,
) : ViewModel() {

    val scientificName: String = checkNotNull(savedStateHandle.get<String>(NavArgs.SCIENTIFIC_NAME))

    val guide: PlantCareGuide? = getCareGuide(scientificName)

    private val _wikipedia = MutableStateFlow<WikipediaUiState>(WikipediaUiState.Loading)
    val wikipedia: StateFlow<WikipediaUiState> = _wikipedia.asStateFlow()

    private val _events = Channel<CatalogDetailEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            getWikipediaSummary(scientificName).fold(
                onSuccess = { summary ->
                    _wikipedia.update {
                        if (summary == null) WikipediaUiState.NotFound
                        else WikipediaUiState.Loaded(summary)
                    }
                },
                onFailure = { e ->
                    _wikipedia.update { WikipediaUiState.Error(e.localizedMessage ?: "Error") }
                },
            )
        }
    }

    fun addToMyPlants() {
        val g = guide ?: return
        val wiki = (_wikipedia.value as? WikipediaUiState.Loaded)?.summary
        val suggestion = PlantSuggestion(
            scientificName = g.scientificName,
            commonNames = g.commonNames,
            family = null,
            genus = g.scientificName.substringBefore(" ").takeIf { it.isNotBlank() },
            score = 1.0,
            imageUrl = wiki?.thumbnailUrl,
        )
        viewModelScope.launch {
            addPlant(
                suggestion = suggestion,
                capturedPhoto = null,
                wateringIntervalDays = g.wateringIntervalDays,
            ).fold(
                onSuccess = {
                    val name = g.commonNames.firstOrNull() ?: g.scientificName
                    _events.send(CatalogDetailEvent.Added(name))
                },
                onFailure = { e ->
                    _events.send(CatalogDetailEvent.Failed(e.localizedMessage ?: "Error"))
                },
            )
        }
    }
}
