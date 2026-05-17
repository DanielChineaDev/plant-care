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
