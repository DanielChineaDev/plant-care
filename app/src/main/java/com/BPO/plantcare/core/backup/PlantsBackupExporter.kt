package com.BPO.plantcare.core.backup

import android.content.Context
import android.net.Uri
import com.BPO.plantcare.domain.repository.PlantRepository
import com.BPO.plantcare.domain.repository.WateringLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exporta la coleccion de plantas + riegos del usuario a JSON. Para
 * importar de vuelta haria falta un importer (no parte de este MVP).
 *
 * El usuario elige el destino con SAF (CreateDocument) desde la UI; aqui
 * solo recibimos el [Uri] al que escribir.
 */
@Singleton
class PlantsBackupExporter @Inject constructor(
    private val plantRepository: PlantRepository,
    private val wateringLogRepository: WateringLogRepository,
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun exportToUri(context: Context, uri: Uri): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val plants = plantRepository.observeAll().first()
                val logs = wateringLogRepository.observeAll().first()
                val backup = BackupPayload(
                    version = 1,
                    exportedAt = System.currentTimeMillis(),
                    plants = plants.map { p ->
                        PlantDto(
                            id = p.id,
                            nickname = p.nickname,
                            scientificName = p.scientificName,
                            commonName = p.commonName,
                            family = p.family,
                            genus = p.genus,
                            referenceImageUrl = p.referenceImageUrl,
                            addedAt = p.addedAt,
                            lastWateredAt = p.lastWateredAt,
                            wateringIntervalDays = p.wateringIntervalDays,
                            notes = p.notes,
                            isOutdoor = p.isOutdoor,
                        )
                    },
                    wateringLogs = logs.map { l ->
                        WateringLogDto(
                            id = l.id,
                            plantId = l.plantId,
                            timestamp = l.timestamp,
                            note = l.note,
                        )
                    },
                )
                val payload = json.encodeToString(backup)
                context.contentResolver.openOutputStream(uri, "w")
                    ?.use { out -> out.write(payload.toByteArray(Charsets.UTF_8)) }
                    ?: error("No se pudo abrir el destino para escribir")
                backup.plants.size
            }
        }
}

@Serializable
private data class BackupPayload(
    val version: Int,
    val exportedAt: Long,
    val plants: List<PlantDto>,
    val wateringLogs: List<WateringLogDto>,
)

@Serializable
private data class PlantDto(
    val id: Long,
    val nickname: String?,
    val scientificName: String,
    val commonName: String?,
    val family: String?,
    val genus: String?,
    val referenceImageUrl: String?,
    val addedAt: Long,
    val lastWateredAt: Long?,
    val wateringIntervalDays: Int,
    val notes: String?,
    val isOutdoor: Boolean?,
)

@Serializable
private data class WateringLogDto(
    val id: Long,
    val plantId: Long,
    val timestamp: Long,
    val note: String?,
)
