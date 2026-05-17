package com.BPO.plantcare.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.InvertColors
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    genusApproximation: String? = null,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Cuidados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (genusApproximation != null) {
                Spacer(modifier = Modifier.height(8.dp))
                GenusApproximationBanner(
                    genus = genusApproximation,
                    referenceSpecies = guide.scientificName,
                )
            }
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

            CareRow(icon = Icons.Outlined.WbSunny, label = "Luz", value = guide.light.label)
            CareRow(icon = Icons.Outlined.InvertColors, label = "Humedad", value = guide.humidity.label)
            CareRow(
                icon = Icons.Outlined.WaterDrop,
                label = "Riego",
                value = "Cada ${guide.wateringIntervalDays} dias",
            )
            guide.wateringNotes?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 32.dp, top = 2.dp, bottom = 6.dp),
                )
            }
            CareRow(icon = Icons.Outlined.Yard, label = "Sustrato", value = guide.substrate)
            CareRow(icon = Icons.Outlined.Eco, label = "Abono", value = guide.fertilizing)
            CareRow(icon = Icons.Outlined.Cached, label = "Trasplante", value = guide.repotting)

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
private fun GenusApproximationBanner(genus: String, referenceSpecies: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Datos aproximados del genero ${genus.replaceFirstChar { it.titlecase() }}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Tu especie aun no esta en el catalogo. Estos cuidados son los de " +
                        "$referenceSpecies, una pariente cercana. Pueden variar ligeramente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun CareRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
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
}
