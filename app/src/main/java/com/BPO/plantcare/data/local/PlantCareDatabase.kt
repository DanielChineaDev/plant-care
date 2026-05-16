package com.BPO.plantcare.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.BPO.plantcare.data.local.dao.PlantDao
import com.BPO.plantcare.data.local.dao.WateringLogDao
import com.BPO.plantcare.data.local.entity.PlantEntity
import com.BPO.plantcare.data.local.entity.WateringLogEntity

@Database(
    entities = [PlantEntity::class, WateringLogEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class PlantCareDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun wateringLogDao(): WateringLogDao

    companion object {
        const val NAME = "plantcare.db"
    }
}
