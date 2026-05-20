package com.BPO.plantcare.domain.model

/**
 * Conversacion 1-a-1 entre dos usuarios. El id se calcula de forma
 * deterministica concatenando los dos uids ordenados alfabeticamente
 * con "_", para que ambos clientes generen el mismo id sin necesidad
 * de consultar antes.
 */
data class Conversation(
    val id: String,
    val otherUserUid: String,
    val otherUserName: String,
    val otherUserPhoto: String?,
    val lastMessage: String,
    val lastMessageAt: Long,
    val lastMessageBy: String,
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderUid: String,
    val text: String,
    val createdAt: Long,
    /** Foto adjunta opcional (URL en Storage). */
    val photoUrl: String? = null,
    /** Reacciones: uid -> emoji. */
    val reactions: Map<String, String> = emptyMap(),
)

/**
 * "Presencia" del otro usuario en la conversacion: si esta escribiendo
 * ahora mismo y hasta que mensaje ha leido (timestamp).
 */
data class ChatPresence(
    val otherTyping: Boolean = false,
    val otherLastReadAt: Long = 0L,
)

/** Genera el id deterministico de una conversacion entre [uidA] y [uidB]. */
fun conversationIdOf(uidA: String, uidB: String): String =
    listOf(uidA, uidB).sorted().joinToString("_")
