package com.BPO.plantcare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.BPO.plantcare.domain.model.Poll

/**
 * Tarjeta de encuesta: lista las opciones, cada una como una barra
 * horizontal con el porcentaje de votos. Tap = votar o cambiar voto.
 *
 * Si [enabled] es false (user sin sesion), las filas no son
 * clickables.
 */
@Composable
fun PollVoteCard(
    poll: Poll,
    myVote: String?,
    enabled: Boolean,
    onVote: (optionId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = poll.totalVotes.coerceAtLeast(1)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        poll.options.forEach { option ->
            val votes = poll.votesByOption[option.id] ?: 0L
            val pct = (votes.toFloat() / total).coerceIn(0f, 1f)
            val selected = myVote == option.id
            PollOptionRow(
                text = option.text,
                votes = votes,
                pct = pct,
                selected = selected,
                enabled = enabled,
                onClick = { if (enabled) onVote(option.id) },
            )
        }
        Text(
            text = if (poll.totalVotes == 1L) "1 voto" else "${poll.totalVotes} votos",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PollOptionRow(
    text: String,
    votes: Long,
    pct: Float,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val barColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.primaryContainer
    val bgColor = MaterialTheme.colorScheme.surfaceVariant

    val rowMod = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .clip(RoundedCornerShape(22.dp))
        .background(bgColor)
        .let { if (enabled) it.clickable(onClick = onClick) else it }

    Box(modifier = rowMod, contentAlignment = Alignment.CenterStart) {
        // Barra de relleno proporcional al porcentaje.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = pct)
                .clip(RoundedCornerShape(22.dp))
                .background(barColor.copy(alpha = if (selected) 0.55f else 0.35f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Tu voto",
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.size(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(pct * 100).toInt()}% · $votes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
