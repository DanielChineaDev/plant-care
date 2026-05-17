package com.BPO.plantcare.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.core.location.LocationProvider
import com.BPO.plantcare.core.work.WateringReminderManager
import com.BPO.plantcare.domain.model.UserSettings
import com.BPO.plantcare.domain.repository.AuthRepository
import com.BPO.plantcare.domain.repository.AuthState
import com.BPO.plantcare.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileEvent {
    data object LocationSaved : ProfileEvent
    data object LocationUnavailable : ProfileEvent
    data class SignInFailed(val message: String) : ProfileEvent
    data object SignedOut : ProfileEvent
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val reminderManager: WateringReminderManager,
    private val locationProvider: LocationProvider,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = preferences.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings(),
    )

    val authState: StateFlow<AuthState> = authRepository.authState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AuthState.Loading,
    )

    private val _events = Channel<ProfileEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

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

    fun setWeatherAware(enabled: Boolean) {
        viewModelScope.launch { preferences.setWeatherAware(enabled) }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            val loc = locationProvider.getLastKnownLocation()
            if (loc == null) {
                _events.send(ProfileEvent.LocationUnavailable)
            } else {
                preferences.setLocation(loc.first, loc.second, System.currentTimeMillis())
                _events.send(ProfileEvent.LocationSaved)
            }
        }
    }

    fun clearLocation() {
        viewModelScope.launch { preferences.setLocation(null, null, null) }
    }

    fun testWateringNotification() {
        reminderManager.runNow()
    }

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            authRepository.signInWithGoogle(activityContext).onFailure { e ->
                _events.send(
                    ProfileEvent.SignInFailed(
                        e.localizedMessage ?: "Error al iniciar sesion con Google"
                    )
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _events.send(ProfileEvent.SignedOut)
        }
    }
}
