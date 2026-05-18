package com.BPO.plantcare.di

import com.BPO.plantcare.data.preferences.PreferencesRepositoryImpl
import com.BPO.plantcare.data.repository.AuthRepositoryImpl
import com.BPO.plantcare.data.repository.ChatRepositoryImpl
import com.BPO.plantcare.data.repository.CommunityRepositoryImpl
import com.BPO.plantcare.data.repository.DiagnosisRepositoryImpl
import com.BPO.plantcare.data.repository.PlantCatalogRepositoryImpl
import com.BPO.plantcare.data.repository.PlantIdentificationRepositoryImpl
import com.BPO.plantcare.data.repository.PlantPhotoRepositoryImpl
import com.BPO.plantcare.data.repository.PlantRepositoryImpl
import com.BPO.plantcare.data.repository.PublicProfileRepositoryImpl
import com.BPO.plantcare.data.repository.WateringLogRepositoryImpl
import com.BPO.plantcare.data.repository.WeatherRepositoryImpl
import com.BPO.plantcare.data.repository.WikipediaRepositoryImpl
import com.BPO.plantcare.domain.repository.AuthRepository
import com.BPO.plantcare.domain.repository.ChatRepository
import com.BPO.plantcare.domain.repository.CommunityRepository
import com.BPO.plantcare.domain.repository.DiagnosisRepository
import com.BPO.plantcare.domain.repository.PlantCatalogRepository
import com.BPO.plantcare.domain.repository.PlantIdentificationRepository
import com.BPO.plantcare.domain.repository.PlantPhotoRepository
import com.BPO.plantcare.domain.repository.PlantRepository
import com.BPO.plantcare.domain.repository.PreferencesRepository
import com.BPO.plantcare.domain.repository.PublicProfileRepository
import com.BPO.plantcare.domain.repository.WateringLogRepository
import com.BPO.plantcare.domain.repository.WeatherRepository
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

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        impl: PreferencesRepositoryImpl
    ): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(
        impl: WeatherRepositoryImpl
    ): WeatherRepository

    @Binds
    @Singleton
    abstract fun bindDiagnosisRepository(
        impl: DiagnosisRepositoryImpl
    ): DiagnosisRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCommunityRepository(
        impl: CommunityRepositoryImpl
    ): CommunityRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindPublicProfileRepository(
        impl: PublicProfileRepositoryImpl
    ): PublicProfileRepository
}
