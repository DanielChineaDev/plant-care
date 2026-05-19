package com.BPO.plantcare.core.backup

import android.content.Context
import android.net.Uri
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.model.WateringLog
import com.BPO.plantcare.domain.repository.PlantRepository
import com.BPO.plantcare.domain.repository.WateringLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lee un JSON exportado por [PlantsBackupExporter] y lo importa a Room.
 *
 * Comportamiento:
 *  - Las plantas se insertan como NUEVAS (id autogenerado). NO se intenta
 *    deduplicar por nombre cientifico ni nada, asi que si importas dos
 *    veces el mismo backup, las plantas se duplicaran.
 *  - Los logs de riego se reinsertan apuntando al nuevo id local de la
 *    planta mediante un mapa idAntiguo -> idNuevo.
 *  - Las fotos NO estan dentro del JSON (son del filesystem privado);
 *    por eso el campo userPhotoPath se importa pero apuntara a una ruta
 *    que probablemente no existe ya en el dispositivo. La UI lo gestiona
 *    porque cae a un placeholder cuando el archivo no existe.
 *
 * Devuelve el numero de plantas importadas o un Result.failure si el
 * JSON no es valido.
 */
@Singleton
class PlantsBackupImporter @Inject constructor(
    private val plantRepository: PlantRepository,
    private val wateringLogRepository: WateringLogRepository,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun importFromUri(context: Context, uri: Uri): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)
                    ?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: error("No se pudo abrir el archivo")
                val payload = json.decodeFromString<ImportBackupPayload>(text)
                if (payload.version > SUPPORTED_VERSION) {
                    error("Version de backup no soportada: ${payload.version}")
                }

                val idMap = mutableMapOf<Long, Long>()
                payload.plants.forEach { dto ->
                    val newId = plantRepository.add(
                        Plant(
                            id = 0L,
                            nickname = dto.nickname,
                            scientificName = dto.scientificName,
                            commonName = dto.commonName,
                            family = dto.family,
                            genus = dto.genus,
                            referenceImageUrl = dto.referenceImageUrl,
                            userPhotoPath = null, // las fotos no viajan en el JSON
                            addedAt = dto.addedAt,
                            lastWateredAt = dto.lastWateredAt,
                            wateringIntervalDays = dto.wateringIntervalDays,
                            notes = dto.notes,
                            isOutdoor = dto.isOutdoor,
                        ),
                    )
                    idMap[dto.id] = newId
                }

                payload.wateringLogs.forEach { dto ->
                    val newPlantId = idMap[dto.plantId] ?: return@forEach
                    wateringLogRepository.add(
                        WateringLog(
                            id = 0L,
                            plantId = newPlantId,
                            timestamp = dto.timestamp,
                            note = dto.note,
                        ),
                    )
                }
                payload.plants.size
            }
        }

    companion object {
        private const val SUPPORTED_VERSION = 1
    }
}

@Serializable
private data class ImportBackupPayload(
    val version: Int,
    val exportedAt: Long = 0L,
    val plants: List<ImportPlantDto> = emptyList(),
    val wateringLogs: List<ImportWateringLogDto> = emptyList(),
)

@Serializable
private data class ImportPlantDto(
    val id: Long,
    val nickname: String? = null,
    val scientificName: String,
    val commonName: String? = null,
    val family: String? = null,
    val genus: String? = null,
    val referenceImageUrl: String? = null,
    val addedAt: Long,
    val lastWateredAt: Long? = null,
    val wateringIntervalDays: Int = 5,
    val notes: String? = null,
    val isOutdoor: Boolean? = null,
)

@Serializable
private data class ImportWateringLogDto(
    val id: Long,
    val plantId: Long,
    val timestamp: Long,
    val note: String? = null,
)
