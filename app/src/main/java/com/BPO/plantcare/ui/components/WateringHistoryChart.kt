package com.BPO.plantcare.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.BPO.plantcare.domain.model.WateringLog
import kotlin.math.max

/**
 * Grafica de cadencia REAL vs SUGERIDA del riego de una planta.
 *
 * Eje X: cada riego (de mas antiguo a mas reciente, hasta MAX_POINTS).
 * Eje Y: dias entre ese riego y el anterior.
 *
 * Linea horizontal punteada = intervalo sugerido. Puntos = intervalos
 * reales. Si los puntos quedan por encima de la linea, el user esta
 * regando MENOS de lo recomendado. Si quedan por debajo, esta regando
 * mas.
 *
 * Con menos de 2 puntos no se dibuja nada; en su lugar un texto
 * explicativo.
 */
@Composable
fun WateringHistoryChart(
    history: List<WateringLog>,
    suggestedIntervalDays: Int,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Cadencia de riego",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = "Tu ritmo real (puntos) vs el sugerido ($suggestedIntervalDays dias).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val intervals = remember(history) { computeIntervals(history) }
            Spacer(modifier = Modifier.size(12.dp))

            if (intervals.size < 2) {
                Text(
                    text = "Riega al menos 3 veces para empezar a ver la grafica.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                return@Column
            }

            val avg = intervals.average()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "Media: ${"%.1f".format(avg)} dias",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.size(16.dp))
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.tertiary),
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "Sugerido: $suggestedIntervalDays dias",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(modifier = Modifier.size(12.dp))

            val pointColor = MaterialTheme.colorScheme.primary
            val refColor = MaterialTheme.colorScheme.tertiary
            val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            ) {
                val w = size.width
                val h = size.height
                val maxObserved = (intervals.maxOrNull() ?: suggestedIntervalDays).toFloat()
                val maxY = max(suggestedIntervalDays.toFloat(), maxObserved) * 1.2f
                val minY = 0f

                // Ejes basicos.
                drawLine(
                    color = axisColor,
                    start = Offset(0f, h),
                    end = Offset(w, h),
                    strokeWidth = 2f,
                )
                drawLine(
                    color = axisColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, h),
                    strokeWidth = 2f,
                )

                // Linea de referencia (sugerido) punteada.
                val refY = h - ((suggestedIntervalDays - minY) / (maxY - minY)) * h
                drawLine(
                    color = refColor,
                    start = Offset(0f, refY),
                    end = Offset(w, refY),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                )

                // Puntos y linea de los intervalos reales.
                val stepX = if (intervals.size > 1) w / (intervals.size - 1) else w
                val points = intervals.mapIndexed { idx, days ->
                    val x = idx * stepX
                    val y = h - ((days.toFloat() - minY) / (maxY - minY)) * h
                    Offset(x, y)
                }
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = pointColor.copy(alpha = 0.6f),
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 4f,
                    )
                }
                points.forEach { p ->
                    drawCircle(
                        color = pointColor,
                        radius = 8f,
                        center = p,
                        style = Stroke(width = 4f),
                    )
                    drawCircle(
                        color = pointColor.copy(alpha = 0.2f),
                        radius = 8f,
                        center = p,
                    )
                }
            }
        }
    }
}

/**
 * Calcula los intervalos en dias entre riegos consecutivos del historial.
 * Devuelve hasta MAX_POINTS, descartando los mas antiguos si excede.
 */
private fun computeIntervals(history: List<WateringLog>): List<Int> {
    val sorted = history.sortedBy { it.timestamp }
    if (sorted.size < 2) return emptyList()
    val intervals = sorted.zipWithNext { a, b ->
        val daysMs = b.timestamp - a.timestamp
        (daysMs / (24L * 60L * 60L * 1000L)).toInt().coerceAtLeast(0)
    }
    return intervals.takeLast(MAX_POINTS)
}

private const val MAX_POINTS = 12

// Color no se usa fuera; suprimimos warning.
@Suppress("unused")
private val LEGEND_GUARD: Color = Color.Unspecified
