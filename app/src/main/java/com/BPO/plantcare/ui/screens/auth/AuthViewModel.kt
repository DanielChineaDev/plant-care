package com.BPO.plantcare.ui.screens.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.BPO.plantcare.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * VM de la pantalla de login/registro. Mantiene el formulario, el modo
 * (login vs register) y emite eventos one-shot (errores, recuperacion
 * de contrasena enviada). El navegacion post-login la hace el observer
 * de AuthState en el gate, no este VM.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthFormState())
    val state: StateFlow<AuthFormState> = _state.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setMode(mode: AuthMode) {
        _state.update { it.copy(mode = mode, errorMessage = null) }
    }

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, errorMessage = null) }
    }

    fun onDisplayNameChange(value: String) {
        _state.update { it.copy(displayName = value, errorMessage = null) }
    }

    fun submit() {
        val current = _state.value
        val validationError = validate(current)
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError) }
            return
        }
        _state.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = when (current.mode) {
                AuthMode.Login -> authRepository.signInWithEmail(current.email, current.password)
                AuthMode.Register -> authRepository.registerWithEmail(
                    email = current.email,
                    password = current.password,
                    displayName = current.displayName,
                )
            }
            result.onSuccess {
                _state.update { it.copy(loading = false) }
                // El gate detecta SignedIn y navega solo.
            }.onFailure { error ->
                _state.update {
                    it.copy(loading = false, errorMessage = humanize(error))
                }
            }
        }
    }

    fun signInWithGoogle(activityContext: Context) {
        _state.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            authRepository.signInWithGoogle(activityContext)
                .onSuccess { _state.update { it.copy(loading = false) } }
                .onFailure { error ->
                    _state.update {
                        it.copy(loading = false, errorMessage = humanize(error))
                    }
                }
        }
    }

    fun sendPasswordReset() {
        val email = _state.value.email.trim()
        if (email.isEmpty()) {
            _state.update { it.copy(errorMessage = "Escribe tu email para enviarte el enlace") }
            return
        }
        viewModelScope.launch {
            authRepository.sendPasswordReset(email)
                .onSuccess { _events.trySend(AuthEvent.PasswordResetSent) }
                .onFailure { error ->
                    _state.update { it.copy(errorMessage = humanize(error)) }
                }
        }
    }

    private fun validate(state: AuthFormState): String? {
        if (state.email.isBlank()) return "El email es obligatorio"
        if (!state.email.contains("@")) return "Email no valido"
        if (state.password.length < 6) return "La contrasena debe tener al menos 6 caracteres"
        if (state.mode == AuthMode.Register && state.displayName.trim().isEmpty()) {
            return "Pon un nombre para que te identifiquen"
        }
        return null
    }

    private fun humanize(t: Throwable): String {
        val raw = t.localizedMessage ?: t.message ?: "Error desconocido"
        // Errores Firebase mas comunes traducidos a algo legible.
        return when {
            raw.contains("password is invalid", ignoreCase = true) ||
                raw.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                raw.contains("INVALID_PASSWORD", ignoreCase = true) -> "Email o contrasena incorrectos"
            raw.contains("no user record", ignoreCase = true) -> "No existe ninguna cuenta con ese email"
            raw.contains("email address is already in use", ignoreCase = true) ||
                raw.contains("EMAIL_EXISTS", ignoreCase = true) -> "Ya hay una cuenta con ese email"
            raw.contains("badly formatted", ignoreCase = true) -> "Email no valido"
            raw.contains("network", ignoreCase = true) -> "Sin conexion. Revisa tu internet."
            else -> raw
        }
    }
}

enum class AuthMode { Login, Register }

data class AuthFormState(
    val mode: AuthMode = AuthMode.Login,
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val loading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface AuthEvent {
    data object PasswordResetSent : AuthEvent
}
