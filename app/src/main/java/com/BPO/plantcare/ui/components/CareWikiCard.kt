package com.BPO.plantcare.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.BPO.plantcare.domain.model.CareWikiAggregate
import com.BPO.plantcare.domain.model.CareWikiContribution

/**
 * Card "Lo que dicen otros usuarios" para la ficha de planta. Resume
 * por mediana lo aportado por la comunidad y lista las ultimas
 * contribuciones. Permite anadir la propia.
 */
@Composable
fun CareWikiCard(
    aggregate: CareWikiAggregate,
    contributions: List<CareWikiContribution>,
    canContribute: Boolean,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Wiki de la comunidad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = "${aggregate.contributionCount} contribuciones de otros usuarios para esta especie.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.size(12.dp))

            if (aggregate.contributionCount > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    aggregate.medianWateringDays?.let {
                        StatChip(label = "Riego (mediana)", value = "$it d")
                    }
                    aggregate.medianFertilizeDays?.let {
                        StatChip(label = "Abono (mediana)", value = "$it d")
                    }
                    aggregate.majorityLightLevel?.let {
                        StatChip(label = "Luz", value = lightLabel(it))
                    }
                }
                Spacer(modifier = Modifier.size(12.dp))
                contributions.take(3).forEach { c ->
                    ContributionRow(c)
                }
            } else {
                Text(
                    text = "Aun no hay contribuciones. Se el primero en compartir como cuidas esta planta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (canContribute) {
                Spacer(modifier = Modifier.size(12.dp))
                Button(
                    onClick = onAddClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Aportar mis cuidados")
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ContributionRow(contribution: CareWikiContribution) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = contribution.authorName?.takeIf { it.isNotBlank() } ?: "Usuario",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        val bits = mutableListOf<String>()
        contribution.wateringDays?.let { bits += "Riega cada $it d" }
        contribution.fertilizeDays?.let { bits += "Abono cada $it d" }
        contribution.lightLevel?.let { bits += "Luz: ${lightLabel(it)}" }
        Text(
            text = bits.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        contribution.notes?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = "\"$it\"",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun lightLabel(key: String): String = when (key.lowercase()) {
    "low" -> "Poca"
    "medium" -> "Media"
    "high" -> "Mucha"
    else -> key
}

/**
 * Dialog para anadir una contribucion. Todos los campos opcionales; al
 * menos uno debe estar relleno.
 */
@Composable
fun AddCareWikiContributionDialog(
    onConfirm: (
        wateringDays: Int?,
        fertilizeDays: Int?,
        lightLevel: String?,
        notes: String?,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    var watering by rememberSaveable { mutableStateOf("") }
    var fertilize by rememberSaveable { mutableStateOf("") }
    var light by rememberSaveable { mutableStateOf<String?>(null) }
    var notes by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aportar cuidados") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Comparte como cuidas tu planta. Al menos un campo es obligatorio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = watering,
                    onValueChange = { watering = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Riega cada N dias") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                )
                OutlinedTextField(
                    value = fertilize,
                    onValueChange = { fertilize = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Abono cada N dias") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                )
                Text(
                    text = "Nivel de luz",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low" to "Poca", "medium" to "Media", "high" to "Mucha").forEach { (key, label) ->
                        FilterChip(
                            selected = light == key,
                            onClick = { light = if (light == key) null else key },
                            label = { Text(label) },
                            leadingIcon = {
                                Icon(Icons.Outlined.LightMode, contentDescription = null)
                            },
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(280) },
                    label = { Text("Notas (opcional)") },
                    minLines = 2,
                    maxLines = 4,
                )
            }
        },
        confirmButton = {
            val anyFilled = watering.isNotBlank() || fertilize.isNotBlank() ||
                light != null || notes.isNotBlank()
            Button(
                enabled = anyFilled,
                onClick = {
                    onConfirm(
                        watering.toIntOrNull(),
                        fertilize.toIntOrNull(),
                        light,
                        notes.trim().ifBlank { null },
                    )
                },
            ) { Text("Publicar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

