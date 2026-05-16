package com.BPO.plantcare

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.BPO.plantcare.core.notification.PlantCareNotifications
import com.BPO.plantcare.core.work.WateringReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PlantCareApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var wateringScheduler: WateringReminderScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        PlantCareNotifications.registerChannels(this)
        wateringScheduler.scheduleDaily()
    }
}
