package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.Comment
import com.BPO.plantcare.domain.model.Community
import com.BPO.plantcare.domain.model.CommunityPost
import kotlinx.coroutines.flow.Flow
import java.io.File

interface CommunityRepository {
    fun observeCommunities(): Flow<List<Community>>
    fun observeCommunity(communityId: String): Flow<Community?>
    suspend fun createCommunity(name: String, description: String, emoji: String): Result<String>
    suspend fun joinCommunity(communityId: String): Result<Unit>
    suspend fun leaveCommunity(communityId: String): Result<Unit>

    fun observePosts(communityId: String, limit: Int = 50): Flow<List<CommunityPost>>
    /** Set de IDs de posts likeados por el usuario actual en la comunidad. */
    fun observeLikedPostsInCommunity(communityId: String): Flow<Set<String>>
    /** [photoFile] opcional: si != null se sube a Firebase Storage y se guarda la URL. */
    suspend fun createPost(communityId: String, text: String, photoFile: File? = null): Result<String>
    suspend fun toggleLike(communityId: String, postId: String): Result<Unit>

    fun observePost(communityId: String, postId: String): Flow<CommunityPost?>
    fun observeComments(communityId: String, postId: String): Flow<List<Comment>>
    suspend fun addComment(communityId: String, postId: String, text: String): Result<String>
    suspend fun deleteComment(communityId: String, postId: String, commentId: String): Result<Unit>
}
