package com.BPO.plantcare.ui.screens.common

import com.BPO.plantcare.domain.model.WikipediaSummary

sealed interface WikipediaUiState {
    data object Loading : WikipediaUiState
    data class Loaded(val summary: WikipediaSummary) : WikipediaUiState
    data object NotFound : WikipediaUiState
    data class Error(val message: String) : WikipediaUiState
}
