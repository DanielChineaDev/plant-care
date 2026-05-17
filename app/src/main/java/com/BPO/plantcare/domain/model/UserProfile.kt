package com.BPO.plantcare.domain.model

/**
 * Perfil del usuario tal y como vive en Firestore (coleccion "users").
 * Para perfiles publicos compartiremos un subset de estos campos en el futuro.
 */
data class UserProfile(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val createdAt: Long,
)
