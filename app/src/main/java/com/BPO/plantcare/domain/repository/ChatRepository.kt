package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.ChatMessage
import com.BPO.plantcare.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    /** Conversaciones del usuario actual, ordenadas por lastMessageAt DESC. */
    fun observeMyConversations(): Flow<List<Conversation>>

    /** Mensajes de una conversacion ordenados por createdAt ASC. */
    fun observeMessages(conversationId: String, limit: Int = 100): Flow<List<ChatMessage>>

    /**
     * Envia un mensaje al otro usuario. Si la conversacion no existia, la crea
     * con sus metadatos (nombres y fotos para que la lista pueda renderizar
     * sin tener que pedir cada perfil aparte).
     */
    suspend fun sendMessage(
        otherUid: String,
        otherName: String,
        otherPhoto: String?,
        text: String,
    ): Result<Unit>
}
