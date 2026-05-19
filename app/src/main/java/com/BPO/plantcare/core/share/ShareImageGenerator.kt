package com.BPO.plantcare.core.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.FileProvider
import com.BPO.plantcare.domain.model.Plant
import java.io.File
import java.io.FileOutputStream

/**
 * Genera un PNG "card" compartible para una planta y dispara el chooser
 * del sistema. El bitmap es de tamano fijo 1080x1350 (formato 4:5 ideal
 * para Instagram/Twitter).
 *
 * Si no hay foto disponible, dibuja un placeholder verde con un emoji
 * de planta en grande. Anade marca PlantCare al pie.
 */
object ShareImageGenerator {

    private const val WIDTH = 1080
    private const val HEIGHT = 1350

    fun shareAsImage(context: Context, plant: Plant) {
        val bitmap = buildBitmap(context, plant)
        val file = saveToCache(context, bitmap, "plant_${plant.id}_${System.currentTimeMillis()}.png")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_TEXT,
                "${plant.displayName} - cuidado con PlantCare 🌱",
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Compartir ${plant.displayName}")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun buildBitmap(context: Context, plant: Plant): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // Fondo blanco.
        canvas.drawColor(Color.WHITE)

        // Foto en la parte superior (cuadrada 1080x1080).
        val photoFile = plant.userPhotoPath?.let { File(it) }?.takeIf { it.exists() }
        val photoArea = Rect(0, 0, WIDTH, WIDTH)
        if (photoFile != null) {
            BitmapFactory.decodeFile(photoFile.absolutePath)?.let { src ->
                drawCropped(canvas, src, photoArea)
                src.recycle()
            } ?: drawPlaceholder(canvas, photoArea)
        } else {
            drawPlaceholder(canvas, photoArea)
        }

        // Texto inferior (zona 1080x270).
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F3A1F")
            textSize = 64f
            isFakeBoldText = true
        }
        canvas.drawText(plant.displayName, 48f, WIDTH + 100f, namePaint)

        val sciPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5C7A5C")
            textSize = 36f
            isAntiAlias = true
        }
        canvas.drawText(plant.scientificName, 48f, WIDTH + 160f, sciPaint)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3A7D3A")
            textSize = 40f
            isFakeBoldText = true
        }
        canvas.drawText("🌱 PlantCare", 48f, WIDTH + 240f, brandPaint)

        return bitmap
    }

    private fun drawCropped(canvas: Canvas, src: Bitmap, dst: Rect) {
        val srcAspect = src.width.toFloat() / src.height
        val dstAspect = dst.width().toFloat() / dst.height()
        val srcRect = if (srcAspect > dstAspect) {
            val newW = (src.height * dstAspect).toInt()
            val offset = (src.width - newW) / 2
            Rect(offset, 0, offset + newW, src.height)
        } else {
            val newH = (src.width / dstAspect).toInt()
            val offset = (src.height - newH) / 2
            Rect(0, offset, src.width, offset + newH)
        }
        canvas.drawBitmap(src, srcRect, dst, null)
    }

    private fun drawPlaceholder(canvas: Canvas, area: Rect) {
        val bg = Paint().apply { color = Color.parseColor("#C8E6C9") }
        canvas.drawRect(area, bg)
        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 320f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🌱", area.centerX().toFloat(), area.centerY() + 120f, emojiPaint)
    }

    private fun saveToCache(context: Context, bitmap: Bitmap, fileName: String): File {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        bitmap.recycle()
        return file
    }
}
