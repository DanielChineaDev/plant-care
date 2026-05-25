package com.BPO.plantcare.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.BPO.plantcare.core.backup.PlantsBackupExporter
import com.BPO.plantcare.core.backup.PlantsBackupImporter
import com.BPO.plantcare.core.location.LocationProvider
import com.BPO.plantcare.core.work.WateringReminderManager
import com.BPO.plantcare.domain.model.UserProfile
import com.BPO.plantcare.domain.model.UserSettings
import com.BPO.plantcare.domain.repository.AuthRepository
import com.BPO.plantcare.domain.repository.AuthState
import com.BPO.plantcare.domain.repository.PreferencesRepository
import com.BPO.plantcare.domain.repository.PublicProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileEvent {
    data object LocationSaved : ProfileEvent
    data object LocationUnavailable : ProfileEvent
    data class SignInFailed(val message: String) : ProfileEvent
    data object SignedOut : ProfileEvent
    data class PublicToggled(val enabled: Boolean) : ProfileEvent
    data object Resynced : ProfileEvent
    data class PublicError(val message: String) : ProfileEvent
    data class BackupExported(val plantCount: Int) : ProfileEvent
    data class BackupFailed(val message: String) : ProfileEvent
    data class BackupImported(val plantCount: Int) : ProfileEvent
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val reminderManager: WateringReminderManager,
    private val locationProvider: LocationProvider,
    private val authRepository: AuthRepository,
    private val publicProfileRepository: PublicProfileRepository,
    private val backupExporter: PlantsBackupExporter,
    private val backupImporter: PlantsBackupImporter,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
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

    /**
     * Perfil observado en tiempo real desde Firestore. A diferencia de
     * [authState] (que carga el perfil una sola vez), este flujo refleja al
     * instante los cambios de visibilidad escritos por el propio usuario,
     * para que los Switch de Ajustes se actualicen al pulsarlos.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val profile: StateFlow<UserProfile?> = authRepository.authState
        .map { (it as? AuthState.SignedIn)?.profile?.uid }
        .flatMapLatest { uid ->
            if (uid == null) flowOf(null)
            else publicProfileRepository.observeUserProfile(uid)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
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

    fun setSeasonalAdjustEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setSeasonalAdjustEnabled(enabled) }
    }

    fun setThemePalette(palette: String) {
        viewModelScope.launch { preferences.setThemePalette(palette) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { preferences.setDynamicColor(enabled) }
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

    fun setCollectionPublic(enabled: Boolean) {
        viewModelScope.launch {
            publicProfileRepository.setMyCollectionPublic(enabled).fold(
                onSuccess = { _events.send(ProfileEvent.PublicToggled(enabled)) },
                onFailure = { _events.send(ProfileEvent.PublicError(it.localizedMessage.orEmpty())) },
            )
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            backupExporter.exportToUri(appContext, uri).fold(
                onSuccess = { count -> _events.send(ProfileEvent.BackupExported(count)) },
                onFailure = { _events.send(ProfileEvent.BackupFailed(it.localizedMessage.orEmpty())) },
            )
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            backupImporter.importFromUri(appContext, uri).fold(
                onSuccess = { count -> _events.send(ProfileEvent.BackupImported(count)) },
                onFailure = { _events.send(ProfileEvent.BackupFailed(it.localizedMessage.orEmpty())) },
            )
        }
    }

    fun resyncPublicCollection() {
        viewModelScope.launch {
            publicProfileRepository.resyncMyPublicCollection().fold(
                onSuccess = { _events.send(ProfileEvent.Resynced) },
                onFailure = { _events.send(ProfileEvent.PublicError(it.localizedMessage.orEmpty())) },
            )
        }
    }

    fun setBadgesPublic(enabled: Boolean) = updateVisibility { publicProfileRepository.setBadgesPublic(enabled) }
    fun setDiaryPublic(enabled: Boolean) = updateVisibility { publicProfileRepository.setDiaryPublic(enabled) }
    fun setNotesPublic(enabled: Boolean) = updateVisibility { publicProfileRepository.setNotesPublic(enabled) }
    fun setCareInfoPublic(enabled: Boolean) = updateVisibility { publicProfileRepository.setCareInfoPublic(enabled) }

    private fun updateVisibility(action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            action().onFailure {
                _events.send(ProfileEvent.PublicError(it.localizedMessage.orEmpty()))
            }
        }
    }
}
