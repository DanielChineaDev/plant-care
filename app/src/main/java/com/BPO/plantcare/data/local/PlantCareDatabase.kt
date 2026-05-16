package com.BPO.plantcare.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.BPO.plantcare.data.local.dao.PlantDao
import com.BPO.plantcare.data.local.entity.PlantEntity

@Database(
    entities = [PlantEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PlantCareDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao

    companion object {
        const val NAME = "plantcare.db"
    }
}
