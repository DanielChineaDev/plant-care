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
    /**
     * Solo los admins pueden crear comunidades. Por defecto false; se cambia
     * manualmente desde Firebase Console -> Firestore -> users/{uid} ->
     * isAdmin = true.
     */
    val isAdmin: Boolean = false,
)
