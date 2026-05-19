package com.BPO.plantcare.data.repository

import com.BPO.plantcare.domain.model.DiagnosisAnalysis
import com.BPO.plantcare.domain.model.DiagnosisMatch
import com.BPO.plantcare.domain.repository.DiagnosePhotoRepository
import com.BPO.plantcare.domain.repository.DiagnosisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * STUB temporal del diagnostico por foto. Mientras no integremos un
 * servicio de Computer Vision real:
 *
 * - Simulamos latencia (~1.2s) para que la UI muestre el loader.
 * - Devolvemos 3 matches aleatorios del catalogo local con confianzas
 *   decrecientes. La pantalla marca el resultado como "modo demo".
 *
 * Para "encender" el modo real solo hay que cambiar esta implementacion
 * por una que llame al endpoint y mapee la respuesta a DiagnosisMatch.
 */
@Singleton
class DiagnosePhotoRepositoryImpl @Inject constructor(
    private val diagnosisRepository: DiagnosisRepository,
) : DiagnosePhotoRepository {

    override suspend fun analyze(photo: File): Result<DiagnosisAnalysis> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!photo.exists()) error("Foto no encontrada")
                delay(1_200)
                val catalog = diagnosisRepository.all()
                if (catalog.isEmpty()) {
                    return@runCatching DiagnosisAnalysis(emptyList(), isDemo = true)
                }
                val random = Random(photo.length())
                val sample = catalog.shuffled(random).take(3)
                val matches = sample.mapIndexed { idx, d ->
                    val base = 0.78f - (idx * 0.18f)
                    DiagnosisMatch(diagnosis = d, confidence = base.coerceIn(0.05f, 0.95f))
                }
                DiagnosisAnalysis(matches = matches, isDemo = true)
            }
        }
}
