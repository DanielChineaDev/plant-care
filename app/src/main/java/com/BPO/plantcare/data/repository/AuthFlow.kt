package com.BPO.plantcare.data.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Emite el uid actual cada vez que cambia el AuthState. null cuando hay
 * sesion cerrada. Sirve para que los repositorios reaccionen a login/logout
 * sin cachear el uid al construir un Flow (lo que causa que la query se
 * quede "fria" cuando el user cambia y el subscriber se reactiva).
 *
 * Uso tipico:
 *   firebaseAuth.uidFlow().flatMapLatest { uid ->
 *       if (uid == null) flowOf(empty) else queryFor(uid)
 *   }
 */
fun FirebaseAuth.uidFlow(): Flow<String?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser?.uid)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}.distinctUntilChanged()
