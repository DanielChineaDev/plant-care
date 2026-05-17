package com.BPO.plantcare.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.BPO.plantcare.R
import com.BPO.plantcare.domain.model.UserProfile
import com.BPO.plantcare.domain.repository.AuthRepository
import com.BPO.plantcare.domain.repository.AuthState
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthRepository {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val authState: Flow<AuthState> = firebaseAuthFlow()
        .distinctUntilChanged()
        .transformLatest { user ->
            if (user == null) {
                emit(AuthState.SignedOut)
            } else {
                emit(AuthState.Loading)
                val profile = loadOrCreateProfile(user)
                emit(AuthState.SignedIn(profile))
            }
        }
        .flowOn(Dispatchers.IO)

    private fun firebaseAuthFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithGoogle(activityContext: Context): Result<UserProfile> =
        runCatching {
            val webClientId = appContext.getString(R.string.default_web_client_id)
            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val response = credentialManager.getCredential(activityContext, request)
            val credential = response.credential
            val idToken = when (credential) {
                is CustomCredential ->
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        GoogleIdTokenCredential.createFrom(credential.data).idToken
                    } else error("Tipo de credencial inesperado: ${credential.type}")
                else -> error("Respuesta de credencial no soportada")
            }
            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(authCredential).await()
            val user = authResult.user ?: error("Firebase no devolvio usuario tras login")
            loadOrCreateProfile(user)
        }

    override suspend fun signOut() {
        firebaseAuth.signOut()
        // Tambien limpiamos las credenciales cacheadas para que el siguiente
        // login muestre el selector de cuentas.
        runCatching {
            CredentialManager.create(appContext)
                .clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
        }
    }

    private suspend fun loadOrCreateProfile(user: FirebaseUser): UserProfile {
        val doc = firestore.collection(USERS).document(user.uid).get().await()
        return if (doc.exists()) {
            UserProfile(
                uid = user.uid,
                displayName = doc.getString("displayName") ?: user.displayName,
                email = doc.getString("email") ?: user.email,
                photoUrl = doc.getString("photoUrl") ?: user.photoUrl?.toString(),
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
            )
        } else {
            val now = System.currentTimeMillis()
            val data = mapOf(
                "uid" to user.uid,
                "displayName" to user.displayName,
                "email" to user.email,
                "photoUrl" to user.photoUrl?.toString(),
                "createdAt" to now,
            )
            firestore.collection(USERS).document(user.uid).set(data).await()
            UserProfile(
                uid = user.uid,
                displayName = user.displayName,
                email = user.email,
                photoUrl = user.photoUrl?.toString(),
                createdAt = now,
            )
        }
    }

    companion object {
        private const val USERS = "users"
    }
}
