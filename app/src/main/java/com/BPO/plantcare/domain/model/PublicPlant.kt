package com.BPO.plantcare.domain.model

/**
 * Subset publico de una Plant: solo lo que queremos mostrar a otros
 * usuarios cuando la coleccion del owner es publica.
 *
 * Por ahora NO subimos las fotos del usuario (eso seria Firebase Storage
 * extra). Mostramos la imagen de referencia de PlantNet/Wikipedia y los
 * nombres comun + cientifico.
 */
data class PublicPlant(
    val id: String,
    val scientificName: String,
    val commonName: String?,
    val nickname: String?,
    val referenceImageUrl: String?,
    /** Foto principal subida por el usuario a Firebase Storage. */
    val userPhotoUrl: String? = null,
    val addedAt: Long,
    /** Notas/descripcion publica (solo si el propietario las hace visibles). */
    val notes: String? = null,
    /** URLs del diario fotografico publico (solo si el propietario lo permite). */
    val diaryPhotos: List<String> = emptyList(),
) {
    val displayName: String
        get() = nickname?.takeIf { it.isNotBlank() }
            ?: commonName?.takeIf { it.isNotBlank() }
            ?: scientificName
}
