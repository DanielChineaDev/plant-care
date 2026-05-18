package com.BPO.plantcare.core.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.BPO.plantcare.MainActivity
import com.BPO.plantcare.R
import com.BPO.plantcare.domain.repository.AuthRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Servicio FCM. Maneja dos cosas:
 *  - onNewToken: re-registra el token cuando Firebase lo regenera (es lo que
 *    pasa cuando el user borra los datos de la app o cambia de dispositivo).
 *  - onMessageReceived: si el push trae data {type:"chat", chatUid:"X"}, lo
 *    transformamos en una notificacion local con deep-link al chat.
 */
@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject lateinit var authRepository: AuthRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch { authRepository.registerFcmToken(token) }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val notif = remoteMessage.notification
        val title = notif?.title ?: data["title"]
        val body = notif?.body ?: data["body"] ?: return
        val type = data["type"] ?: "chat"

        if (type == "chat") {
            val chatUid = data["chatUid"] ?: return
            showChatNotification(
                title = title ?: "Nuevo mensaje",
                body = body,
                chatUid = chatUid,
            )
        }
    }

    private fun showChatNotification(title: String, body: String, chatUid: String) {
        if (!hasNotificationPermission()) return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CHAT_UID, chatUid)
        }
        // requestCode unico por chatUid para que cada chat tenga su propio
        // PendingIntent y no se sobreescriban extras.
        val pending = PendingIntent.getActivity(
            this,
            chatUid.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(
            this,
            PlantCareNotifications.CHANNEL_ID_CHAT,
        )
            .setSmallIcon(R.drawable.ic_notification_drop)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        // ID = hash del chatUid para que mensajes de la misma conversacion
        // colapsen en una sola notif.
        NotificationManagerCompat.from(this).notify(chatUid.hashCode(), notification)
    }

    private fun hasNotificationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val EXTRA_CHAT_UID = "chat_uid"
    }
}
