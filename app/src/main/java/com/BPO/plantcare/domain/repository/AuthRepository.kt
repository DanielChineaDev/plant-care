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

    /** Login con email + contrasena. */
    suspend fun signInWithEmail(email: String, password: String): Result<UserProfile>

    /**
     * Crea una cuenta nueva con email + contrasena. [displayName] se guarda
     * tanto en el perfil de Firebase Auth como en Firestore.
     */
    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
    ): Result<UserProfile>

    /** Envia un email de recuperacion de contrasena. */
    suspend fun sendPasswordReset(email: String): Result<Unit>

    /** Actualiza el nombre publico en FirebaseAuth + Firestore. */
    suspend fun updateDisplayName(displayName: String): Result<Unit>

    /**
     * Sube una imagen como avatar del usuario actual a Storage y guarda la
     * URL publica en users/{uid}/photoUrl + en FirebaseAuth.
     */
    suspend fun updateAvatar(file: java.io.File): Result<String>

    /**
     * Cambia la contrasena del usuario actual. Requiere haberse logueado
     * recientemente (de lo contrario Firebase devuelve REQUIRES_RECENT_LOGIN).
     */
    suspend fun updatePassword(newPassword: String): Result<Unit>

    /** Lee el perfil de otro usuario desde Firestore (one-shot). */
    suspend fun getProfile(uid: String): Result<UserProfile?>

    /** Registra un token FCM en users/{currentUid}/fcmTokens/{token}. */
    suspend fun registerFcmToken(token: String): Result<Unit>

    /** Borra un token FCM previamente registrado. */
    suspend fun unregisterFcmToken(token: String): Result<Unit>
}
