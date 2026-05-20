package com.BPO.plantcare.data.repository

import android.net.Uri
import com.BPO.plantcare.domain.model.Comment
import com.BPO.plantcare.domain.model.Community
import com.BPO.plantcare.domain.model.CommunityMember
import com.BPO.plantcare.domain.model.CommunityPost
import com.BPO.plantcare.domain.model.PostTag
import com.BPO.plantcare.domain.repository.CommunityRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseStorage: FirebaseStorage,
) : CommunityRepository {

    private fun currentUid(): String? = firebaseAuth.currentUser?.uid

    // ---- Comunidades ----

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCommunities(): Flow<List<Community>> =
        firebaseAuth.uidFlow().flatMapLatest { uid ->
            val communitiesFlow = communitiesSnapshotFlow()
            val membershipFlow =
                if (uid == null) flowOf(emptySet()) else userMembershipFlow(uid)
            combine(communitiesFlow, membershipFlow) { list, joined ->
                list.map { it.copy(isMember = it.id in joined) }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCommunity(communityId: String): Flow<Community?> =
        firebaseAuth.uidFlow().flatMapLatest { uid ->
            val docFlow: Flow<Community?> = callbackFlow {
                val reg = firestore.collection(COMMUNITIES).document(communityId)
                    .addSnapshotListener { snap, err ->
                        if (err != null) {
                            trySend(null); return@addSnapshotListener
                        }
                        trySend(snap?.toCommunity())
                    }
                awaitClose { reg.remove() }
            }
            if (uid == null) {
                docFlow
            } else {
                val memberFlow: Flow<Boolean> = callbackFlow {
                    val reg = firestore.collection(COMMUNITIES).document(communityId)
                        .collection(MEMBERS).document(uid)
                        .addSnapshotListener { snap, err ->
                            if (err != null) {
                                trySend(false); return@addSnapshotListener
                            }
                            trySend(snap?.exists() == true)
                        }
                    awaitClose { reg.remove() }
                }
                combine(docFlow, memberFlow) { c, isMember -> c?.copy(isMember = isMember) }
            }
        }

    private fun communitiesSnapshotFlow(): Flow<List<Community>> = callbackFlow {
        val reg = firestore.collection(COMMUNITIES)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList()); return@addSnapshotListener
                }
                trySend(snap?.documents?.mapNotNull { it.toCommunity() } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    private fun userMembershipFlow(uid: String): Flow<Set<String>> = callbackFlow {
        val reg = firestore.collection(USERS).document(uid)
            .collection(JOINED_COMMUNITIES)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptySet()); return@addSnapshotListener
                }
                trySend(snap?.documents?.map { it.id }?.toSet() ?: emptySet())
            }
        awaitClose { reg.remove() }
    }

    override suspend fun createCommunity(
        name: String,
        description: String,
        emoji: String,
        photoFile: File?,
    ): Result<String> = runCatching {
        val uid = requireUid()
        val doc = firestore.collection(COMMUNITIES).document()
        val storageRef = if (photoFile != null) {
            firebaseStorage.reference.child("communities/${doc.id}/cover.jpg")
        } else null

        val photoUrl: String? = if (photoFile != null && storageRef != null) {
            storageRef.putFile(Uri.fromFile(photoFile)).await()
            storageRef.downloadUrl.await().toString()
        } else null

        val data = mapOf(
            "name" to name,
            // Para busqueda global case-insensitive: guardamos el nombre en
            // minusculas en un campo aparte que indexa Firestore por defecto.
            "nameLower" to name.lowercase(),
            "description" to description,
            "emoji" to emoji,
            "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "memberCount" to 0L,
            "photoUrl" to photoUrl,
        )
        // Si la escritura del doc falla (p.ej. reglas rechazan porque el user
        // no es admin), borramos el blob ya subido para no dejar huerfano.
        try {
            doc.set(data).await()
        } catch (e: Exception) {
            if (storageRef != null) runCatching { storageRef.delete().await() }
            throw e
        }
        joinCommunityInternal(uid, doc.id)
        doc.id
    }

    override suspend fun joinCommunity(communityId: String): Result<Unit> = runCatching {
        val uid = requireUid()
        joinCommunityInternal(uid, communityId)
    }

    private suspend fun joinCommunityInternal(uid: String, communityId: String) {
        val user = firebaseAuth.currentUser
        val communityRef = firestore.collection(COMMUNITIES).document(communityId)
        val memberRef = communityRef.collection(MEMBERS).document(uid)
        val mirrorRef = firestore.collection(USERS).document(uid)
            .collection(JOINED_COMMUNITIES).document(communityId)
        firestore.runTransaction { tx ->
            val existing = tx.get(memberRef)
            if (existing.exists()) return@runTransaction null
            // Denormalizamos nombre/foto en el doc del miembro para poder
            // listar miembros sin pedir cada perfil por separado.
            tx.set(
                memberRef,
                mapOf(
                    "joinedAt" to FieldValue.serverTimestamp(),
                    "name" to (user?.displayName ?: ""),
                    "photoUrl" to user?.photoUrl?.toString(),
                ),
            )
            tx.set(mirrorRef, mapOf("joinedAt" to FieldValue.serverTimestamp()))
            tx.update(communityRef, "memberCount", FieldValue.increment(1))
            null
        }.await()
    }

    override suspend fun leaveCommunity(communityId: String): Result<Unit> = runCatching {
        val uid = requireUid()
        val communityRef = firestore.collection(COMMUNITIES).document(communityId)
        val memberRef = communityRef.collection(MEMBERS).document(uid)
        val mirrorRef = firestore.collection(USERS).document(uid)
            .collection(JOINED_COMMUNITIES).document(communityId)
        firestore.runTransaction { tx ->
            val existing = tx.get(memberRef)
            if (!existing.exists()) return@runTransaction null
            tx.delete(memberRef)
            tx.delete(mirrorRef)
            tx.update(communityRef, "memberCount", FieldValue.increment(-1))
            null
        }.await()
    }

    override fun observeMembers(communityId: String): Flow<List<CommunityMember>> = callbackFlow {
        // Necesitamos el createdBy de la comunidad para marcar al fundador.
        val communityRef = firestore.collection(COMMUNITIES).document(communityId)
        var createdBy: String? = null
        val communityReg = communityRef.addSnapshotListener { snap, _ ->
            createdBy = snap?.getString("createdBy")
        }
        val membersReg = communityRef.collection(MEMBERS)
            .orderBy("joinedAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList()); return@addSnapshotListener
                }
                val list = snap?.documents?.map { doc ->
                    CommunityMember(
                        uid = doc.id,
                        name = doc.getString("name").orEmpty(),
                        photoUrl = doc.getString("photoUrl"),
                        joinedAt = (doc.getDate("joinedAt") ?: Date(0)).time,
                        isCreator = doc.id == createdBy,
                    )
                }.orEmpty()
                trySend(list)
            }
        awaitClose {
            communityReg.remove()
            membersReg.remove()
        }
    }

    override suspend fun updateCommunity(
        communityId: String,
        name: String,
        description: String,
        photoFile: File?,
    ): Result<Unit> = runCatching {
        requireUid()
        val docRef = firestore.collection(COMMUNITIES).document(communityId)
        val photoUrl: String? = if (photoFile != null) {
            val storageRef = firebaseStorage.reference.child("communities/$communityId/cover.jpg")
            storageRef.putFile(Uri.fromFile(photoFile)).await()
            storageRef.downloadUrl.await().toString()
        } else null

        val data = mutableMapOf<String, Any?>(
            "name" to name,
            "nameLower" to name.lowercase(),
            "description" to description,
        )
        if (photoUrl != null) data["photoUrl"] = photoUrl
        docRef.update(data).await()
    }

    override suspend fun removeMember(
        communityId: String,
        memberUid: String,
    ): Result<Unit> = runCatching {
        requireUid()
        val communityRef = firestore.collection(COMMUNITIES).document(communityId)
        val memberRef = communityRef.collection(MEMBERS).document(memberUid)
        firestore.runTransaction { tx ->
            val existing = tx.get(memberRef)
            if (!existing.exists()) return@runTransaction null
            tx.delete(memberRef)
            tx.update(communityRef, "memberCount", FieldValue.increment(-1))
            null
        }.await()
    }

    override suspend fun setPostFeatured(
        communityId: String,
        postId: String,
        featured: Boolean,
    ): Result<Unit> = runCatching {
        requireUid()
        firestore.collection(COMMUNITIES).document(communityId)
            .collection(POSTS).document(postId)
            .update("featured", featured)
            .await()
    }

    // ---- Posts ----

    override fun observePosts(communityId: String, limit: Int): Flow<List<CommunityPost>> =
        callbackFlow {
            val reg = firestore.collection(COMMUNITIES).document(communityId)
                .collection(POSTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        trySend(emptyList()); return@addSnapshotListener
                    }
                    val posts = snap?.documents?.mapNotNull { it.toPost(communityId) }.orEmpty()
                    trySend(posts)
                }
            awaitClose { reg.remove() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeLikedPostsInCommunity(communityId: String): Flow<Set<String>> =
        firebaseAuth.uidFlow().flatMapLatest { uid ->
            if (uid == null) {
                flowOf(emptySet())
            } else {
                callbackFlow {
                    val reg = firestore.collection(USERS).document(uid)
                        .collection(POST_LIKES)
                        .whereEqualTo("communityId", communityId)
                        .addSnapshotListener { snap, err ->
                            if (err != null) {
                                trySend(emptySet()); return@addSnapshotListener
                            }
                            trySend(snap?.documents?.map { it.id }?.toSet() ?: emptySet())
                        }
                    awaitClose { reg.remove() }
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observePost(communityId: String, postId: String): Flow<CommunityPost?> =
        firebaseAuth.uidFlow().flatMapLatest { uid ->
            val docFlow: Flow<CommunityPost?> = callbackFlow {
                val reg = firestore.collection(COMMUNITIES).document(communityId)
                    .collection(POSTS).document(postId)
                    .addSnapshotListener { snap, err ->
                        if (err != null) {
                            trySend(null); return@addSnapshotListener
                        }
                        trySend(snap?.toPost(communityId))
                    }
                awaitClose { reg.remove() }
            }
            if (uid == null) {
                docFlow
            } else {
                val likeFlow: Flow<Boolean> = callbackFlow {
                    val reg = firestore.collection(COMMUNITIES).document(communityId)
                        .collection(POSTS).document(postId)
                        .collection(LIKES).document(uid)
                        .addSnapshotListener { snap, err ->
                            if (err != null) {
                                trySend(false); return@addSnapshotListener
                            }
                            trySend(snap?.exists() == true)
                        }
                    awaitClose { reg.remove() }
                }
                val pollVoteFlow: Flow<String?> = callbackFlow {
                    val reg = firestore.collection(COMMUNITIES).document(communityId)
                        .collection(POSTS).document(postId)
                        .collection(POLL_VOTES).document(uid)
                        .addSnapshotListener { snap, err ->
                            if (err != null) {
                                trySend(null); return@addSnapshotListener
                            }
                            trySend(snap?.getString("optionId"))
                        }
                    awaitClose { reg.remove() }
                }
                combine(docFlow, likeFlow, pollVoteFlow) { p, liked, vote ->
                    p?.copy(isLikedByMe = liked, myPollVote = vote)
                }
            }
        }

    override suspend fun createPost(
        communityId: String,
        text: String,
        photoFile: File?,
        pollOptions: List<com.BPO.plantcare.domain.model.PollOption>?,
        tag: PostTag?,
    ): Result<String> = runCatching {
        val user = firebaseAuth.currentUser ?: error("Inicia sesion para publicar")
        val doc = firestore.collection(COMMUNITIES).document(communityId)
            .collection(POSTS).document()

        // Si es encuesta, la foto se ignora (la UI tampoco la pide).
        val isPoll = pollOptions != null && pollOptions.size >= 2
        val storageRef = if (!isPoll && photoFile != null) {
            firebaseStorage.reference.child("community_posts/$communityId/${doc.id}.jpg")
        } else null
        val photoUrl: String? = if (!isPoll && photoFile != null && storageRef != null) {
            storageRef.putFile(Uri.fromFile(photoFile)).await()
            storageRef.downloadUrl.await().toString()
        } else null

        val baseData = mutableMapOf<String, Any?>(
            "authorUid" to user.uid,
            "authorName" to (user.displayName ?: ""),
            "authorPhoto" to user.photoUrl?.toString(),
            "text" to text,
            "photoUrl" to photoUrl,
            "createdAt" to FieldValue.serverTimestamp(),
            "likeCount" to 0L,
            "commentCount" to 0L,
            "featured" to false,
            "tag" to tag?.key,
        )
        if (isPoll) {
            baseData["pollOptions"] = pollOptions!!.map {
                mapOf("id" to it.id, "text" to it.text)
            }
            // Contadores arrancan a 0 para cada opcion.
            baseData["pollVotesCount"] = pollOptions.associate { it.id to 0L }
        }
        try {
            doc.set(baseData).await()
        } catch (e: Exception) {
            if (storageRef != null) runCatching { storageRef.delete().await() }
            throw e
        }
        doc.id
    }

    override suspend fun voteOnPoll(
        communityId: String,
        postId: String,
        optionId: String,
    ): Result<Unit> = runCatching {
        val uid = requireUid()
        val postRef = firestore.collection(COMMUNITIES).document(communityId)
            .collection(POSTS).document(postId)
        val voteRef = postRef.collection(POLL_VOTES).document(uid)

        firestore.runTransaction { tx ->
            val voteSnap = tx.get(voteRef)
            val postSnap = tx.get(postRef)
            @Suppress("UNCHECKED_CAST")
            val counts = (postSnap.get("pollVotesCount") as? Map<String, Number>)
                ?: error("Post no es encuesta")
            // Validamos que la opcion exista para no inflar contadores fantasma.
            if (!counts.containsKey(optionId)) error("Opcion invalida")

            val previousOption = voteSnap.getString("optionId")
            when {
                previousOption == null -> {
                    // Voto nuevo.
                    tx.set(
                        voteRef,
                        mapOf(
                            "optionId" to optionId,
                            "votedAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                    tx.update(postRef, "pollVotesCount.$optionId", FieldValue.increment(1))
                }
                previousOption == optionId -> {
                    // Volver a tocar la misma opcion = retirar voto.
                    tx.delete(voteRef)
                    tx.update(postRef, "pollVotesCount.$optionId", FieldValue.increment(-1))
                }
                else -> {
                    // Cambio de voto.
                    tx.update(voteRef, "optionId", optionId)
                    tx.update(voteRef, "votedAt", FieldValue.serverTimestamp())
                    tx.update(
                        postRef,
                        mapOf(
                            "pollVotesCount.$previousOption" to FieldValue.increment(-1),
                            "pollVotesCount.$optionId" to FieldValue.increment(1),
                        ),
                    )
                }
            }
            null
        }.await()
    }

    override suspend fun toggleLike(communityId: String, postId: String): Result<Unit> =
        runCatching {
            val uid = requireUid()
            val postRef = firestore.collection(COMMUNITIES).document(communityId)
                .collection(POSTS).document(postId)
            val likeRef = postRef.collection(LIKES).document(uid)
            val mirrorRef = firestore.collection(USERS).document(uid)
                .collection(POST_LIKES).document(postId)
            firestore.runTransaction { tx ->
                val existing = tx.get(likeRef)
                if (existing.exists()) {
                    tx.delete(likeRef)
                    tx.delete(mirrorRef)
                    tx.update(postRef, "likeCount", FieldValue.increment(-1))
                } else {
                    tx.set(likeRef, mapOf("likedAt" to FieldValue.serverTimestamp()))
                    tx.set(
                        mirrorRef,
                        mapOf(
                            "communityId" to communityId,
                            "likedAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                    tx.update(postRef, "likeCount", FieldValue.increment(1))
                }
                null
            }.await()
        }

    // ---- Comments ----

    override fun observeComments(communityId: String, postId: String): Flow<List<Comment>> =
        callbackFlow {
            val reg = firestore.collection(COMMUNITIES).document(communityId)
                .collection(POSTS).document(postId)
                .collection(COMMENTS)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        trySend(emptyList()); return@addSnapshotListener
                    }
                    val comments = snap?.documents?.mapNotNull { it.toComment(postId) }.orEmpty()
                    trySend(comments)
                }
            awaitClose { reg.remove() }
        }

    override suspend fun addComment(
        communityId: String,
        postId: String,
        text: String,
    ): Result<String> = runCatching {
        val user = firebaseAuth.currentUser ?: error("Inicia sesion para comentar")
        val postRef = firestore.collection(COMMUNITIES).document(communityId)
            .collection(POSTS).document(postId)
        val commentRef = postRef.collection(COMMENTS).document()
        firestore.runTransaction { tx ->
            tx.set(
                commentRef,
                mapOf(
                    "authorUid" to user.uid,
                    "authorName" to (user.displayName ?: ""),
                    "authorPhoto" to user.photoUrl?.toString(),
                    "text" to text,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            )
            tx.update(postRef, "commentCount", FieldValue.increment(1))
            null
        }.await()
        commentRef.id
    }

    override suspend fun deleteComment(
        communityId: String,
        postId: String,
        commentId: String,
    ): Result<Unit> = runCatching {
        val uid = requireUid()
        val postRef = firestore.collection(COMMUNITIES).document(communityId)
            .collection(POSTS).document(postId)
        val commentRef = postRef.collection(COMMENTS).document(commentId)
        firestore.runTransaction { tx ->
            val snap = tx.get(commentRef)
            if (!snap.exists()) return@runTransaction null
            val author = snap.getString("authorUid")
            if (author != uid) error("Solo el autor puede borrar el comentario")
            tx.delete(commentRef)
            tx.update(postRef, "commentCount", FieldValue.increment(-1))
            null
        }.await()
    }

    // ---- Helpers ----

    private fun requireUid(): String =
        currentUid() ?: error("Inicia sesion para hacer esta accion")

    private fun DocumentSnapshot.toCommunity(): Community? {
        if (!exists()) return null
        return Community(
            id = id,
            name = getString("name").orEmpty(),
            description = getString("description").orEmpty(),
            emoji = getString("emoji") ?: "🌱",
            createdBy = getString("createdBy").orEmpty(),
            createdAt = getDate("createdAt")?.time ?: 0L,
            memberCount = getLong("memberCount") ?: 0L,
            photoUrl = getString("photoUrl"),
        )
    }

    private fun DocumentSnapshot.toPost(communityId: String): CommunityPost? {
        if (!exists()) return null
        return CommunityPost(
            id = id,
            communityId = communityId,
            authorUid = getString("authorUid").orEmpty(),
            authorName = getString("authorName").orEmpty(),
            authorPhoto = getString("authorPhoto"),
            text = getString("text").orEmpty(),
            photoUrl = getString("photoUrl"),
            createdAt = (getDate("createdAt") ?: Date(0)).time,
            likeCount = getLong("likeCount") ?: 0L,
            commentCount = getLong("commentCount") ?: 0L,
            poll = parsePoll(),
            tag = PostTag.fromKey(getString("tag")),
            featured = getBoolean("featured") ?: false,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.parsePoll(): com.BPO.plantcare.domain.model.Poll? {
        val rawOptions = get("pollOptions") as? List<Map<String, Any?>> ?: return null
        if (rawOptions.size < 2) return null
        val options = rawOptions.mapNotNull { o ->
            val id = (o["id"] as? String) ?: return@mapNotNull null
            val text = (o["text"] as? String) ?: return@mapNotNull null
            com.BPO.plantcare.domain.model.PollOption(id, text)
        }
        val rawCounts = (get("pollVotesCount") as? Map<String, Any?>).orEmpty()
        val votes = rawCounts.mapValues { (_, v) ->
            (v as? Number)?.toLong() ?: 0L
        }
        return com.BPO.plantcare.domain.model.Poll(options, votes)
    }

    private fun DocumentSnapshot.toComment(postId: String): Comment? {
        if (!exists()) return null
        return Comment(
            id = id,
            postId = postId,
            authorUid = getString("authorUid").orEmpty(),
            authorName = getString("authorName").orEmpty(),
            authorPhoto = getString("authorPhoto"),
            text = getString("text").orEmpty(),
            createdAt = (getDate("createdAt") ?: Date(0)).time,
        )
    }

    companion object {
        private const val COMMUNITIES = "communities"
        private const val MEMBERS = "members"
        private const val POSTS = "posts"
        private const val LIKES = "likes"
        private const val COMMENTS = "comments"
        private const val USERS = "users"
        private const val JOINED_COMMUNITIES = "joinedCommunities"
        private const val POST_LIKES = "postLikes"
        private const val POLL_VOTES = "pollVotes"
    }
}
