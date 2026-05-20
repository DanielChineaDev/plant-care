package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.Comment
import com.BPO.plantcare.domain.model.Community
import com.BPO.plantcare.domain.model.CommunityMember
import com.BPO.plantcare.domain.model.CommunityPost
import com.BPO.plantcare.domain.model.PollOption
import com.BPO.plantcare.domain.model.PostTag
import kotlinx.coroutines.flow.Flow
import java.io.File

interface CommunityRepository {
    fun observeCommunities(): Flow<List<Community>>
    fun observeCommunity(communityId: String): Flow<Community?>
    /** Miembros de la comunidad, ordenados por antiguedad (joinedAt ASC). */
    fun observeMembers(communityId: String): Flow<List<CommunityMember>>
    /** [photoFile] opcional: si != null se sube a Storage y se guarda la URL. */
    suspend fun createCommunity(
        name: String,
        description: String,
        emoji: String,
        photoFile: File? = null,
    ): Result<String>
    /**
     * Edita los metadatos de una comunidad (solo admin, validado por reglas).
     * Si [photoFile] != null se sube y reemplaza la portada.
     */
    suspend fun updateCommunity(
        communityId: String,
        name: String,
        description: String,
        photoFile: File? = null,
    ): Result<Unit>
    /** Expulsa a un miembro (solo admin). Borra su doc de members y decrementa. */
    suspend fun removeMember(communityId: String, memberUid: String): Result<Unit>
    /** Marca/desmarca un post como destacado (solo admin). */
    suspend fun setPostFeatured(
        communityId: String,
        postId: String,
        featured: Boolean,
    ): Result<Unit>
    suspend fun joinCommunity(communityId: String): Result<Unit>
    suspend fun leaveCommunity(communityId: String): Result<Unit>

    fun observePosts(communityId: String, limit: Int = 50): Flow<List<CommunityPost>>
    /** Set de IDs de posts likeados por el usuario actual en la comunidad. */
    fun observeLikedPostsInCommunity(communityId: String): Flow<Set<String>>
    /**
     * Crea un post. Si [pollOptions] no es null, el post sera una encuesta
     * (en ese caso [photoFile] se ignora). Si [photoFile] no es null, se
     * sube a Storage y se guarda la URL.
     */
    suspend fun createPost(
        communityId: String,
        text: String,
        photoFile: File? = null,
        pollOptions: List<PollOption>? = null,
        tag: PostTag? = null,
    ): Result<String>
    suspend fun toggleLike(communityId: String, postId: String): Result<Unit>

    /**
     * Vota una opcion de una encuesta. Si el user ya habia votado otra
     * opcion, transacciona el cambio de voto (-1 a la antigua, +1 a la
     * nueva). Si vuelve a tocar la misma opcion, retira el voto.
     */
    suspend fun voteOnPoll(
        communityId: String,
        postId: String,
        optionId: String,
    ): Result<Unit>

    /** Numero total de posts del usuario en todas las comunidades. */
    suspend fun countUserPosts(uid: String): Result<Int>
    /** Numero total de comentarios del usuario en todas las comunidades. */
    suspend fun countUserComments(uid: String): Result<Int>

    fun observePost(communityId: String, postId: String): Flow<CommunityPost?>
    fun observeComments(communityId: String, postId: String): Flow<List<Comment>>
    suspend fun addComment(communityId: String, postId: String, text: String): Result<String>
    suspend fun deleteComment(communityId: String, postId: String, commentId: String): Result<Unit>
}
