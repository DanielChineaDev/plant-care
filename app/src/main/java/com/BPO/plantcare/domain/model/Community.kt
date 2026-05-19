package com.BPO.plantcare.domain.model

data class Community(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val createdBy: String,
    val createdAt: Long,
    val memberCount: Long,
    val isMember: Boolean = false,
    /** Foto de portada opcional almacenada en Storage. */
    val photoUrl: String? = null,
)

data class CommunityPost(
    val id: String,
    val communityId: String,
    val authorUid: String,
    val authorName: String,
    val authorPhoto: String?,
    val text: String,
    val photoUrl: String? = null,
    val createdAt: Long,
    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val isLikedByMe: Boolean = false,
    /**
     * Si esta presente, este post es una encuesta. Cada opcion tiene su
     * id, texto y contador de votos. El total se calcula sumando.
     */
    val poll: Poll? = null,
    /** Id de la opcion votada por el usuario actual, o null si no ha votado. */
    val myPollVote: String? = null,
)

data class Poll(
    val options: List<PollOption>,
    /** Mapa optionId -> votos. Lo mantiene el backend con transacciones. */
    val votesByOption: Map<String, Long>,
) {
    val totalVotes: Long get() = votesByOption.values.sum()
}

data class PollOption(
    val id: String,
    val text: String,
)

data class Comment(
    val id: String,
    val postId: String,
    val authorUid: String,
    val authorName: String,
    val authorPhoto: String?,
    val text: String,
    val createdAt: Long,
)
