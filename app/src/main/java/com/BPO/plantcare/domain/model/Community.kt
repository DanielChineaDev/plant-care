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
    val createdAt: Long,
)
