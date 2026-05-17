package com.BPO.plantcare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.BPO.plantcare.data.local.entity.PlantPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantPhotoDao {

    @Query("SELECT * FROM plant_photos WHERE plantId = :plantId ORDER BY timestamp DESC")
    fun observeForPlant(plantId: Long): Flow<List<PlantPhotoEntity>>

    @Query("SELECT * FROM plant_photos WHERE plantId = :plantId")
    suspend fun getForPlant(plantId: Long): List<PlantPhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PlantPhotoEntity): Long

    @Query("SELECT * FROM plant_photos WHERE id = :id")
    suspend fun getById(id: Long): PlantPhotoEntity?

    @Query("DELETE FROM plant_photos WHERE id = :id")
    suspend fun delete(id: Long)
}
