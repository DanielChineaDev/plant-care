package com.BPO.plantcare.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Caja con gradiente animado horizontal (efecto "shimmer") para usar
 * como placeholder mientras carga contenido remoto.
 *
 * Internamente: tres colores en gradiente que se desplazan de izquierda
 * a derecha en bucle infinito. La velocidad y los colores son sutiles
 * para no distraer.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-translate",
    )
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight.copy(alpha = 0.6f), base),
        start = Offset(translate, 0f),
        end = Offset(translate + 400f, 0f),
    )
    Box(
        modifier = modifier
            .graphicsLayer { clip = false }
            .clip(shape)
            .background(brush),
    )
}

/**
 * Placeholder de tarjeta de post para feed. Se renderiza N veces en
 * lugar de la lista real mientras llega la primera emision de
 * Firestore. Cuando llegan los posts (o el flow confirma que la lista
 * es vacia), el caller deja de pintar estos skeletons.
 */
@Composable
fun PostCardSkeleton(modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera: avatar + nombre + tiempo
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                ShimmerBox(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                )
                Spacer(modifier = Modifier.size(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerBox(modifier = Modifier.width(120.dp).height(14.dp))
                    ShimmerBox(modifier = Modifier.width(80.dp).height(10.dp))
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            // Cuerpo: dos lineas + bloque imagen
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(14.dp))
            Spacer(modifier = Modifier.size(6.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp))
            Spacer(modifier = Modifier.size(12.dp))
            ShimmerBox(
                modifier = Modifier.fillMaxWidth().aspectRatio(1.6f),
                shape = RoundedCornerShape(12.dp),
            )
        }
    }
}
