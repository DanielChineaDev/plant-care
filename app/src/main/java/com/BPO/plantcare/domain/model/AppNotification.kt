package com.BPO.plantcare.domain.model

/**
 * Notificacion in-app que muestra el centro de notificaciones (icono
 * campana en la TopAppBar). Las crea el backend con Cloud Functions; el
 * cliente solo las lee y puede marcarlas como leidas.
 */
data class AppNotification(
    val id: String,
    val type: AppNotificationType,
    val fromUid: String?,
    val fromName: String?,
    val communityId: String?,
    val postId: String?,
    val preview: String?,
    val createdAt: Long,
    val read: Boolean,
)

enum class AppNotificationType(val storageKey: String) {
    PostLike("post_like"),
    PostComment("post_comment"),
    CommunityJoin("community_join"),
    Unknown("unknown");

    companion object {
        fun fromKey(key: String?): AppNotificationType =
            entries.firstOrNull { it.storageKey == key } ?: Unknown
    }
}
