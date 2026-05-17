package com.BPO.plantcare.ui.screens.myplants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.model.PlantStatus
import com.BPO.plantcare.domain.model.status
import com.BPO.plantcare.ui.theme.StatusHealthy
import com.BPO.plantcare.ui.theme.StatusThirsty
import com.BPO.plantcare.ui.theme.StatusWarning
import java.io.File
import kotlin.math.max

@Composable
fun MyPlantsScreen(
    onPlantClick: (Long) -> Unit,
    onIdentifyClick: () -> Unit,
    viewModel: MyPlantsViewModel = hiltViewModel(),
) {
    val plants by viewModel.plants.collectAsStateWithLifecycle()

    if (plants.isEmpty()) {
        EmptyState(onIdentifyClick = onIdentifyClick)
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(plants, key = { it.id }) { plant ->
                PlantCard(
                    plant = plant,
                    onClick = { onPlantClick(plant.id) },
                    onWaterClick = { viewModel.onWatered(plant.id) },
                )
            }
        }
    }
}

@Composable
private fun PlantCard(plant: Plant, onClick: () -> Unit, onWaterClick: () -> Unit) {
    val status = plant.status()
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                val model = plant.userPhotoPath?.let { File(it) } ?: plant.referenceImageUrl
                if (model != null) {
                    AsyncImage(
                        model = model,
                        contentDescription = plant.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.LocalFlorist,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                StatusBadge(
                    status = status,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                )
            }
            Column(
                modifier = Modifier.padding(12.dp),
            ) {
                Text(
                    text = plant.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = plant.scientificName,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = wateringHint(plant),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(
                        onClick = onWaterClick,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WaterDrop,
                            contentDescription = "Marcar como regada",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: PlantStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        PlantStatus.Healthy -> StatusHealthy
        PlantStatus.Attention -> StatusWarning
        PlantStatus.Thirsty -> StatusThirsty
        PlantStatus.NotWatered -> MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .padding(0.dp),
    ) {
        AssistChip(
            onClick = {},
            enabled = false,
            label = { Text("${status.emoji} ${status.label}") },
            colors = AssistChipDefaults.assistChipColors(
                disabledContainerColor = color.copy(alpha = 0.9f),
                disabledLabelColor = Color.White,
            ),
        )
    }
}

@Composable
private fun EmptyState(onIdentifyClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Spa,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Aún no tienes plantas",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = "Identifica una planta y añadela a tu colección para empezar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        androidx.compose.material3.Button(
            onClick = onIdentifyClick,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Icon(imageVector = Icons.Outlined.CameraAlt, contentDescription = null)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
            Text("Identificar mi primera planta")
        }
    }
}

private const val MS_PER_DAY = 24L * 60L * 60L * 1000L

private fun wateringHint(plant: Plant): String {
    val last = plant.lastWateredAt ?: return "Sin regar aún"
    val now = System.currentTimeMillis()
    val daysSince = ((now - last) / MS_PER_DAY).toInt()
    val daysUntil = plant.wateringIntervalDays - daysSince
    return when {
        daysUntil > 1 -> "En $daysUntil días"
        daysUntil == 1 -> "Mañana"
        daysUntil == 0 -> "Hoy"
        else -> "Retraso ${max(0, -daysUntil)} d"
    }
}
