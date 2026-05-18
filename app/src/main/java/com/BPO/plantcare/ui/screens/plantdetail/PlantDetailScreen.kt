package com.BPO.plantcare.ui.screens.plantdetail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.core.content.FileProvider
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.BPO.plantcare.core.storage.copyUriToCache
import com.BPO.plantcare.domain.model.GuideMatch
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.model.PlantPhoto
import com.BPO.plantcare.domain.model.PlantStatus
import com.BPO.plantcare.domain.model.WateringLog
import com.BPO.plantcare.domain.model.status
import com.BPO.plantcare.ui.components.CareGuideCard
import com.BPO.plantcare.ui.components.DrawerActionButton
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
    onOpenDrawer: () -> Unit,
    onPhotoClick: (plantId: Long, photoId: Long) -> Unit,
    viewModel: PlantDetailViewModel = hiltViewModel(),
) {
    val plant by viewModel.plant.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val careGuide by viewModel.careGuide.collectAsStateWithLifecycle()
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
                        message = "Riego eliminado",
                        actionLabel = "Deshacer",
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
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar alias")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Eliminar planta")
                    }
                    DrawerActionButton(onOpenDrawer)
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
            WateringCard(
                plant = current,
                onMarkWatered = viewModel::onMarkWatered,
                onIntervalChange = viewModel::onIntervalChange,
            )
            careGuide?.let { match ->
                CareGuideCard(
                    guide = match.guide,
                    genusApproximation = (match as? GuideMatch.Genus)?.genusName,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            DiaryCard(
                photos = photos,
                onAddPhoto = viewModel::onAddDiaryPhoto,
                onDeletePhoto = viewModel::onDeleteDiaryPhoto,
                onPhotoClick = { photoId ->
                    plant?.id?.let { onPhotoClick(it, photoId) }
                },
            )
            HistoryCard(
                history = history,
                onDeleteLog = { log -> viewModel.onDeleteWateringLog(log) },
            )
            TaxonomyCard(current)
            WikipediaCard(wikipedia, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar planta") },
            text = { Text("Esta accion no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
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
                text = "Cambiar foto de la planta",
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
                Text("Sacar una foto")
            }
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.OutlinedButton(
                onClick = onGallery,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Elegir de la galeria")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeroImage(plant: Plant, onChangePhotoClick: () -> Unit) {
    val model = plant.userPhotoPath?.let { File(it) } ?: plant.referenceImageUrl
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
            Icon(Icons.Outlined.CameraAlt, contentDescription = "Cambiar foto")
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
            label = { Text("${status.emoji} ${status.label}") },
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
                text = "Riego",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Ultimo riego: ${formatDate(plant.lastWateredAt)}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Proximo riego: ${nextWateringHint(plant)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cada", style = MaterialTheme.typography.bodyMedium)
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
                Text("Marcar como regada")
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
            Text("Taxonomia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            plant.family?.let { Text("Familia: $it", style = MaterialTheme.typography.bodyMedium) }
            plant.genus?.let { Text("Genero: $it", style = MaterialTheme.typography.bodyMedium) }
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
                text = "Diario fotografico",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (photos.isEmpty()) {
                Text(
                    text = "Anade fotos cada cierto tiempo para ver el crecimiento de tu planta.",
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
                                            contentDescription = "Anadir foto",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp),
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Anadir",
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
                model = File(photo.path),
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
                    contentDescription = "Eliminar foto",
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
            Text("Historial de riegos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            if (history.isEmpty()) {
                Text(
                    text = "Aun no has registrado ningun riego.",
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
                            contentDescription = "Eliminar riego",
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

@Composable
private fun EditNicknameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar alias") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Como la llamas") },
                singleLine = true,
            )
        },
        confirmButton = { Button(onClick = { onConfirm(text) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
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

private fun formatDate(timestamp: Long?): String {
    if (timestamp == null) return "Sin regar aun"
    val df = DateFormat.getDateInstance(DateFormat.MEDIUM)
    return df.format(Date(timestamp))
}

private fun nextWateringHint(plant: Plant): String {
    val last = plant.lastWateredAt ?: return "cuando la riegues por primera vez"
    val now = System.currentTimeMillis()
    val daysSince = ((now - last) / MS_PER_DAY).toInt()
    val daysUntil = plant.wateringIntervalDays - daysSince
    return when {
        daysUntil > 1 -> "en $daysUntil dias"
        daysUntil == 1 -> "manana"
        daysUntil == 0 -> "hoy"
        else -> "atrasado ${-daysUntil} d"
    }
}

private fun relativeAgo(timestamp: Long): String {
    val days = ((System.currentTimeMillis() - timestamp) / MS_PER_DAY).toInt()
    return when {
        days < 1 -> "Hoy"
        days == 1 -> "Ayer"
        days < 7 -> "Hace $days dias"
        days < 30 -> "Hace ${days / 7} semanas"
        else -> "Hace ${days / 30} meses"
    }
}
