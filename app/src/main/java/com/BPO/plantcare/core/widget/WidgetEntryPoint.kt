package com.BPO.plantcare.core.widget

import com.BPO.plantcare.domain.repository.PlantRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun plantRepository(): PlantRepository
}
