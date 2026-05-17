package com.BPO.plantcare.domain.repository

import android.content.Context
import com.BPO.plantcare.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val profile: UserProfile) : AuthState
}

interface AuthRepository {
    /**
     * Estado actual de la sesion. Emite cambios cuando el usuario hace login,
     * logout o cuando se carga su perfil desde Firestore por primera vez.
     */
    val authState: Flow<AuthState>

    /**
     * Lanza el flujo de Sign-In con Google via Credentials Manager y, si tiene
     * exito, crea/actualiza el documento del usuario en Firestore.
     *
     * Necesita el Activity [context] (Credentials Manager requiere uno para
     * mostrar el bottom sheet del sistema).
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<UserProfile>

    suspend fun signOut()
}
