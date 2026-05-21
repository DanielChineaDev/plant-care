package com.BPO.plantcare.domain.model

/**
 * Reporte de contenido inapropiado (post o comentario) hecho por un user.
 * Lo gestionan los admins desde la pantalla de moderacion.
 */
data class Report(
    val id: String,
    val reporterUid: String,
    val contentType: ReportedContentType,
    val communityId: String,
    val postId: String?,
    val commentId: String?,
    val reason: ReportReason,
    val notes: String?,
    val status: ReportStatus,
    val createdAt: Long,
)

enum class ReportedContentType(val storageKey: String) {
    Post("post"),
    Comment("comment"),
    ;
    companion object {
        fun fromKey(key: String?): ReportedContentType =
            entries.firstOrNull { it.storageKey == key } ?: Post
    }
}

enum class ReportReason(
    val storageKey: String,
    @androidx.annotation.StringRes val labelRes: Int,
) {
    Spam("spam", com.BPO.plantcare.R.string.reason_spam),
    Harassment("harassment", com.BPO.plantcare.R.string.reason_harassment),
    Misinformation("misinformation", com.BPO.plantcare.R.string.reason_misinformation),
    Inappropriate("inappropriate", com.BPO.plantcare.R.string.reason_inappropriate),
    Other("other", com.BPO.plantcare.R.string.reason_other),
    ;
    companion object {
        fun fromKey(key: String?): ReportReason =
            entries.firstOrNull { it.storageKey == key } ?: Other
    }
}

enum class ReportStatus(val storageKey: String) {
    Pending("pending"),
    Dismissed("dismissed"),
    Actioned("actioned"),
    ;
    companion object {
        fun fromKey(key: String?): ReportStatus =
            entries.firstOrNull { it.storageKey == key } ?: Pending
    }
}
