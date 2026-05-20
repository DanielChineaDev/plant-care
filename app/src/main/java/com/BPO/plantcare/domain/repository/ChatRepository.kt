package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.ChatMessage
import com.BPO.plantcare.domain.model.ChatPresence
import com.BPO.plantcare.domain.model.Conversation
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ChatRepository {
    /** Conversaciones del usuario actual, ordenadas por lastMessageAt DESC. */
    fun observeMyConversations(): Flow<List<Conversation>>

    /** Mensajes de una conversacion ordenados por createdAt ASC. */
    fun observeMessages(conversationId: String, limit: Int = 100): Flow<List<ChatMessage>>

    /** Presencia del otro usuario: si escribe y hasta donde ha leido. */
    fun observePresence(conversationId: String, otherUid: String): Flow<ChatPresence>

    /**
     * Envia un mensaje al otro usuario. Si la conversacion no existia, la crea
     * con sus metadatos (nombres y fotos para que la lista pueda renderizar
     * sin tener que pedir cada perfil aparte). Si [photoFile] != null se sube
     * a Storage y se adjunta la URL.
     */
    suspend fun sendMessage(
        otherUid: String,
        otherName: String,
        otherPhoto: String?,
        text: String,
        photoFile: File? = null,
    ): Result<Unit>

    /** Marca la conversacion como leida hasta ahora por el usuario actual. */
    suspend fun markRead(otherUid: String): Result<Unit>

    /** Actualiza el flag "escribiendo" del usuario actual. */
    suspend fun setTyping(otherUid: String, typing: Boolean): Result<Unit>

    /** Anade/cambia/quita la reaccion del usuario actual a un mensaje. */
    suspend fun reactToMessage(
        otherUid: String,
        messageId: String,
        emoji: String?,
    ): Result<Unit>
}
