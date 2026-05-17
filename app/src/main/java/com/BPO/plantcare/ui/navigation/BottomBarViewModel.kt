package com.BPO.plantcare.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.CalendarEventType
import com.BPO.plantcare.domain.usecase.GetCalendarEventsUseCase
import com.BPO.plantcare.domain.usecase.ObserveMyPlantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class BottomBarCounts(
    val myPlants: Int = 0,
    val calendarToday: Int = 0,
)

@HiltViewModel
class BottomBarViewModel @Inject constructor(
    observeMyPlants: ObserveMyPlantsUseCase,
    getCalendarEvents: GetCalendarEventsUseCase,
) : ViewModel() {

    val counts: StateFlow<BottomBarCounts> = combine(
        observeMyPlants().map { it.size },
        getCalendarEvents().map { events ->
            val today = LocalDate.now()
            events[today].orEmpty().count { it.type == CalendarEventType.WateringDue }
        },
    ) { myPlants, todayDue ->
        BottomBarCounts(myPlants = myPlants, calendarToday = todayDue)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BottomBarCounts(),
    )
}
