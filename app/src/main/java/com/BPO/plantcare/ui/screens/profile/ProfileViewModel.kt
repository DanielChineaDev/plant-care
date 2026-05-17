package com.BPO.plantcare.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.core.work.WateringReminderManager
import com.BPO.plantcare.domain.model.UserSettings
import com.BPO.plantcare.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val reminderManager: WateringReminderManager,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = preferences.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings(),
    )

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { reminderManager.setEnabled(enabled) }
    }

    fun setReminderHour(hour: Int) {
        viewModelScope.launch { reminderManager.setHour(hour) }
    }

    fun setTravelEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setTravelEnabled(enabled) }
    }

    fun setTravelRange(start: Long?, end: Long?) {
        viewModelScope.launch { preferences.setTravelRange(start, end) }
    }

    fun testWateringNotification() {
        reminderManager.runNow()
    }
}
