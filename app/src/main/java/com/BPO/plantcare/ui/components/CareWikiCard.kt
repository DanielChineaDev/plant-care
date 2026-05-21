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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.BPO.plantcare.R
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
    isAdmin: Boolean = false,
    onApproveToggle: (CareWikiContribution) -> Unit = {},
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
                    text = stringResource(R.string.cw_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = stringResource(R.string.cw_count, aggregate.contributionCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.size(12.dp))

            if (aggregate.contributionCount > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    aggregate.medianWateringDays?.let {
                        StatChip(label = stringResource(R.string.cw_water_median), value = stringResource(R.string.cw_days_short, it))
                    }
                    aggregate.medianFertilizeDays?.let {
                        StatChip(label = stringResource(R.string.cw_fertilize_median), value = stringResource(R.string.cw_days_short, it))
                    }
                    aggregate.majorityLightLevel?.let {
                        StatChip(label = stringResource(R.string.cw_light), value = lightLabel(it))
                    }
                }
                Spacer(modifier = Modifier.size(12.dp))
                contributions.take(3).forEach { c ->
                    ContributionRow(
                        contribution = c,
                        isAdmin = isAdmin,
                        onApproveToggle = { onApproveToggle(c) },
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.cw_empty),
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
                    Text(stringResource(R.string.cw_contribute))
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
private fun ContributionRow(
    contribution: CareWikiContribution,
    isAdmin: Boolean,
    onApproveToggle: () -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = contribution.authorName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.cw_user),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (contribution.approved) {
                Text(
                    text = stringResource(R.string.cw_verified),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        val bits = mutableListOf<String>()
        contribution.wateringDays?.let { bits += stringResource(R.string.cw_water_every, it) }
        contribution.fertilizeDays?.let { bits += stringResource(R.string.cw_fertilize_every, it) }
        contribution.lightLevel?.let { bits += stringResource(R.string.cw_light_value, lightLabel(it)) }
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
        if (isAdmin) {
            androidx.compose.material3.TextButton(
                onClick = onApproveToggle,
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Text(
                    if (contribution.approved) stringResource(R.string.cw_unverify)
                    else stringResource(R.string.cw_verify),
                )
            }
        }
    }
}

@Composable
private fun lightLabel(key: String): String = when (key.lowercase()) {
    "low" -> stringResource(R.string.cw_light_low)
    "medium" -> stringResource(R.string.cw_light_medium)
    "high" -> stringResource(R.string.cw_light_high)
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
        title = { Text(stringResource(R.string.cw_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.cw_add_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = watering,
                    onValueChange = { watering = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text(stringResource(R.string.cw_water_label)) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                )
                OutlinedTextField(
                    value = fertilize,
                    onValueChange = { fertilize = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text(stringResource(R.string.cw_fertilize_label)) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                )
                Text(
                    text = stringResource(R.string.cw_light_level),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "low" to stringResource(R.string.cw_light_low),
                        "medium" to stringResource(R.string.cw_light_medium),
                        "high" to stringResource(R.string.cw_light_high),
                    ).forEach { (key, label) ->
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
                    label = { Text(stringResource(R.string.report_notes)) },
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
            ) { Text(stringResource(R.string.cw_publish)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

