package com.BPO.plantcare.ui.screens.editprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.model.UserProfile
import com.BPO.plantcare.domain.repository.AuthRepository
import com.BPO.plantcare.domain.repository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * VM de la pantalla "Editar perfil". Trabaja con tres operaciones
 * independientes:
 *   - cambiar nombre
 *   - cambiar foto (subida a Storage + update photoUrl)
 *   - cambiar contrasena (FirebaseAuth.updatePassword, requiere reauth
 *     reciente; mostramos un error claro si no)
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val profile: StateFlow<UserProfile?> = authRepository.authState
        .map { (it as? AuthState.SignedIn)?.profile }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    private val _events = Channel<EditProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun saveName(newName: String) {
        val cleaned = newName.trim()
        if (cleaned.isEmpty()) {
            _events.trySend(EditProfileEvent.Error("El nombre no puede estar vacio"))
            return
        }
        _state.update { it.copy(savingName = true) }
        viewModelScope.launch {
            authRepository.updateDisplayName(cleaned)
                .onSuccess {
                    _state.update { it.copy(savingName = false) }
                    _events.trySend(EditProfileEvent.NameSaved)
                }
                .onFailure { err ->
                    _state.update { it.copy(savingName = false) }
                    _events.trySend(EditProfileEvent.Error(humanize(err)))
                }
        }
    }

    fun saveDetails(bio: String, location: String, favoritePlants: List<String>) {
        _state.update { it.copy(savingDetails = true) }
        viewModelScope.launch {
            authRepository.updateProfileDetails(
                bio = bio,
                location = location,
                favoritePlants = favoritePlants,
            ).onSuccess {
                _state.update { it.copy(savingDetails = false) }
                _events.trySend(EditProfileEvent.DetailsSaved)
            }.onFailure { err ->
                _state.update { it.copy(savingDetails = false) }
                _events.trySend(EditProfileEvent.Error(humanize(err)))
            }
        }
    }

    fun uploadAvatar(file: File) {
        _state.update { it.copy(uploadingPhoto = true) }
        viewModelScope.launch {
            authRepository.updateAvatar(file)
                .onSuccess {
                    _state.update { it.copy(uploadingPhoto = false) }
                    _events.trySend(EditProfileEvent.PhotoSaved)
                }
                .onFailure { err ->
                    _state.update { it.copy(uploadingPhoto = false) }
                    _events.trySend(EditProfileEvent.Error(humanize(err)))
                }
        }
    }

    fun changePassword(newPassword: String) {
        if (newPassword.length < 6) {
            _events.trySend(EditProfileEvent.Error("La contrasena debe tener al menos 6 caracteres"))
            return
        }
        _state.update { it.copy(changingPassword = true) }
        viewModelScope.launch {
            authRepository.updatePassword(newPassword)
                .onSuccess {
                    _state.update { it.copy(changingPassword = false) }
                    _events.trySend(EditProfileEvent.PasswordChanged)
                }
                .onFailure { err ->
                    _state.update { it.copy(changingPassword = false) }
                    _events.trySend(EditProfileEvent.Error(humanize(err)))
                }
        }
    }

    private fun humanize(t: Throwable): String {
        val raw = t.localizedMessage ?: t.message ?: "Error desconocido"
        return when {
            raw.contains("requires-recent-login", ignoreCase = true) ||
                raw.contains("recent authentication", ignoreCase = true) ->
                "Por seguridad, vuelve a iniciar sesion y reintenta cambiar la contrasena."
            raw.contains("weak-password", ignoreCase = true) ->
                "La contrasena es demasiado debil."
            raw.contains("network", ignoreCase = true) ->
                "Sin conexion. Revisa tu internet."
            else -> raw
        }
    }
}

data class EditProfileState(
    val savingName: Boolean = false,
    val uploadingPhoto: Boolean = false,
    val changingPassword: Boolean = false,
    val savingDetails: Boolean = false,
)

sealed interface EditProfileEvent {
    data object NameSaved : EditProfileEvent
    data object PhotoSaved : EditProfileEvent
    data object PasswordChanged : EditProfileEvent
    data object DetailsSaved : EditProfileEvent
    data class Error(val message: String) : EditProfileEvent
}
