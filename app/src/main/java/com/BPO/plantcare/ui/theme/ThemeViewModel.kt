package com.BPO.plantcare.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ThemeState(
    val palette: AppPalette = AppPalette.Green,
    val dynamicColor: Boolean = false,
)

/**
 * Expone la preferencia de tema (paleta + color dinamico) para que
 * MainActivity la aplique en PlantCareTheme.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
) : ViewModel() {
    val theme: StateFlow<ThemeState> = preferencesRepository.settings
        .map { ThemeState(AppPalette.fromKey(it.themePalette), it.dynamicColor) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeState())
}
