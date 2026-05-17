package com.BPO.plantcare.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.BPO.plantcare.domain.model.CareDifficulty
import com.BPO.plantcare.domain.model.PlantCareGuide
import com.BPO.plantcare.ui.theme.StatusHealthy
import com.BPO.plantcare.ui.theme.StatusThirsty
import com.BPO.plantcare.ui.theme.StatusWarning

@Composable
fun CareGuideCard(
    guide: PlantCareGuide,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Cuidados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DifficultyChip(guide.difficulty)
                LocationChip(indoor = guide.indoor, outdoor = guide.outdoor)
                if (guide.toxicToPets) ToxicChip()
            }

            Spacer(modifier = Modifier.height(12.dp))

            CareRow(label = "Luz", value = guide.light.label)
            CareRow(label = "Humedad", value = guide.humidity.label)
            CareRow(label = "Riego", value = "Cada ${guide.wateringIntervalDays} dias")
            guide.wateringNotes?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 6.dp),
                )
            }
            CareRow(label = "Sustrato", value = guide.substrate)
            CareRow(label = "Abono", value = guide.fertilizing)
            CareRow(label = "Trasplante", value = guide.repotting)

            guide.funFact?.let { fact ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "✨ Sabías que...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = fact,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DifficultyChip(difficulty: CareDifficulty) {
    val color = when (difficulty) {
        CareDifficulty.EASY -> StatusHealthy
        CareDifficulty.MEDIUM -> StatusWarning
        CareDifficulty.HARD -> StatusThirsty
        CareDifficulty.EXPERT -> Color(0xFF8E24AA)
        CareDifficulty.PRO -> Color(0xFF3949AB)
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(difficulty.label) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = color.copy(alpha = 0.9f),
            disabledLabelColor = Color.White,
        ),
    )
}

@Composable
fun LocationChip(indoor: Boolean, outdoor: Boolean) {
    val label = when {
        indoor && outdoor -> "Interior / exterior"
        indoor -> "Interior"
        outdoor -> "Exterior"
        else -> return
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    )
}

@Composable
fun ToxicChip() {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text("⚠ Tóxica para mascotas") },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = StatusThirsty.copy(alpha = 0.15f),
            disabledLabelColor = StatusThirsty,
        ),
    )
}

@Composable
private fun CareRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
