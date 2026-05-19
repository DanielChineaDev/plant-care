package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.model.PlantTask
import com.BPO.plantcare.domain.model.isDue
import com.BPO.plantcare.domain.repository.PlantRepository
import com.BPO.plantcare.domain.repository.PlantTaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Tareas (no-riego) que vencen hoy o antes, agrupadas con su planta.
 *
 * Para "tareas de hoy" en Calendar/Home. NO incluye riego, que sigue
 * viviendo en `lastWateredAt` de Plant + WateringLog. Una vez unifiquemos
 * todo en plant_tasks, esto pasara a incluirlo.
 */
class ObserveTodayTasksUseCase @Inject constructor(
    private val plantTaskRepository: PlantTaskRepository,
    private val plantRepository: PlantRepository,
) {
    data class TodayTask(val task: PlantTask, val plant: Plant)

    operator fun invoke(): Flow<List<TodayTask>> =
        combine(
            plantTaskRepository.observeAllEnabled(),
            plantRepository.observeAll(),
        ) { tasks, plants ->
            val now = System.currentTimeMillis()
            val plantsById = plants.associateBy { it.id }
            tasks
                .mapNotNull { task ->
                    val plant = plantsById[task.plantId] ?: return@mapNotNull null
                    if (task.isDue(now, plant.addedAt)) TodayTask(task, plant) else null
                }
                .sortedBy { it.plant.displayName.lowercase() }
        }
}
