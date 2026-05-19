package com.BPO.plantcare.data.repository

import com.BPO.plantcare.data.local.dao.PlantTaskDao
import com.BPO.plantcare.data.local.entity.PlantTaskEntity
import com.BPO.plantcare.domain.model.PlantTask
import com.BPO.plantcare.domain.model.PlantTaskType
import com.BPO.plantcare.domain.repository.PlantTaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlantTaskRepositoryImpl @Inject constructor(
    private val dao: PlantTaskDao,
) : PlantTaskRepository {

    override fun observeForPlant(plantId: Long): Flow<List<PlantTask>> =
        dao.observeForPlant(plantId).map { list -> list.mapNotNull { it.toDomainOrNull() } }

    override fun observeAllEnabled(): Flow<List<PlantTask>> =
        dao.observeAllEnabled().map { list -> list.mapNotNull { it.toDomainOrNull() } }

    override suspend fun enableTask(
        plantId: Long,
        type: PlantTaskType,
        intervalDays: Int,
    ): Long {
        val existing = dao.findByPlantAndType(plantId, type.storageKey)
        return if (existing != null) {
            // Re-habilitar conservando lastDoneAt y snoozedUntil (no resetear).
            dao.update(existing.copy(enabled = true, intervalDays = intervalDays))
            existing.id
        } else {
            dao.insert(
                PlantTaskEntity(
                    plantId = plantId,
                    type = type.storageKey,
                    intervalDays = intervalDays,
                    lastDoneAt = null,
                    snoozedUntil = null,
                    enabled = true,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    override suspend fun disableTask(taskId: Long) {
        dao.setEnabled(taskId, false)
    }

    override suspend fun markDone(taskId: Long, now: Long) {
        dao.markDone(taskId, now)
    }

    override suspend fun snooze(taskId: Long, until: Long) {
        dao.snooze(taskId, until)
    }

    override suspend fun updateInterval(taskId: Long, days: Int) {
        dao.updateInterval(taskId, days.coerceAtLeast(1))
    }

    private fun PlantTaskEntity.toDomainOrNull(): PlantTask? {
        val parsedType = PlantTaskType.fromStorageKey(type) ?: return null
        return PlantTask(
            id = id,
            plantId = plantId,
            type = parsedType,
            intervalDays = intervalDays,
            lastDoneAt = lastDoneAt,
            snoozedUntil = snoozedUntil,
            enabled = enabled,
            createdAt = createdAt,
        )
    }
}
