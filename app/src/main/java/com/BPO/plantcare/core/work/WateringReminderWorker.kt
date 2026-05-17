package com.BPO.plantcare.core.work

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.BPO.plantcare.core.notification.WateringNotifier
import com.BPO.plantcare.core.widget.WateringWidget
import com.BPO.plantcare.domain.model.needsWatering
import com.BPO.plantcare.domain.repository.PlantRepository
import com.BPO.plantcare.domain.repository.PreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class WateringReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: PlantRepository,
    private val preferences: PreferencesRepository,
    private val notifier: WateringNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val now = System.currentTimeMillis()
        val settings = preferences.settings.first()

        // Si el usuario esta de viaje, no molestamos con notificaciones.
        // Aun asi refrescamos el widget para que muestre el estado real.
        val onTrip = settings.isCurrentlyOnTrip(now)

        val due = repository.observeAll().first().filter { it.needsWatering(now) }
        if (due.isNotEmpty() && !onTrip) {
            notifier.showWateringReminder(due)
        }
        WateringWidget().updateAll(appContext)
        Result.success()
    }.getOrElse { Result.retry() }
}
