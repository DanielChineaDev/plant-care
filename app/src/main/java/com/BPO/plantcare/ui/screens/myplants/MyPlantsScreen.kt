package com.BPO.plantcare.ui.screens.myplants

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.BPO.plantcare.R
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.model.PlantStatus
import com.BPO.plantcare.domain.model.status
import com.BPO.plantcare.ui.theme.StatusHealthy
import com.BPO.plantcare.ui.theme.StatusThirsty
import com.BPO.plantcare.ui.theme.StatusWarning
import java.io.File
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPlantsScreen(
    onOpenDrawer: () -> Unit,
    onNotificationsClick: () -> Unit,
    onPlantClick: (Long) -> Unit,
    onIdentifyClick: () -> Unit,
    viewModel: MyPlantsViewModel = hiltViewModel(),
) {
    val plants by viewModel.plants.collectAsStateWithLifecycle()
    val grouped by viewModel.groupedPlants.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val selectionMode by viewModel.selectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    // Riega + feedback haptico sutil.
    val waterWithHaptic: (Long) -> Unit = { id ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.onWatered(id)
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                SelectionTopBar(
                    count = selectedIds.size,
                    onClose = viewModel::clearSelection,
                    onWaterAll = viewModel::waterSelected,
                    onDeleteAll = viewModel::deleteSelected,
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.nav_plants)) },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Outlined.Menu, contentDescription = stringResource(R.string.menu))
                        }
                    },
                    actions = {
                        // Toggle lista/grid
                        IconButton(onClick = viewModel::toggleViewMode) {
                            Icon(
                                imageVector = if (viewMode == PlantsViewMode.Grid)
                                    Icons.Outlined.ViewList else Icons.Outlined.GridView,
                                contentDescription = stringResource(R.string.plants_change_view),
                            )
                        }
                        // Ordenar
                        SortMenu(current = filters.sort, onSelect = viewModel::setSort)
                        com.BPO.plantcare.ui.components.NotificationsActionButton(
                            onClick = onNotificationsClick,
                        )
                    },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            PlantsSearchBar(
                query = filters.query,
                onQueryChange = viewModel::onQueryChange,
            )
            PlantsFilterRow(
                selected = filters.filter,
                groupByRoom = filters.groupByRoom,
                onSelect = viewModel::setFilter,
                onToggleGroup = viewModel::toggleGroupByRoom,
            )
            if (plants.isEmpty()) {
                if (filters.query.isBlank() && filters.filter == PlantsFilter.All) {
                    EmptyState(
                        onIdentifyClick = onIdentifyClick,
                        modifier = Modifier,
                    )
                } else {
                    NoResultsState(modifier = Modifier)
                }
            } else {
                val contentPad = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                )
                if (viewMode == PlantsViewMode.List || filters.groupByRoom) {
                    PlantsList(
                        plants = plants,
                        grouped = if (filters.groupByRoom) grouped else null,
                        selectionMode = selectionMode,
                        selectedIds = selectedIds,
                        contentPadding = contentPad,
                        onClick = { plant ->
                            if (selectionMode) viewModel.toggleSelected(plant.id)
                            else onPlantClick(plant.id)
                        },
                        onLongClick = { viewModel.startSelection(it.id) },
                        onWaterClick = { waterWithHaptic(it.id) },
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = contentPad,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(plants, key = { it.id }) { plant ->
                            PlantCard(
                                plant = plant,
                                selected = plant.id in selectedIds,
                                selectionMode = selectionMode,
                                modifier = Modifier.animateItem(),
                                onClick = {
                                    if (selectionMode) viewModel.toggleSelected(plant.id)
                                    else onPlantClick(plant.id)
                                },
                                onLongClick = { viewModel.startSelection(plant.id) },
                                onWaterClick = { waterWithHaptic(plant.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    count: Int,
    onClose: () -> Unit,
    onWaterAll: () -> Unit,
    onDeleteAll: () -> Unit,
) {
    TopAppBar(
        title = { Text(pluralStringResource(R.plurals.plants_selected_count, count, count)) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Outlined.Clear,
                    contentDescription = stringResource(R.string.plants_selection_cancel),
                )
            }
        },
        actions = {
            IconButton(onClick = onWaterAll) {
                Icon(
                    Icons.Outlined.WaterDrop,
                    contentDescription = stringResource(R.string.plants_selection_water),
                )
            }
            IconButton(onClick = onDeleteAll) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.plants_selection_delete),
                    tint = androidx.compose.ui.graphics.Color(0xFFE53935),
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortMenu(current: PlantsSort, onSelect: (PlantsSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.Sort, contentDescription = stringResource(R.string.plants_sort))
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            PlantsSort.entries.forEach { sort ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(stringResource(sort.labelRes)) },
                    onClick = {
                        onSelect(sort)
                        expanded = false
                    },
                    trailingIcon = {
                        if (sort == current) {
                            Icon(Icons.Outlined.Check, contentDescription = null)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PlantsList(
    plants: List<Plant>,
    grouped: Map<String, List<Plant>>?,
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    contentPadding: PaddingValues,
    onClick: (Plant) -> Unit,
    onLongClick: (Plant) -> Unit,
    onWaterClick: (Plant) -> Unit,
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (grouped != null) {
            grouped.forEach { (room, roomPlants) ->
                item(key = "header_$room") {
                    Text(
                        text = room,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }
                items(roomPlants, key = { it.id }) { plant ->
                    PlantListItem(
                        plant = plant,
                        selected = plant.id in selectedIds,
                        selectionMode = selectionMode,
                        modifier = Modifier.animateItem(),
                        onClick = { onClick(plant) },
                        onLongClick = { onLongClick(plant) },
                        onWaterClick = { onWaterClick(plant) },
                    )
                }
            }
        } else {
            items(plants, key = { it.id }) { plant ->
                PlantListItem(
                    plant = plant,
                    selected = plant.id in selectedIds,
                    selectionMode = selectionMode,
                    modifier = Modifier.animateItem(),
                    onClick = { onClick(plant) },
                    onLongClick = { onLongClick(plant) },
                    onWaterClick = { onWaterClick(plant) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PlantListItem(
    plant: Plant,
    selected: Boolean,
    selectionMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onWaterClick: () -> Unit,
) {
    val status = plant.status()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                val img = plant.userPhotoPath ?: plant.referenceImageUrl
                if (img != null) {
                    AsyncImage(
                        model = if (plant.userPhotoPath != null) File(plant.userPhotoPath) else img,
                        contentDescription = plant.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.LocalFlorist, contentDescription = null) }
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plant.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${status.emoji} ${stringResource(status.labelRes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selectionMode) {
                androidx.compose.material3.Checkbox(checked = selected, onCheckedChange = { onClick() })
            } else {
                IconButton(onClick = onWaterClick) {
                    Icon(
                        Icons.Outlined.WaterDrop,
                        contentDescription = stringResource(R.string.water),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlantsSearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.plants_search_placeholder)) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.clear))
                }
            }
        },
        singleLine = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantsFilterRow(
    selected: PlantsFilter,
    groupByRoom: Boolean,
    onSelect: (PlantsFilter) -> Unit,
    onToggleGroup: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == PlantsFilter.All,
            onClick = { onSelect(PlantsFilter.All) },
            label = { Text(stringResource(R.string.filter_all)) },
        )
        FilterChip(
            selected = selected == PlantsFilter.NeedsAttention,
            onClick = { onSelect(PlantsFilter.NeedsAttention) },
            label = { Text(stringResource(R.string.filter_needs_attention)) },
        )
        FilterChip(
            selected = selected == PlantsFilter.Healthy,
            onClick = { onSelect(PlantsFilter.Healthy) },
            label = { Text(stringResource(R.string.filter_healthy)) },
        )
        FilterChip(
            selected = selected == PlantsFilter.NotWatered,
            onClick = { onSelect(PlantsFilter.NotWatered) },
            label = { Text(stringResource(R.string.filter_not_watered)) },
        )
        // Toggle agrupar por habitacion.
        FilterChip(
            selected = groupByRoom,
            onClick = onToggleGroup,
            label = { Text(stringResource(R.string.filter_by_room)) },
            leadingIcon = {
                Icon(Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(18.dp))
            },
        )
        Spacer(modifier = Modifier.size(4.dp))
    }
}

@Composable
private fun NoResultsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.no_results_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = stringResource(R.string.no_results_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PlantCard(
    plant: Plant,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onWaterClick: () -> Unit,
) {
    val status = plant.status()
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = if (selected) {
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            )
        } else CardDefaults.elevatedCardColors(),
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
                            contentDescription = stringResource(R.string.mark_as_watered),
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
    // Pildora custom (no AssistChip): Box con background y clip ambos en
    // el mismo shape. Antes el AssistChip tenia su border interno + clip
    // externo dejaba un contorno con un "pixel" que se veia recortado.
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.95f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = "${status.emoji} ${stringResource(status.labelRes)}",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EmptyState(onIdentifyClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
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
            text = stringResource(R.string.plants_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = stringResource(R.string.plants_empty_subtitle),
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
            Text(stringResource(R.string.plants_empty_cta))
        }
    }
}

private const val MS_PER_DAY = 24L * 60L * 60L * 1000L

@Composable
private fun wateringHint(plant: Plant): String {
    val last = plant.lastWateredAt ?: return stringResource(R.string.watering_never)
    val now = System.currentTimeMillis()
    val daysSince = ((now - last) / MS_PER_DAY).toInt()
    val daysUntil = plant.wateringIntervalDays - daysSince
    return when {
        daysUntil > 1 -> stringResource(R.string.watering_in_days, daysUntil)
        daysUntil == 1 -> stringResource(R.string.watering_tomorrow)
        daysUntil == 0 -> stringResource(R.string.watering_today)
        else -> stringResource(R.string.watering_overdue, max(0, -daysUntil))
    }
}
