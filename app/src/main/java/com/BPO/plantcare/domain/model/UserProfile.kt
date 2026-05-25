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
    /**
     * Reputacion del usuario: +1 por cada like recibido, -1 al retirarlo.
     * En el futuro tambien -N por reportes confirmados. Lo escribe el
     * Cloud Function, el cliente solo lo lee.
     */
    val karma: Long = 0,
    /** Biografia corta opcional. */
    val bio: String? = null,
    /** Localizacion opcional (texto libre, ej. "Madrid"). */
    val location: String? = null,
    /** Nombres de las plantas favoritas del usuario. */
    val favoritePlants: List<String> = emptyList(),
    /** Si las insignias/logros del usuario son visibles para los demas. */
    val badgesPublic: Boolean = true,
    /**
     * Visibilidad centralizada de la informacion de las plantas en su detalle
     * publico. Todas dependen ademas de [isCollectionPublic] (si la coleccion
     * es privada no se muestra nada).
     */
    val diaryPublic: Boolean = false,
    val notesPublic: Boolean = false,
    val careInfoPublic: Boolean = true,
)
