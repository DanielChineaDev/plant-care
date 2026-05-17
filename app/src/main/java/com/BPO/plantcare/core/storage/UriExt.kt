package com.BPO.plantcare.core.storage

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Copia un [Uri] (gallery, providers, etc.) al cacheDir como JPEG.
 * Devuelve el archivo creado para subir a Retrofit o persistir despues.
 */
fun copyUriToCache(context: Context, uri: Uri): File {
    val file = File(context.cacheDir, "plantcare_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
    return file
}
