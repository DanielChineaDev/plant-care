package com.BPO.plantcare.domain.model

/**
 * Perfil del usuario tal y como vive en Firestore (coleccion "users").
 */
data class UserProfile(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val createdAt: Long,
    val isCollectionPublic: Boolean = false,
)
