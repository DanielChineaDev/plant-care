package com.BPO.plantcare.data.repository

import android.net.Uri
import com.BPO.plantcare.domain.model.Comment
import com.BPO.plantcare.domain.model.Community
import com.BPO.plantcare.domain.model.CommunityPost
import com.BPO.plantcare.domain.repository.CommunityRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
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

    override fun observeCommunities(): Flow<List<Community>> {
        val communitiesFlow = communitiesSnapshotFlow()
        val uid = currentUid()
        val membershipFlow = if (uid == null) flowOf(emptySet()) else userMembershipFlow(uid)
        return combine(communitiesFlow, membershipFlow) { list, joined ->
            list.map { it.copy(isMember = it.id in joined) }
        }
    }

    override fun observeCommunity(communityId: String): Flow<Community?> {
        val uid = currentUid()
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
        if (uid == null) return docFlow
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
        return combine(docFlow, memberFlow) { c, isMember -> c?.copy(isMember = isMember) }
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
        // Si trae foto la subimos primero a Storage usando el id del doc
        // como nombre, de forma estable. Igual que con posts.
        val photoUrl: String? = if (photoFile != null) {
            val storageRef = firebaseStorage.reference
                .child("communities/${doc.id}/cover.jpg")
            storageRef.putFile(Uri.fromFile(photoFile)).await()
            storageRef.downloadUrl.await().toString()
        } else null

        val data = mapOf(
            "name" to name,
            "description" to description,
            "emoji" to emoji,
            "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "memberCount" to 0L,
            "photoUrl" to photoUrl,
        )
        doc.set(data).await()
        joinCommunityInternal(uid, doc.id)
        doc.id
    }

    override suspend fun joinCommunity(communityId: String): Result<Unit> = runCatching {
        val uid = requireUid()
        joinCommunityInternal(uid, communityId)
    }

    private suspend fun joinCommunityInternal(uid: String, communityId: String) {
        val communityRef = firestore.collection(COMMUNITIES).document(communityId)
        val memberRef = communityRef.collection(MEMBERS).document(uid)
        val mirrorRef = firestore.collection(USERS).document(uid)
            .collection(JOINED_COMMUNITIES).document(communityId)
        firestore.runTransaction { tx ->
            val existing = tx.get(memberRef)
            if (existing.exists()) return@runTransaction null
            tx.set(memberRef, mapOf("joinedAt" to FieldValue.serverTimestamp()))
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

    override fun observeLikedPostsInCommunity(communityId: String): Flow<Set<String>> {
        val uid = currentUid() ?: return flowOf(emptySet())
        return callbackFlow {
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

    override fun observePost(communityId: String, postId: String): Flow<CommunityPost?> {
        val uid = currentUid()
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
        if (uid == null) return docFlow
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
        return combine(docFlow, likeFlow) { p, liked -> p?.copy(isLikedByMe = liked) }
    }

    override suspend fun createPost(
        communityId: String,
        text: String,
        photoFile: File?,
    ): Result<String> = runCatching {
        val user = firebaseAuth.currentUser ?: error("Inicia sesion para publicar")
        val doc = firestore.collection(COMMUNITIES).document(communityId)
            .collection(POSTS).document()
        // Si hay foto, la subimos PRIMERO a Storage en un path estable
        // construido con el id del doc. Asi si falla la subida no queda un
        // post sin imagen, y si falla el set del doc el archivo queda
        // huerfano (asumible para MVP).
        val photoUrl: String? = if (photoFile != null) {
            val storageRef = firebaseStorage.reference
                .child("community_posts/$communityId/${doc.id}.jpg")
            storageRef.putFile(Uri.fromFile(photoFile)).await()
            storageRef.downloadUrl.await().toString()
        } else null

        val data = mapOf(
            "authorUid" to user.uid,
            "authorName" to (user.displayName ?: ""),
            "authorPhoto" to user.photoUrl?.toString(),
            "text" to text,
            "photoUrl" to photoUrl,
            "createdAt" to FieldValue.serverTimestamp(),
            "likeCount" to 0L,
            "commentCount" to 0L,
        )
        doc.set(data).await()
        doc.id
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
        )
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
    }
}
