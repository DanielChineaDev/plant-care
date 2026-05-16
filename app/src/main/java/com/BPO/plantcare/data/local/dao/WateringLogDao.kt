package com.BPO.plantcare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.BPO.plantcare.data.local.entity.WateringLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WateringLogDao {

    @Query("SELECT * FROM watering_logs ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<WateringLogEntity>>

    @Query("SELECT * FROM watering_logs WHERE plantId = :plantId ORDER BY timestamp DESC")
    fun observeForPlant(plantId: Long): Flow<List<WateringLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: WateringLogEntity): Long

    @Query("DELETE FROM watering_logs WHERE id = :id")
    suspend fun delete(id: Long)
}
