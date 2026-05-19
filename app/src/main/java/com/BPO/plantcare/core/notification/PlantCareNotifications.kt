package com.BPO.plantcare.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object PlantCareNotifications {

    const val CHANNEL_ID_WATERING = "watering_reminders"
    const val NOTIFICATION_ID_WATERING = 1001

    const val CHANNEL_ID_CHAT = "chat_messages"

    const val CHANNEL_ID_TASKS = "plant_tasks"
    /** Cada tarea se notifica con id = (taskId.toInt() XOR base) para no colisionar. */
    const val NOTIFICATION_ID_TASKS_BASE = 5000

    fun registerChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val watering = NotificationChannel(
            CHANNEL_ID_WATERING,
            "Recordatorios de riego",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Avisos diarios de las plantas que toca regar."
        }
        val chat = NotificationChannel(
            CHANNEL_ID_CHAT,
            "Mensajes",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notificaciones de mensajes directos recibidos."
        }
        val tasks = NotificationChannel(
            CHANNEL_ID_TASKS,
            "Tareas de cuidado",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Recordatorios de tareas: abonar, podar, trasplantar, etc."
        }
        manager.createNotificationChannels(listOf(watering, chat, tasks))
    }
}
