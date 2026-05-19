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
import com.BPO.plantcare.domain.model.PlantTask
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Construye y muestra notificaciones de tareas de cuidado. Una notif por
 * tarea (no agregamos como en riego) porque cada una lleva acciones
 * propias de snooze + marcar hecho.
 */
@Singleton
class PlantTaskNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun showTaskReminder(task: PlantTask, plant: Plant) {
        if (!hasNotificationPermission()) return

        val notificationId = notifIdFor(task.id)
        val title = "${task.type.emoji} ${task.type.label}: ${plant.displayName}"

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val doneAction = buildAction(
            action = TaskActionReceiver.ACTION_DONE,
            taskId = task.id,
            notificationId = notificationId,
            label = "Hecho",
            extras = null,
        )
        val snooze1h = buildAction(
            action = TaskActionReceiver.ACTION_SNOOZE,
            taskId = task.id,
            notificationId = notificationId,
            label = "+1h",
            extras = SNOOZE_60,
        )
        val snoozeTomorrow = buildAction(
            action = TaskActionReceiver.ACTION_SNOOZE,
            taskId = task.id,
            notificationId = notificationId,
            label = "Manana",
            extras = SNOOZE_TOMORROW,
        )

        val notification = NotificationCompat.Builder(
            context,
            PlantCareNotifications.CHANNEL_ID_TASKS,
        )
            .setSmallIcon(R.drawable.ic_notification_drop)
            .setContentTitle(title)
            .setContentText("Tarea pendiente hoy")
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(doneAction)
            .addAction(snooze1h)
            .addAction(snoozeTomorrow)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun buildAction(
        action: String,
        taskId: Long,
        notificationId: Int,
        label: String,
        extras: Int?,
    ): NotificationCompat.Action {
        val intent = Intent(context, TaskActionReceiver::class.java).apply {
            this.action = action
            putExtra(TaskActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            if (extras != null) putExtra(TaskActionReceiver.EXTRA_MINUTES, extras)
        }
        // requestCode unico por (taskId, action, label) para que cada accion
        // tenga su propio PendingIntent sin sobreescribirse.
        val requestCode = (taskId.toInt() xor action.hashCode() xor label.hashCode())
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action.Builder(0, label, pending).build()
    }

    private fun notifIdFor(taskId: Long): Int =
        PlantCareNotifications.NOTIFICATION_ID_TASKS_BASE + (taskId.toInt() and 0xFFFF)

    private fun hasNotificationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val SNOOZE_60 = 60
        private const val SNOOZE_TOMORROW = 24 * 60
    }
}
