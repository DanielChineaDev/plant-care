package com.BPO.plantcare.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.CalendarEvent
import com.BPO.plantcare.domain.usecase.GetCalendarEventsUseCase
import com.BPO.plantcare.domain.usecase.MarkPlantWateredUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    getCalendarEvents: GetCalendarEventsUseCase,
    private val markWatered: MarkPlantWateredUseCase,
) : ViewModel() {

    val events: StateFlow<Map<LocalDate, List<CalendarEvent>>> = getCalendarEvents()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap(),
        )

    fun onWatered(plantId: Long) {
        viewModelScope.launch { markWatered(plantId) }
    }
}
