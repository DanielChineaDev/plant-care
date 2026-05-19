package com.BPO.plantcare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.BPO.plantcare.data.local.entity.PlantTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantTaskDao {

    @Query("SELECT * FROM plant_tasks WHERE plantId = :plantId ORDER BY type ASC")
    fun observeForPlant(plantId: Long): Flow<List<PlantTaskEntity>>

    @Query("SELECT * FROM plant_tasks WHERE plantId = :plantId AND type = :type LIMIT 1")
    suspend fun findByPlantAndType(plantId: Long, type: String): PlantTaskEntity?

    @Query("SELECT * FROM plant_tasks WHERE enabled = 1")
    fun observeAllEnabled(): Flow<List<PlantTaskEntity>>

    @Query("SELECT * FROM plant_tasks WHERE enabled = 1")
    suspend fun getAllEnabled(): List<PlantTaskEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(task: PlantTaskEntity): Long

    @Update
    suspend fun update(task: PlantTaskEntity)

    @Query("DELETE FROM plant_tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE plant_tasks SET lastDoneAt = :now, snoozedUntil = NULL WHERE id = :id")
    suspend fun markDone(id: Long, now: Long)

    @Query("UPDATE plant_tasks SET snoozedUntil = :until WHERE id = :id")
    suspend fun snooze(id: Long, until: Long)

    @Query("UPDATE plant_tasks SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE plant_tasks SET intervalDays = :days, snoozedUntil = NULL WHERE id = :id")
    suspend fun updateInterval(id: Long, days: Int)
}
