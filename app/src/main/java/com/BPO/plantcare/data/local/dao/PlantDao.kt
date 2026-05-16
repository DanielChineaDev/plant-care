package com.BPO.plantcare.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.BPO.plantcare.data.local.entity.PlantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {

    @Query("SELECT * FROM plants ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plants WHERE id = :id")
    fun observeById(id: Long): Flow<PlantEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plant: PlantEntity): Long

    @Update
    suspend fun update(plant: PlantEntity)

    @Query("DELETE FROM plants WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE plants SET lastWateredAt = :timestamp WHERE id = :id")
    suspend fun markWatered(id: Long, timestamp: Long)
}
