package com.BPO.plantcare.di

import com.BPO.plantcare.data.repository.PlantCatalogRepositoryImpl
import com.BPO.plantcare.data.repository.PlantIdentificationRepositoryImpl
import com.BPO.plantcare.data.repository.PlantPhotoRepositoryImpl
import com.BPO.plantcare.data.repository.PlantRepositoryImpl
import com.BPO.plantcare.data.repository.WateringLogRepositoryImpl
import com.BPO.plantcare.data.repository.WikipediaRepositoryImpl
import com.BPO.plantcare.domain.repository.PlantCatalogRepository
import com.BPO.plantcare.domain.repository.PlantIdentificationRepository
import com.BPO.plantcare.domain.repository.PlantPhotoRepository
import com.BPO.plantcare.domain.repository.PlantRepository
import com.BPO.plantcare.domain.repository.WateringLogRepository
import com.BPO.plantcare.domain.repository.WikipediaRepository
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

    @Binds
    @Singleton
    abstract fun bindPlantRepository(
        impl: PlantRepositoryImpl
    ): PlantRepository

    @Binds
    @Singleton
    abstract fun bindWateringLogRepository(
        impl: WateringLogRepositoryImpl
    ): WateringLogRepository

    @Binds
    @Singleton
    abstract fun bindWikipediaRepository(
        impl: WikipediaRepositoryImpl
    ): WikipediaRepository

    @Binds
    @Singleton
    abstract fun bindPlantCatalogRepository(
        impl: PlantCatalogRepositoryImpl
    ): PlantCatalogRepository

    @Binds
    @Singleton
    abstract fun bindPlantPhotoRepository(
        impl: PlantPhotoRepositoryImpl
    ): PlantPhotoRepository
}
