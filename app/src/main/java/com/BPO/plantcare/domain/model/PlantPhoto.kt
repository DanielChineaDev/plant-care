package com.BPO.plantcare.domain.model

data class PlantPhoto(
    val id: Long = 0,
    val plantId: Long,
    val path: String,
    val timestamp: Long,
    val note: String?,
    /**
     * URL de la foto en Firebase Storage. Permite recuperar la imagen al
     * cambiar de dispositivo, cuando [path] (ruta local) ya no existe.
     */
    val remoteUrl: String? = null,
)

/**
 * Modelo de imagen preferido: el archivo local si existe, si no la URL en la
 * nube. Apto para Coil (acepta File o String).
 */
fun PlantPhoto.imageModel(): Any =
    java.io.File(path).takeIf { it.exists() } ?: remoteUrl ?: path
