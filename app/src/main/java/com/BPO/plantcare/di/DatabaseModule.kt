package com.BPO.plantcare.di

import android.content.Context
import androidx.room.Room
import com.BPO.plantcare.data.local.PlantCareDatabase
import com.BPO.plantcare.data.local.dao.PlantDao
import com.BPO.plantcare.data.local.dao.PlantPhotoDao
import com.BPO.plantcare.data.local.dao.WateringLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PlantCareDatabase =
        Room.databaseBuilder(context, PlantCareDatabase::class.java, PlantCareDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePlantDao(db: PlantCareDatabase): PlantDao = db.plantDao()

    @Provides
    fun provideWateringLogDao(db: PlantCareDatabase): WateringLogDao = db.wateringLogDao()

    @Provides
    fun providePlantPhotoDao(db: PlantCareDatabase): PlantPhotoDao = db.plantPhotoDao()
}
