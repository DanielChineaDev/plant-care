package com.BPO.plantcare.data.repository

import android.net.Uri
import com.BPO.plantcare.domain.model.ChatMessage
import com.BPO.plantcare.domain.model.ChatPresence
import com.BPO.plantcare.domain.model.Conversation
import com.BPO.plantcare.domain.model.conversationIdOf
import com.BPO.plantcare.domain.repository.ChatRepository
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseStorage: FirebaseStorage,
) : ChatRepository {

    private fun currentUid(): String? = firebaseAuth.currentUser?.uid

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMyConversations(): Flow<List<Conversation>> =
        firebaseAuth.uidFlow().flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList()) else conversationsFlow(uid)
        }

    private fun conversationsFlow(uid: String): Flow<List<Conversation>> = callbackFlow {
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
        photoFile: File?,
    ): Result<Unit> = runCatching {
        val me = firebaseAuth.currentUser ?: error("Inicia sesion para enviar mensajes")
        val cid = conversationIdOf(me.uid, otherUid)
        val convRef = firestore.collection(CONVERSATIONS).document(cid)
        val msgRef = convRef.collection(MESSAGES).document()

        // Subimos la foto (si la hay) ANTES de la transaccion.
        val photoUrl: String? = if (photoFile != null) {
            val storageRef = firebaseStorage.reference
                .child("chat_photos/$cid/${msgRef.id}.jpg")
            storageRef.putFile(Uri.fromFile(photoFile)).await()
            storageRef.downloadUrl.await().toString()
        } else null

        // Resumen para la lista de conversaciones (texto o "Foto").
        val preview = when {
            text.isNotBlank() -> text
            photoUrl != null -> "📷 Foto"
            else -> ""
        }

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
                        "lastMessage" to preview,
                        "lastMessageAt" to now,
                        "lastMessageBy" to me.uid,
                    ),
                )
            } else {
                tx.update(
                    convRef,
                    mapOf(
                        "lastMessage" to preview,
                        "lastMessageAt" to now,
                        "lastMessageBy" to me.uid,
                        // Al enviar dejamos de "escribir".
                        "typing.${me.uid}" to 0L,
                    ),
                )
            }
            tx.set(
                msgRef,
                mapOf(
                    "senderUid" to me.uid,
                    "text" to text,
                    "photoUrl" to photoUrl,
                    "reactions" to emptyMap<String, String>(),
                    "createdAt" to now,
                ),
            )
            null
        }.await()
    }

    override fun observePresence(conversationId: String, otherUid: String): Flow<ChatPresence> =
        callbackFlow {
            val reg = firestore.collection(CONVERSATIONS).document(conversationId)
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) {
                        trySend(ChatPresence()); return@addSnapshotListener
                    }
                    @Suppress("UNCHECKED_CAST")
                    val typing = (snap.get("typing") as? Map<String, Any?>).orEmpty()
                    @Suppress("UNCHECKED_CAST")
                    val lastRead = (snap.get("lastReadAt") as? Map<String, Any?>).orEmpty()
                    val typingTs = (typing[otherUid] as? Date)?.time
                        ?: (typing[otherUid] as? Number)?.toLong() ?: 0L
                    val readTs = (lastRead[otherUid] as? Date)?.time
                        ?: (lastRead[otherUid] as? Number)?.toLong() ?: 0L
                    trySend(
                        ChatPresence(
                            otherTyping = System.currentTimeMillis() - typingTs < TYPING_WINDOW_MS,
                            otherLastReadAt = readTs,
                        ),
                    )
                }
            awaitClose { reg.remove() }
        }

    override suspend fun markRead(otherUid: String): Result<Unit> = runCatching {
        val me = currentUid() ?: return@runCatching
        val cid = conversationIdOf(me, otherUid)
        // update falla si la conversacion no existe todavia: lo ignoramos.
        runCatching {
            firestore.collection(CONVERSATIONS).document(cid)
                .update("lastReadAt.$me", FieldValue.serverTimestamp())
                .await()
        }
    }

    override suspend fun setTyping(otherUid: String, typing: Boolean): Result<Unit> = runCatching {
        val me = currentUid() ?: return@runCatching
        val cid = conversationIdOf(me, otherUid)
        val value: Any = if (typing) FieldValue.serverTimestamp() else 0L
        runCatching {
            firestore.collection(CONVERSATIONS).document(cid)
                .update("typing.$me", value)
                .await()
        }
    }

    override suspend fun reactToMessage(
        otherUid: String,
        messageId: String,
        emoji: String?,
    ): Result<Unit> = runCatching {
        val me = currentUid() ?: error("Inicia sesion")
        val cid = conversationIdOf(me, otherUid)
        val msgRef = firestore.collection(CONVERSATIONS).document(cid)
            .collection(MESSAGES).document(messageId)
        val value: Any = emoji ?: FieldValue.delete()
        msgRef.update("reactions.$me", value).await()
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
        @Suppress("UNCHECKED_CAST")
        val reactions = (get("reactions") as? Map<String, Any?>).orEmpty()
            .mapNotNull { (k, v) -> (v as? String)?.let { k to it } }
            .toMap()
        return ChatMessage(
            id = id,
            conversationId = conversationId,
            senderUid = getString("senderUid").orEmpty(),
            text = getString("text").orEmpty(),
            createdAt = (getDate("createdAt") ?: Date(0)).time,
            photoUrl = getString("photoUrl"),
            reactions = reactions,
        )
    }

    companion object {
        private const val CONVERSATIONS = "conversations"
        private const val MESSAGES = "messages"
        private const val TYPING_WINDOW_MS = 6_000L
    }
}
