package com.BPO.plantcare.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.BPO.plantcare.data.local.dao.PlantDao
import com.BPO.plantcare.data.local.dao.PlantPhotoDao
import com.BPO.plantcare.data.local.dao.PlantTaskDao
import com.BPO.plantcare.data.local.dao.WateringLogDao
import com.BPO.plantcare.data.local.entity.PlantEntity
import com.BPO.plantcare.data.local.entity.PlantPhotoEntity
import com.BPO.plantcare.data.local.entity.PlantTaskEntity
import com.BPO.plantcare.data.local.entity.WateringLogEntity

@Database(
    entities = [
        PlantEntity::class,
        WateringLogEntity::class,
        PlantPhotoEntity::class,
        PlantTaskEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
abstract class PlantCareDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun wateringLogDao(): WateringLogDao
    abstract fun plantPhotoDao(): PlantPhotoDao
    abstract fun plantTaskDao(): PlantTaskDao

    companion object {
        const val NAME = "plantcare.db"
    }
}
