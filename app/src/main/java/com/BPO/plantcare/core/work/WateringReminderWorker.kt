package com.BPO.plantcare.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.BPO.plantcare.core.notification.WateringNotifier
import com.BPO.plantcare.domain.model.needsWatering
import com.BPO.plantcare.domain.repository.PlantRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class WateringReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: PlantRepository,
    private val notifier: WateringNotifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        val now = System.currentTimeMillis()
        val due = repository.observeAll().first().filter { it.needsWatering(now) }
        if (due.isNotEmpty()) notifier.showWateringReminder(due)
        Result.success()
    }.getOrElse { Result.retry() }
}
