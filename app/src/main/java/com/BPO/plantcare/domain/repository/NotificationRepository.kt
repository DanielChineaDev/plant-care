package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {

    /** Notificaciones del usuario actual, ordenadas por fecha descendente. */
    fun observeMyNotifications(limit: Int = DEFAULT_LIMIT): Flow<List<AppNotification>>

    /** Solo el conteo de no leidas, para badges. */
    fun observeUnreadCount(): Flow<Int>

    suspend fun markAsRead(notificationId: String): Result<Unit>

    suspend fun markAllAsRead(): Result<Unit>

    suspend fun delete(notificationId: String): Result<Unit>

    companion object {
        const val DEFAULT_LIMIT = 100
    }
}
