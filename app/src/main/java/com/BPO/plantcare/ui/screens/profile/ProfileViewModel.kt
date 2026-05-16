package com.BPO.plantcare.ui.screens.profile

import androidx.lifecycle.ViewModel
import com.BPO.plantcare.core.work.WateringReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val scheduler: WateringReminderScheduler,
) : ViewModel() {

    fun testWateringNotification() {
        scheduler.runNow()
    }
}
