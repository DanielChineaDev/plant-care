package com.BPO.plantcare.ui.screens.lightmeter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.core.sensor.LightSensorReader
import com.BPO.plantcare.domain.model.LightLevel
import com.BPO.plantcare.domain.model.PlantCareGuide
import com.BPO.plantcare.domain.usecase.ClassifyLuxUseCase
import com.BPO.plantcare.domain.usecase.FindPlantsForLightLevelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LightMeterState(
    val available: Boolean = true,
    val lux: Float? = null,
    val level: LightLevel? = null,
    val candidates: List<PlantCareGuide> = emptyList(),
)

@HiltViewModel
class LightMeterViewModel @Inject constructor(
    private val sensor: LightSensorReader,
    private val classifyLux: ClassifyLuxUseCase,
    private val findPlants: FindPlantsForLightLevelUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LightMeterState(available = sensor.isAvailable))
    val state: StateFlow<LightMeterState> = _state.asStateFlow()

    init {
        if (sensor.isAvailable) {
            viewModelScope.launch {
                sensor.readLux().collect { lux ->
                    val level = classifyLux(lux)
                    _state.update {
                        it.copy(
                            lux = lux,
                            level = level,
                            candidates = findPlants(level),
                        )
                    }
                }
            }
        }
    }
}
