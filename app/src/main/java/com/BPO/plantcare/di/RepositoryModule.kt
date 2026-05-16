package com.BPO.plantcare.di

import com.BPO.plantcare.data.repository.PlantIdentificationRepositoryImpl
import com.BPO.plantcare.domain.repository.PlantIdentificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPlantIdentificationRepository(
        impl: PlantIdentificationRepositoryImpl
    ): PlantIdentificationRepository
}
