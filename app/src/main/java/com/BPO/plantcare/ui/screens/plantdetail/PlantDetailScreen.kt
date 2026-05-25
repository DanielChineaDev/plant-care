package com.BPO.plantcare.ui.screens.plantdetail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.BPO.plantcare.R
import com.BPO.plantcare.core.storage.copyUriToCache
import com.BPO.plantcare.domain.model.GuideMatch
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.model.PlantPhoto
import com.BPO.plantcare.domain.model.PlantStatus
import com.BPO.plantcare.domain.model.WateringLog
import com.BPO.plantcare.domain.model.imageModel
import com.BPO.plantcare.domain.model.photoModel
import com.BPO.plantcare.domain.model.status
import com.BPO.plantcare.ui.components.AddCareWikiContributionDialog
import com.BPO.plantcare.ui.components.CareGuideCard
import com.BPO.plantcare.ui.components.CareWikiCard
import com.BPO.plantcare.ui.components.PlantTasksCard
import com.BPO.plantcare.ui.components.WateringHistoryChart
import com.BPO.plantcare.ui.components.WikipediaCard
import com.BPO.plantcare.ui.theme.StatusHealthy
import com.BPO.plantcare.ui.theme.StatusThirsty
import com.BPO.plantcare.ui.theme.StatusWarning
import java.io.File
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    onBack: () -> Unit,
    onPhotoClick: (plantId: Long, photoId: Long) -> Unit,
    viewModel: PlantDetailViewModel = hiltViewModel(),
) {
    val plant by viewModel.plant.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val careGuide by viewModel.careGuide.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val wikiContributions by viewModel.wikiContributions.collectAsStateWithLifecycle()
    val wikiAggregate by viewModel.wikiAggregate.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    var showWikiDialog by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val wikipedia by viewModel.wikipedia.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var showChangePhotoSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val file = pendingCameraFile
        if (success && file != null) viewModel.onChangeMainPhoto(file)
        pendingCameraFile = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val file = com.BPO.plantcare.core.storage.copyUriToCache(context, uri)
            viewModel.onChangeMainPhoto(file)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PlantDetailEvent.Deleted -> onBack()
                is PlantDetailEvent.WateringLogDeleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.pd_watering_log_deleted),
                        actionLabel = context.getString(R.string.pd_undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDeleteWateringLog(event.log)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plant?.displayName.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    plant?.let { p ->
                        IconButton(
                            onClick = {
                                com.BPO.plantcare.core.share.ShareImageGenerator.shareAsImage(
                                    context, p,
                                )
                            },
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.pd_share))
                        }
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.pd_edit_alias))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.pd_delete_plant))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val current = plant
        if (current == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            HeroImage(current, onChangePhotoClick = { showChangePhotoSheet = true })
            TitleSection(current)

            val tabs = listOf(
                stringResource(R.string.pd_tab_care),
                stringResource(R.string.pd_tab_diary),
                stringResource(R.string.pd_tab_history),
                stringResource(R.string.pd_tab_notes),
            )
            androidx.compose.material3.TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    androidx.compose.material3.Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            when (selectedTab) {
                0 -> { // Cuidados
                    WateringCard(
                        plant = current,
                        onMarkWatered = viewModel::onMarkWatered,
                        onIntervalChange = viewModel::onIntervalChange,
                    )
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        PlantTasksCard(
                            tasks = tasks,
                            plantAddedAt = current.addedAt,
                            onToggle = viewModel::onToggleTask,
                            onMarkDone = viewModel::onMarkTaskDone,
                            onUpdateInterval = viewModel::onUpdateTaskInterval,
                        )
                    }
                    careGuide?.let { match ->
                        CareGuideCard(
                            guide = match.guide,
                            genusApproximation = (match as? GuideMatch.Genus)?.genusName,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    TaxonomyCard(current)
                    CareWikiCard(
                        aggregate = wikiAggregate,
                        contributions = wikiContributions,
                        canContribute = true,
                        onAddClick = { showWikiDialog = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        isAdmin = isAdmin,
                        onApproveToggle = viewModel::toggleWikiApproval,
                    )
                    WikipediaCard(
                        wikipedia,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                1 -> { // Diario
                    DiaryCard(
                        photos = photos,
                        onAddPhoto = viewModel::onAddDiaryPhoto,
                        onDeletePhoto = viewModel::onDeleteDiaryPhoto,
                        onPhotoClick = { photoId ->
                            plant?.id?.let { onPhotoClick(it, photoId) }
                        },
                    )
                }
                2 -> { // Historial
                    WateringHistoryChart(
                        history = history,
                        suggestedIntervalDays = current.wateringIntervalDays,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    HistoryCard(
                        history = history,
                        onDeleteLog = { log -> viewModel.onDeleteWateringLog(log) },
                    )
                }
                3 -> { // Notas
                    NotesAndRoomCard(
                        notes = current.notes.orEmpty(),
                        room = current.room.orEmpty(),
                        onNotesChange = viewModel::onNotesChange,
                        onRoomChange = viewModel::onRoomChange,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showEditDialog) {
        EditNicknameDialog(
            current = plant?.nickname.orEmpty(),
            onConfirm = { newName ->
                viewModel.onNicknameChange(newName)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false },
        )
    }

    if (showWikiDialog) {
        AddCareWikiContributionDialog(
            onConfirm = { water, fert, light, notes ->
                viewModel.addWikiContribution(water, fert, light, notes)
                showWikiDialog = false
            },
            onDismiss = { showWikiDialog = false },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.pd_delete_title)) },
            text = { Text(stringResource(R.string.pd_delete_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showChangePhotoSheet) {
        ChangePhotoSheet(
            onCamera = {
                showChangePhotoSheet = false
                val file = File(
                    context.cacheDir,
                    "capture_${System.currentTimeMillis()}.jpg",
                ).apply { createNewFile() }
                pendingCameraFile = file
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
                cameraLauncher.launch(uri)
            },
            onGallery = {
                showChangePhotoSheet = false
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onDismiss = { showChangePhotoSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePhotoSheet(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.pd_change_photo_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Button(
                onClick = onCamera,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.pd_take_photo))
            }
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.OutlinedButton(
                onClick = onGallery,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.pd_choose_gallery))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeroImage(plant: Plant, onChangePhotoClick: () -> Unit) {
    val model = plant.photoModel()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.4f),
        contentAlignment = Alignment.Center,
    ) {
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
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        SmallFloatingActionButton(
            onClick = onChangePhotoClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Icon(Icons.Outlined.CameraAlt, contentDescription = stringResource(R.string.pd_change_photo))
        }
    }
}

@Composable
private fun TitleSection(plant: Plant) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = plant.displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (plant.scientificName != plant.displayName) {
            Text(
                text = plant.scientificName,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val status = plant.status()
        AssistChip(
            onClick = {},
            enabled = false,
            label = { Text("${status.emoji} ${androidx.compose.ui.res.stringResource(status.labelRes)}") },
            colors = AssistChipDefaults.assistChipColors(
                disabledContainerColor = statusColor(status).copy(alpha = 0.9f),
                disabledLabelColor = Color.White,
            ),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun WateringCard(
    plant: Plant,
    onMarkWatered: () -> Unit,
    onIntervalChange: (Int) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.pd_watering),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.pd_last_watering, formatDate(plant.lastWateredAt)), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(R.string.pd_next_watering, nextWateringHint(plant)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.pd_every), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = { onIntervalChange(plant.wateringIntervalDays - 1) },
                    modifier = Modifier.size(36.dp),
                ) { Text("-", style = MaterialTheme.typography.titleMedium) }
                Text(
                    text = "${plant.wateringIntervalDays} dias",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                FilledTonalIconButton(
                    onClick = { onIntervalChange(plant.wateringIntervalDays + 1) },
                    modifier = Modifier.size(36.dp),
                ) { Text("+", style = MaterialTheme.typography.titleMedium) }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onMarkWatered, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.WaterDrop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.mark_as_watered))
            }
        }
    }
}

@Composable
private fun TaxonomyCard(plant: Plant) {
    if (plant.family.isNullOrBlank() && plant.genus.isNullOrBlank()) return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.pd_taxonomy), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            plant.family?.let { Text(stringResource(R.string.pd_family, it), style = MaterialTheme.typography.bodyMedium) }
            plant.genus?.let { Text(stringResource(R.string.pd_genus, it), style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun DiaryCard(
    photos: List<PlantPhoto>,
    onAddPhoto: (File) -> Unit,
    onDeletePhoto: (Long) -> Unit,
    onPhotoClick: (Long) -> Unit,
) {
    val context = LocalContext.current
    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val file = copyUriToCache(context, uri)
            onAddPhoto(file)
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.pd_diary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (photos.isEmpty()) {
                Text(
                    text = stringResource(R.string.pd_diary_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 4.dp),
            ) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .padding(0.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            ElevatedCard(
                                onClick = {
                                    pickMedia.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                                        ),
                                    )
                                },
                                modifier = Modifier.size(100.dp),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Icon(
                                            Icons.Outlined.AddAPhoto,
                                            contentDescription = stringResource(R.string.pd_add_photo),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp),
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            stringResource(R.string.pd_add),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
                items(photos, key = { it.id }) { photo ->
                    DiaryThumbnail(
                        photo = photo,
                        onClick = { onPhotoClick(photo.id) },
                        onDelete = { onDeletePhoto(photo.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DiaryThumbnail(
    photo: PlantPhoto,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(100.dp)) {
            AsyncImage(
                model = photo.imageModel(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onClick),
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp),
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.pd_delete_photo),
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(50))
                        .padding(2.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatShortDate(photo.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatShortDate(timestamp: Long): String {
    val df = DateFormat.getDateInstance(DateFormat.SHORT)
    return df.format(Date(timestamp))
}

@Composable
private fun HistoryCard(history: List<WateringLog>, onDeleteLog: (WateringLog) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.pd_history), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            if (history.isEmpty()) {
                Text(
                    text = stringResource(R.string.pd_no_history),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            val visible = history.take(MAX_VISIBLE_LOGS)
            visible.forEach { log ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WaterDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(formatDate(log.timestamp), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = relativeAgo(log.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onDeleteLog(log) }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.pd_delete_watering),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            if (history.size > MAX_VISIBLE_LOGS) {
                Text(
                    text = "Y ${history.size - MAX_VISIBLE_LOGS} mas...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Tags de ubicacion predefinidos + etiqueta para el campo personalizado. */
private val PRESET_ROOM_TAGS = listOf(
    "🛋️ Salón",
    "🍳 Cocina",
    "🛏️ Dormitorio",
    "🚿 Baño",
    "🌿 Terraza",
    "🌻 Jardín",
    "💼 Oficina",
    "🪟 Balcón",
)

/**
 * Tab "Notas": ubicacion con tags preestablecidos y nota libre tipo sticky
 * note. La visibilidad publica de notas/diario se gestiona de forma
 * centralizada en Ajustes -> Mi perfil.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun NotesAndRoomCard(
    notes: String,
    room: String,
    onNotesChange: (String) -> Unit,
    onRoomChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var notesText by rememberSaveable(notes) { mutableStateOf(notes) }
    // Si el valor actual no esta entre los presets, es un tag personalizado.
    val isPreset = room.isEmpty() || PRESET_ROOM_TAGS.contains(room)
    var customTag by rememberSaveable { mutableStateOf(if (isPreset) "" else room) }
    var showCustomInput by rememberSaveable { mutableStateOf(!isPreset) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // ---- Ubicacion con tags ----
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.pd_location),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.pd_location_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Grid de chips preestablecidos
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PRESET_ROOM_TAGS.forEach { tag ->
                        FilterChip(
                            selected = room == tag,
                            onClick = {
                                showCustomInput = false
                                onRoomChange(if (room == tag) "" else tag)
                            },
                            label = { Text(tag) },
                        )
                    }
                    // Chip "Otro" para tag personalizado
                    FilterChip(
                        selected = showCustomInput,
                        onClick = { showCustomInput = !showCustomInput },
                        label = { Text("✏️ Otro") },
                    )
                }
                // Input personalizado (visible solo si se pulso "Otro")
                if (showCustomInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customTag,
                        onValueChange = { customTag = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.pd_location_hint)) },
                        trailingIcon = {
                            if (customTag.isNotBlank() && customTag != room) {
                                TextButton(onClick = { onRoomChange(customTag) }) {
                                    Text(stringResource(R.string.save))
                                }
                            }
                        },
                    )
                }
                // Mostrar tag seleccionado si es preset
                if (!showCustomInput && room.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { onRoomChange("") },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        Text(
                            stringResource(R.string.pd_location_clear),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        // ---- Notas estilo sticky note ----
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                containerColor = androidx.compose.ui.graphics.Color(0xFFFFF59D),
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.pd_my_notes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = androidx.compose.ui.graphics.Color(0xFF5D4037),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 10,
                    placeholder = { Text(stringResource(R.string.pd_notes_hint)) },
                )
                if (notesText != notes) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onNotesChange(notesText) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.pd_save_note)) }
                }
            }
        }
    }
}

@Composable
private fun EditNicknameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pd_edit_alias)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.pd_alias_hint)) },
                singleLine = true,
            )
        },
        confirmButton = { Button(onClick = { onConfirm(text) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private const val MAX_VISIBLE_LOGS = 10
private const val MS_PER_DAY = 24L * 60L * 60L * 1000L

private fun statusColor(status: PlantStatus): Color = when (status) {
    PlantStatus.Healthy -> StatusHealthy
    PlantStatus.Attention -> StatusWarning
    PlantStatus.Thirsty -> StatusThirsty
    PlantStatus.NotWatered -> Color(0xFF2E7D32)
}

@Composable
private fun formatDate(timestamp: Long?): String {
    if (timestamp == null) return stringResource(R.string.watering_never)
    val df = DateFormat.getDateInstance(DateFormat.MEDIUM)
    return df.format(Date(timestamp))
}

@Composable
private fun nextWateringHint(plant: Plant): String {
    val last = plant.lastWateredAt ?: return stringResource(R.string.pd_next_never)
    val now = System.currentTimeMillis()
    val daysSince = ((now - last) / MS_PER_DAY).toInt()
    val daysUntil = plant.wateringIntervalDays - daysSince
    return when {
        daysUntil > 1 -> stringResource(R.string.pd_next_in_days, daysUntil)
        daysUntil == 1 -> stringResource(R.string.pd_next_tomorrow)
        daysUntil == 0 -> stringResource(R.string.pd_next_today)
        else -> stringResource(R.string.pd_next_overdue, -daysUntil)
    }
}

@Composable
private fun relativeAgo(timestamp: Long): String {
    val days = ((System.currentTimeMillis() - timestamp) / MS_PER_DAY).toInt()
    return when {
        days < 1 -> stringResource(R.string.time_today)
        days == 1 -> stringResource(R.string.time_yesterday)
        days < 7 -> stringResource(R.string.time_days_ago_full, days)
        days < 30 -> stringResource(R.string.time_weeks_ago, days / 7)
        else -> stringResource(R.string.time_months_ago, days / 30)
    }
}
