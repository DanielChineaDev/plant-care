package com.BPO.plantcare.data.repository

import com.BPO.plantcare.domain.model.ChatMessage
import com.BPO.plantcare.domain.model.Conversation
import com.BPO.plantcare.domain.model.conversationIdOf
import com.BPO.plantcare.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) : ChatRepository {

    private fun currentUid(): String? = firebaseAuth.currentUser?.uid

    override fun observeMyConversations(): Flow<List<Conversation>> {
        val uid = currentUid() ?: return flowOf(emptyList())
        return callbackFlow {
            val reg = firestore.collection(CONVERSATIONS)
                .whereArrayContains("participants", uid)
                .orderBy("lastMessageAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        trySend(emptyList()); return@addSnapshotListener
                    }
                    val list = snap?.documents
                        ?.mapNotNull { it.toConversation(uid) }
                        .orEmpty()
                    trySend(list)
                }
            awaitClose { reg.remove() }
        }
    }

    override fun observeMessages(conversationId: String, limit: Int): Flow<List<ChatMessage>> =
        callbackFlow {
            val reg = firestore.collection(CONVERSATIONS).document(conversationId)
                .collection(MESSAGES)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limit(limit.toLong())
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        trySend(emptyList()); return@addSnapshotListener
                    }
                    val list = snap?.documents
                        ?.mapNotNull { it.toMessage(conversationId) }
                        .orEmpty()
                    trySend(list)
                }
            awaitClose { reg.remove() }
        }

    override suspend fun sendMessage(
        otherUid: String,
        otherName: String,
        otherPhoto: String?,
        text: String,
    ): Result<Unit> = runCatching {
        val me = firebaseAuth.currentUser ?: error("Inicia sesion para enviar mensajes")
        val cid = conversationIdOf(me.uid, otherUid)
        val convRef = firestore.collection(CONVERSATIONS).document(cid)
        val msgRef = convRef.collection(MESSAGES).document()

        // Una transaccion: crea/actualiza la conversacion + inserta el mensaje.
        firestore.runTransaction { tx ->
            val snap = tx.get(convRef)
            val now = FieldValue.serverTimestamp()
            val myName = me.displayName ?: ""
            val myPhoto = me.photoUrl?.toString()

            if (!snap.exists()) {
                // Primera vez: crear la conversacion con los metadatos de ambos.
                tx.set(
                    convRef,
                    mapOf(
                        "participants" to listOf(me.uid, otherUid),
                        "participantNames" to mapOf(me.uid to myName, otherUid to otherName),
                        "participantPhotos" to mapOf(
                            me.uid to myPhoto,
                            otherUid to otherPhoto,
                        ),
                        "lastMessage" to text,
                        "lastMessageAt" to now,
                        "lastMessageBy" to me.uid,
                    ),
                )
            } else {
                tx.update(
                    convRef,
                    mapOf(
                        "lastMessage" to text,
                        "lastMessageAt" to now,
                        "lastMessageBy" to me.uid,
                    ),
                )
            }
            tx.set(
                msgRef,
                mapOf(
                    "senderUid" to me.uid,
                    "text" to text,
                    "createdAt" to now,
                ),
            )
            null
        }.await()
    }

    private fun DocumentSnapshot.toConversation(myUid: String): Conversation? {
        if (!exists()) return null
        val participants = (get("participants") as? List<*>)?.filterIsInstance<String>()
            ?: return null
        val otherUid = participants.firstOrNull { it != myUid } ?: return null
        @Suppress("UNCHECKED_CAST")
        val names = (get("participantNames") as? Map<String, String>).orEmpty()
        @Suppress("UNCHECKED_CAST")
        val photos = (get("participantPhotos") as? Map<String, String?>).orEmpty()
        return Conversation(
            id = id,
            otherUserUid = otherUid,
            otherUserName = names[otherUid].orEmpty(),
            otherUserPhoto = photos[otherUid],
            lastMessage = getString("lastMessage").orEmpty(),
            lastMessageAt = (getDate("lastMessageAt") ?: Date(0)).time,
            lastMessageBy = getString("lastMessageBy").orEmpty(),
        )
    }

    private fun DocumentSnapshot.toMessage(conversationId: String): ChatMessage? {
        if (!exists()) return null
        return ChatMessage(
            id = id,
            conversationId = conversationId,
            senderUid = getString("senderUid").orEmpty(),
            text = getString("text").orEmpty(),
            createdAt = (getDate("createdAt") ?: Date(0)).time,
        )
    }

    companion object {
        private const val CONVERSATIONS = "conversations"
        private const val MESSAGES = "messages"
    }
}
