package com.BPO.plantcare.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.BPO.plantcare.domain.repository.PlantTaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receptor de las acciones del usuario sobre una notificacion de tarea:
 *
 *  - ACTION_SNOOZE: con extras taskId + minutes -> pospone la tarea.
 *  - ACTION_DONE  : con extra  taskId           -> marca la tarea como hecha.
 *
 * En ambos casos cancela la notificacion del system tray.
 *
 * Se registra en el manifest como @AndroidEntryPoint para inyectar el
 * repositorio. Ojo: onReceive no es suspending; usamos goAsync() + un
 * scope propio para terminar el trabajo en background sin bloquear el
 * Looper. PendingResult.finish() debe llamarse SI O SI o el sistema
 * matara el proceso.
 */
@AndroidEntryPoint
class TaskActionReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: PlantTaskRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (taskId <= 0L) return

        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_SNOOZE -> {
                        val minutes = intent.getIntExtra(EXTRA_MINUTES, 60)
                        val until = System.currentTimeMillis() + minutes.toLong() * 60_000L
                        repository.snooze(taskId, until)
                    }
                    ACTION_DONE -> repository.markDone(taskId)
                }
                if (notificationId > 0) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_SNOOZE = "com.BPO.plantcare.action.TASK_SNOOZE"
        const val ACTION_DONE = "com.BPO.plantcare.action.TASK_DONE"

        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_MINUTES = "minutes"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
