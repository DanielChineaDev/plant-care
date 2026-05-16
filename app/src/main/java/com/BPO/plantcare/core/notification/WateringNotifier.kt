package com.BPO.plantcare.core.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.BPO.plantcare.MainActivity
import com.BPO.plantcare.R
import com.BPO.plantcare.domain.model.Plant
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WateringNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun showWateringReminder(plants: List<Plant>) {
        if (plants.isEmpty()) return
        if (!hasNotificationPermission()) return

        val title = when (plants.size) {
            1 -> "🌱 Hoy toca regar a ${plants.first().displayName}"
            else -> "🌱 Hoy toca regar ${plants.size} plantas"
        }
        val body = plants.joinToString(separator = "\n") { "• ${it.displayName}" }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(
            context,
            PlantCareNotifications.CHANNEL_ID_WATERING,
        )
            .setSmallIcon(R.drawable.ic_notification_drop)
            .setContentTitle(title)
            .setContentText(plants.joinToString { it.displayName })
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        NotificationManagerCompat.from(context)
            .notify(PlantCareNotifications.NOTIFICATION_ID_WATERING, notification)
    }

    private fun hasNotificationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
}
