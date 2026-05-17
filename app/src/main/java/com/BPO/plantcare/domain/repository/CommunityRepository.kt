package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.Community
import com.BPO.plantcare.domain.model.CommunityPost
import kotlinx.coroutines.flow.Flow

interface CommunityRepository {
    /** Lista de todas las comunidades ordenadas por fecha de creacion DESC. */
    fun observeCommunities(): Flow<List<Community>>

    /** Detalle de una comunidad (incluye flag isMember para el usuario actual). */
    fun observeCommunity(communityId: String): Flow<Community?>

    /** Crea una comunidad nueva. Devuelve el id generado. */
    suspend fun createCommunity(name: String, description: String, emoji: String): Result<String>

    /** Une al usuario actual a la comunidad. Aumenta memberCount atomicamente. */
    suspend fun joinCommunity(communityId: String): Result<Unit>

    /** Saca al usuario actual de la comunidad. Decrementa memberCount. */
    suspend fun leaveCommunity(communityId: String): Result<Unit>

    /** Feed de posts de la comunidad ordenado por createdAt DESC. */
    fun observePosts(communityId: String, limit: Int = 50): Flow<List<CommunityPost>>

    /** Publica un post de texto en la comunidad. */
    suspend fun createPost(communityId: String, text: String): Result<String>
}
