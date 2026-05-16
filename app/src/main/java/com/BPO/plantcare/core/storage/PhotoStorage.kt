package com.BPO.plantcare.core.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persiste fotos del usuario en el almacenamiento interno (filesDir) para que no las
 * limpie el sistema (como ocurre con cacheDir).
 */
@Singleton
class PhotoStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val photosDir: File by lazy {
        File(context.filesDir, "plant_photos").apply { mkdirs() }
    }

    /** Copia el archivo a almacenamiento interno y devuelve la ruta absoluta. */
    suspend fun persist(source: File): String = withContext(Dispatchers.IO) {
        val target = File(photosDir, "plant_${System.currentTimeMillis()}.jpg")
        source.copyTo(target, overwrite = true)
        target.absolutePath
    }

    suspend fun delete(path: String) = withContext(Dispatchers.IO) {
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }
}
