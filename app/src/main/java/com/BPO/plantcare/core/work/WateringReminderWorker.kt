package com.BPO.plantcare.core.work

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.BPO.plantcare.core.notification.WateringNotifier
import com.BPO.plantcare.core.widget.WateringWidget
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.model.UserSettings
import com.BPO.plantcare.domain.model.needsWatering
import com.BPO.plantcare.domain.repository.PlantRepository
import com.BPO.plantcare.domain.repository.PreferencesRepository
import com.BPO.plantcare.domain.repository.WeatherRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class WateringReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: PlantRepository,
    private val preferences: PreferencesRepository,
    private val weather: WeatherRepository,
    private val notifier: WateringNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val now = System.currentTimeMillis()
        val settings = preferences.settings.first()

        val onTrip = settings.isCurrentlyOnTrip(now)
        val allDue = repository.observeAll().first().filter { it.needsWatering(now) }

        val toNotify = filterByRainfall(allDue, settings)

        if (toNotify.isNotEmpty() && !onTrip) {
            notifier.showWateringReminder(toNotify)
        }
        WateringWidget().updateAll(appContext)
        Result.success()
    }.getOrElse { Result.retry() }

    /**
     * Si el usuario activo "saltar riego si llovio" y tenemos su ubicacion,
     * filtra las plantas marcadas como exterior cuando la lluvia acumulada
     * de las ultimas 24h supera [RAIN_THRESHOLD_MM].
     */
    private suspend fun filterByRainfall(
        plants: List<Plant>,
        settings: UserSettings,
    ): List<Plant> {
        if (!settings.weatherAware || !settings.hasLocation) return plants
        val lat = settings.latitude ?: return plants
        val lon = settings.longitude ?: return plants
        val rainfall = weather.getRecentRainfallMm(lat, lon).getOrNull() ?: return plants
        if (rainfall < RAIN_THRESHOLD_MM) return plants
        // Llovio suficiente: omitimos las plantas marcadas como exterior.
        return plants.filter { it.isOutdoor != true }
    }

    companion object {
        /** Litros / metro cuadrado en 24h que consideramos un "buen riego" natural. */
        private const val RAIN_THRESHOLD_MM = 5.0
    }
}
