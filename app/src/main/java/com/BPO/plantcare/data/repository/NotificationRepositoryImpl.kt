package com.BPO.plantcare.data.repository

import com.BPO.plantcare.domain.model.AppNotification
import com.BPO.plantcare.domain.model.AppNotificationType
import com.BPO.plantcare.domain.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) : NotificationRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMyNotifications(limit: Int): Flow<List<AppNotification>> =
        firebaseAuth.uidFlow().flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList())
            else notificationsFlow(uid, limit)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeUnreadCount(): Flow<Int> =
        observeMyNotifications().map { list -> list.count { !it.read } }

    override suspend fun markAsRead(notificationId: String): Result<Unit> = runCatching {
        val uid = firebaseAuth.currentUser?.uid ?: return@runCatching
        firestore.collection(NOTIFICATIONS).document(uid)
            .collection(ITEMS).document(notificationId)
            .update("read", true).await()
    }

    override suspend fun markAllAsRead(): Result<Unit> = runCatching {
        val uid = firebaseAuth.currentUser?.uid ?: return@runCatching
        val unread = firestore.collection(NOTIFICATIONS).document(uid)
            .collection(ITEMS)
            .whereEqualTo("read", false)
            .get().await()
        // Batches de 500. Mientras la coleccion sea < 500 lo hacemos en un
        // solo commit; suficiente para v1.
        if (unread.isEmpty) return@runCatching
        val batch = firestore.batch()
        unread.documents.forEach { batch.update(it.reference, "read", true) }
        batch.commit().await()
    }

    override suspend fun delete(notificationId: String): Result<Unit> = runCatching {
        val uid = firebaseAuth.currentUser?.uid ?: return@runCatching
        firestore.collection(NOTIFICATIONS).document(uid)
            .collection(ITEMS).document(notificationId)
            .delete().await()
    }

    private fun notificationsFlow(uid: String, limit: Int): Flow<List<AppNotification>> =
        callbackFlow {
            val reg = firestore.collection(NOTIFICATIONS).document(uid)
                .collection(ITEMS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        trySend(emptyList()); return@addSnapshotListener
                    }
                    val list = snap?.documents
                        ?.mapNotNull { it.toAppNotification() }
                        .orEmpty()
                    trySend(list)
                }
            awaitClose { reg.remove() }
        }

    private fun DocumentSnapshot.toAppNotification(): AppNotification? {
        if (!exists()) return null
        return AppNotification(
            id = id,
            type = AppNotificationType.fromKey(getString("type")),
            fromUid = getString("fromUid"),
            fromName = getString("fromName"),
            communityId = getString("communityId"),
            postId = getString("postId"),
            preview = getString("preview"),
            createdAt = (getDate("createdAt") ?: Date(0)).time,
            read = getBoolean("read") ?: false,
        )
    }

    companion object {
        private const val NOTIFICATIONS = "notifications"
        private const val ITEMS = "items"
    }
}
