package com.BPO.plantcare.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.glance.appwidget.updateAll
import com.BPO.plantcare.core.notification.WateringNotifier
import com.BPO.plantcare.core.widget.WateringWidget
import com.BPO.plantcare.domain.model.needsWatering
import com.BPO.plantcare.domain.repository.PlantRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class WateringReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: PlantRepository,
    private val notifier: WateringNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val now = System.currentTimeMillis()
        val due = repository.observeAll().first().filter { it.needsWatering(now) }
        if (due.isNotEmpty()) notifier.showWateringReminder(due)
        // Refresca el widget aunque no haya nada que regar (para mostrar el estado "sin riegos hoy").
        WateringWidget().updateAll(appContext)
        Result.success()
    }.getOrElse { Result.retry() }
}
