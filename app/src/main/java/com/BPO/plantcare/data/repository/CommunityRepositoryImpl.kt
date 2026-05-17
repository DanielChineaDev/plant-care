package com.BPO.plantcare.data.repository

import com.BPO.plantcare.domain.model.Community
import com.BPO.plantcare.domain.model.CommunityPost
import com.BPO.plantcare.domain.repository.CommunityRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) : CommunityRepository {

    private fun currentUid(): String? = firebaseAuth.currentUser?.uid

    // ---- Comunidades ----

    override fun observeCommunities(): Flow<List<Community>> {
        val communitiesFlow = communitiesSnapshotFlow()
        val uid = currentUid()
        val membershipFlow = if (uid == null) flowOf(emptySet())
        else userMembershipFlow(uid)
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
        return combine(docFlow, memberFlow) { c, isMember ->
            c?.copy(isMember = isMember)
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
        // collectionGroup("members") con filtro por user no se puede sin indice
        // compuesto; en su lugar usamos un mirror per-user en users/{uid}/joinedCommunities.
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
    ): Result<String> = runCatching {
        val uid = requireUid()
        val doc = firestore.collection(COMMUNITIES).document()
        val data = mapOf(
            "name" to name,
            "description" to description,
            "emoji" to emoji,
            "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "memberCount" to 0L,
        )
        doc.set(data).await()
        // El creador se une automaticamente.
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

    override suspend fun createPost(communityId: String, text: String): Result<String> =
        runCatching {
            val user = firebaseAuth.currentUser ?: error("Inicia sesion para publicar")
            val doc = firestore.collection(COMMUNITIES).document(communityId)
                .collection(POSTS).document()
            val data = mapOf(
                "authorUid" to user.uid,
                "authorName" to (user.displayName ?: ""),
                "authorPhoto" to user.photoUrl?.toString(),
                "text" to text,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            doc.set(data).await()
            doc.id
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
            createdAt = (getDate("createdAt") ?: Date(0)).time,
        )
    }

    companion object {
        private const val COMMUNITIES = "communities"
        private const val MEMBERS = "members"
        private const val POSTS = "posts"
        private const val USERS = "users"
        private const val JOINED_COMMUNITIES = "joinedCommunities"
    }
}
