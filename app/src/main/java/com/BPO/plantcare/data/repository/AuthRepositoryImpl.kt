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
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
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
    private val storage: FirebaseStorage,
) : AuthRepository {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val authState: Flow<AuthState> = firebaseAuthFlow()
        .distinctUntilChanged()
        .transformLatest { user ->
            if (user == null) {
                emit(AuthState.SignedOut)
            } else {
                // IMPORTANTE: emitimos SignedIn inmediato con los datos
                // que ya tenemos del FirebaseUser local. Antes hacia
                // emit(Loading) y luego loadOrCreateProfile (red), lo
                // que dejaba el splash colgado durante segundos si la
                // red era lenta o estaba caida.
                emit(
                    AuthState.SignedIn(
                        UserProfile(
                            uid = user.uid,
                            displayName = user.displayName,
                            email = user.email,
                            photoUrl = user.photoUrl?.toString(),
                            createdAt = 0L,
                        ),
                    ),
                )
                // En background completamos con el doc de Firestore
                // (puede traer karma, isAdmin, isCollectionPublic...).
                runCatching {
                    val full = loadOrCreateProfile(user)
                    emit(AuthState.SignedIn(full))
                }
            }
        }
        .flowOn(Dispatchers.IO)

    private fun firebaseAuthFlow(): Flow<FirebaseUser?> = callbackFlow {
        // Emitimos el currentUser ACTUAL inmediatamente al subscribirnos,
        // sin esperar al primer disparo del AuthStateListener. En arranque
        // frio el listener puede tardar varios segundos en disparar
        // (Firebase inicializa en background); sin este trySend el splash
        // se quedaba en blanco con solo el CircularProgressIndicator.
        trySend(firebaseAuth.currentUser)
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
            val profile = loadOrCreateProfile(user)
            // Registramos el token FCM tan pronto haya sesion. Si falla
            // ignoramos: el login no debe romperse por culpa de FCM.
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                registerFcmTokenInternal(user.uid, token)
            }
            profile
        }

    override suspend fun signInWithEmail(
        email: String,
        password: String,
    ): Result<UserProfile> = runCatching {
        val result = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: error("Firebase no devolvio usuario tras login")
        val profile = loadOrCreateProfile(user)
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            registerFcmTokenInternal(user.uid, token)
        }
        profile
    }

    override suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
    ): Result<UserProfile> = runCatching {
        val result = firebaseAuth
            .createUserWithEmailAndPassword(email.trim(), password)
            .await()
        val user = result.user ?: error("Firebase no devolvio usuario tras crear cuenta")
        val cleanName = displayName.trim()
        if (cleanName.isNotEmpty()) {
            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build(),
            ).await()
            // refresh local
            user.reload().await()
        }
        val profile = loadOrCreateProfile(firebaseAuth.currentUser ?: user)
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            registerFcmTokenInternal(user.uid, token)
        }
        profile
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        firebaseAuth.sendPasswordResetEmail(email.trim()).await()
    }

    override suspend fun updateDisplayName(displayName: String): Result<Unit> = runCatching {
        val user = firebaseAuth.currentUser ?: error("Sin sesion activa")
        val clean = displayName.trim()
        require(clean.isNotEmpty()) { "El nombre no puede estar vacio" }
        user.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName(clean)
                .build(),
        ).await()
        firestore.collection(USERS).document(user.uid)
            .update(
                mapOf(
                    "displayName" to clean,
                    // Lowercase para que la busqueda global encuentre el user
                    // sin importar mayusculas/minusculas.
                    "displayNameLower" to clean.lowercase(),
                ),
            ).await()
    }

    override suspend fun updateProfileDetails(
        bio: String?,
        location: String?,
        favoritePlants: List<String>,
    ): Result<Unit> = runCatching {
        val user = firebaseAuth.currentUser ?: error("Sin sesion activa")
        firestore.collection(USERS).document(user.uid)
            .update(
                mapOf(
                    "bio" to bio?.trim()?.ifBlank { null },
                    "location" to location?.trim()?.ifBlank { null },
                    "favoritePlants" to favoritePlants.map { it.trim() }.filter { it.isNotEmpty() },
                ),
            ).await()
    }

    override suspend fun updateAvatar(file: java.io.File): Result<String> = runCatching {
        val user = firebaseAuth.currentUser ?: error("Sin sesion activa")
        // Path: avatars/{uid}/avatar_{timestamp}.jpg. Cambiamos el nombre cada
        // vez para que el CDN no sirva la antigua cacheada.
        val ref = storage.reference
            .child("avatars/${user.uid}/avatar_${System.currentTimeMillis()}.jpg")
        ref.putFile(android.net.Uri.fromFile(file)).await()
        val url = ref.downloadUrl.await().toString()
        // Actualizamos FirebaseAuth + Firestore en paralelo.
        user.updateProfile(
            UserProfileChangeRequest.Builder()
                .setPhotoUri(android.net.Uri.parse(url))
                .build(),
        ).await()
        firestore.collection(USERS).document(user.uid)
            .update("photoUrl", url).await()
        url
    }

    override suspend fun updatePassword(newPassword: String): Result<Unit> = runCatching {
        val user = firebaseAuth.currentUser ?: error("Sin sesion activa")
        require(newPassword.length >= 6) { "La contrasena debe tener al menos 6 caracteres" }
        user.updatePassword(newPassword).await()
    }

    override suspend fun signOut() {
        // Antes de cerrar sesion:
        // 1) Borramos el doc del token en Firestore para que el backend no
        //    siga mandando pushes a este device para este user.
        // 2) Invalidamos el token a nivel FCM con deleteToken(). Si no lo
        //    hacemos, Firebase reciclaria el mismo token al loguear otro
        //    user en este device y se mezclarian destinatarios.
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                unregisterFcmTokenInternal(uid, token)
            }
        }
        runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
        firebaseAuth.signOut()
        runCatching {
            CredentialManager.create(appContext)
                .clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
        }
    }

    override suspend fun registerFcmToken(token: String): Result<Unit> = runCatching {
        val uid = firebaseAuth.currentUser?.uid ?: return@runCatching
        registerFcmTokenInternal(uid, token)
    }

    override suspend fun unregisterFcmToken(token: String): Result<Unit> = runCatching {
        val uid = firebaseAuth.currentUser?.uid ?: return@runCatching
        unregisterFcmTokenInternal(uid, token)
    }

    private suspend fun registerFcmTokenInternal(uid: String, token: String) {
        firestore.collection(USERS).document(uid)
            .collection(FCM_TOKENS).document(token)
            .set(
                mapOf(
                    "token" to token,
                    "platform" to "android",
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
    }

    private suspend fun unregisterFcmTokenInternal(uid: String, token: String) {
        firestore.collection(USERS).document(uid)
            .collection(FCM_TOKENS).document(token)
            .delete().await()
    }

    override suspend fun getProfile(uid: String): Result<UserProfile?> = runCatching {
        val doc = firestore.collection(USERS).document(uid).get().await()
        if (!doc.exists()) null
        else UserProfile(
            uid = uid,
            displayName = doc.getString("displayName"),
            email = doc.getString("email"),
            photoUrl = doc.getString("photoUrl"),
            createdAt = doc.getLong("createdAt") ?: 0L,
            isCollectionPublic = doc.getBoolean("isCollectionPublic") ?: false,
            isAdmin = doc.getBoolean("isAdmin") ?: false,
            karma = doc.getLong("karma") ?: 0L,
            bio = doc.getString("bio"),
            location = doc.getString("location"),
            favoritePlants = (doc.get("favoritePlants") as? List<*>)
                ?.filterIsInstance<String>().orEmpty(),
            badgesPublic = doc.getBoolean("badgesPublic") ?: true,
            diaryPublic = doc.getBoolean("diaryPublic") ?: false,
            notesPublic = doc.getBoolean("notesPublic") ?: false,
            careInfoPublic = doc.getBoolean("careInfoPublic") ?: true,
        )
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
                isCollectionPublic = doc.getBoolean("isCollectionPublic") ?: false,
                isAdmin = doc.getBoolean("isAdmin") ?: false,
                karma = doc.getLong("karma") ?: 0L,
                bio = doc.getString("bio"),
                location = doc.getString("location"),
                favoritePlants = (doc.get("favoritePlants") as? List<*>)
                    ?.filterIsInstance<String>().orEmpty(),
                badgesPublic = doc.getBoolean("badgesPublic") ?: true,
                diaryPublic = doc.getBoolean("diaryPublic") ?: false,
                notesPublic = doc.getBoolean("notesPublic") ?: false,
                careInfoPublic = doc.getBoolean("careInfoPublic") ?: true,
            )
        } else {
            val now = System.currentTimeMillis()
            val data = mapOf(
                "uid" to user.uid,
                "displayName" to user.displayName,
                "displayNameLower" to (user.displayName ?: "").lowercase(),
                "email" to user.email,
                "photoUrl" to user.photoUrl?.toString(),
                "createdAt" to now,
                "isCollectionPublic" to false,
                "isAdmin" to false,
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
        private const val FCM_TOKENS = "fcmTokens"
    }
}
