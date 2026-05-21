package com.BPO.plantcare.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.sp
import com.BPO.plantcare.R
import com.BPO.plantcare.domain.model.PlantTask
import com.BPO.plantcare.domain.model.PlantTaskType
import com.BPO.plantcare.domain.model.nextDueAt
import java.text.DateFormat
import java.util.Date
import kotlin.math.max

/**
 * Card "Tareas de cuidado" que se renderiza en la ficha de planta.
 * Lista todos los [PlantTaskType] disponibles. Para cada uno muestra
 * el estado (activo / inactivo), su intervalo personalizado y, si esta
 * activo, los botones de "Hecho" y "Editar intervalo".
 *
 * No incluye el riego (Water), que se gestiona en su propia tarjeta
 * dentro de la ficha (legacy).
 */
@Composable
fun PlantTasksCard(
    tasks: List<PlantTask>,
    plantAddedAt: Long,
    onToggle: (type: PlantTaskType, taskId: Long?, enable: Boolean) -> Unit,
    onMarkDone: (taskId: Long) -> Unit,
    onUpdateInterval: (taskId: Long, days: Int) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.tasks_card_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = stringResource(R.string.tasks_card_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.size(12.dp))

            // El riego (Water) lo dejamos fuera: se gestiona en otra card de
            // la ficha de planta (compatibilidad con WateringLog/widget).
            val managed = PlantTaskType.entries.filter { it != PlantTaskType.Water }

            managed.forEachIndexed { index, type ->
                val task = tasks.firstOrNull { it.type == type }
                TaskRow(
                    type = type,
                    task = task,
                    plantAddedAt = plantAddedAt,
                    onToggle = { enable -> onToggle(type, task?.id, enable) },
                    onMarkDone = { task?.id?.let(onMarkDone) },
                    onUpdateInterval = { days -> task?.id?.let { onUpdateInterval(it, days) } },
                )
                if (index != managed.lastIndex) Spacer(modifier = Modifier.size(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskRow(
    type: PlantTaskType,
    task: PlantTask?,
    plantAddedAt: Long,
    onToggle: (enable: Boolean) -> Unit,
    onMarkDone: () -> Unit,
    onUpdateInterval: (Int) -> Unit,
) {
    val enabled = task?.enabled == true
    var showIntervalDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = type.emoji, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(type.labelRes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (enabled && task != null) {
                            stringResource(R.string.tasks_every_days, task.intervalDays, nextDueLabel(task, plantAddedAt))
                        } else stringResource(R.string.tasks_suggested_every, type.defaultIntervalDays),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }

            if (enabled && task != null) {
                Spacer(modifier = Modifier.size(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onMarkDone,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(R.string.tasks_done))
                    }
                    IconButton(onClick = { showIntervalDialog = true }) {
                        Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.tasks_edit_interval))
                    }
                }
            }
        }
    }

    if (showIntervalDialog && task != null) {
        IntervalDialog(
            initial = task.intervalDays,
            type = type,
            onConfirm = {
                onUpdateInterval(it)
                showIntervalDialog = false
            },
            onDismiss = { showIntervalDialog = false },
        )
    }
}

@Composable
private fun IntervalDialog(
    initial: Int,
    type: PlantTaskType,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tasks_frequency_title, stringResource(type.labelRes))) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.tasks_how_many_days),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.size(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { new -> if (new.length <= 4) text = new.filter { it.isDigit() } },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val days = max(1, text.toIntOrNull() ?: type.defaultIntervalDays)
                onConfirm(days)
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun nextDueLabel(task: PlantTask, plantAddedAt: Long): String {
    val now = System.currentTimeMillis()
    val due = task.nextDueAt(plantAddedAt)
    val days = ((due - now) / (24L * 60L * 60L * 1000L)).toInt()
    return when {
        due <= now -> stringResource(R.string.tasks_due_today)
        days == 0 -> stringResource(R.string.tasks_due_today)
        days == 1 -> stringResource(R.string.tasks_due_tomorrow)
        days < 30 -> stringResource(R.string.tasks_due_in_days, days)
        else -> DateFormat.getDateInstance(DateFormat.SHORT).format(Date(due))
    }
}
