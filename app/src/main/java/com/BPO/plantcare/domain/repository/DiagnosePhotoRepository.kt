package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.DiagnosisAnalysis
import java.io.File

/**
 * Analiza una foto de planta y devuelve posibles diagnosticos.
 *
 * La implementacion actual es un STUB que samplea aleatoriamente del
 * catalogo local de diagnoses con confianzas demo. Cuando integremos
 * un servicio real (Plant.id Health, modelo propio entrenado, etc.)
 * se cambia la impl y la UI sigue funcionando sin tocar.
 */
interface DiagnosePhotoRepository {
    suspend fun analyze(photo: File): Result<DiagnosisAnalysis>
}
