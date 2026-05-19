package com.BPO.plantcare.ui.screens.communities

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.BPO.plantcare.core.storage.copyUriToCache
import com.BPO.plantcare.domain.model.Community
import com.BPO.plantcare.ui.components.FeedPostCard
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitiesListScreen(
    onBack: () -> Unit,
    onNotificationsClick: () -> Unit,
    onCommunityClick: (String) -> Unit,
    viewModel: CommunitiesListViewModel = hiltViewModel(),
    onPostClick: (communityId: String, postId: String) -> Unit = { _, _ -> },
    onAuthorClick: (uid: String) -> Unit = {},
) {
    val popular by viewModel.popularCommunities.collectAsStateWithLifecycle()
    val others by viewModel.otherCommunities.collectAsStateWithLifecycle()
    val featured by viewModel.featuredPosts.collectAsStateWithLifecycle()
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val msg = when (event) {
                is CommunitiesEvent.Error -> event.message.ifBlank { "Error" }
                is CommunitiesEvent.CommunityCreated -> "Comunidad creada"
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comunidades") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    com.BPO.plantcare.ui.components.NotificationsActionButton(
                        onClick = onNotificationsClick,
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Solo admins ven el boton de crear comunidad.
            if (isSignedIn && isAdmin) {
                FloatingActionButton(onClick = { showCreate = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Crear comunidad")
                }
            }
        },
    ) { padding ->
        if (popular.isEmpty() && others.isEmpty()) {
            EmptyState(modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (popular.isNotEmpty()) {
                item {
                    SectionTitle(
                        text = "Comunidades populares",
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        items(popular, key = { it.id }) { community ->
                            PopularCommunityCard(
                                community = community,
                                canInteract = isSignedIn,
                                onClick = { onCommunityClick(community.id) },
                                onJoinToggle = { viewModel.toggleMembership(community) },
                            )
                        }
                    }
                }
            }

            if (others.isNotEmpty()) {
                item {
                    SectionTitle(
                        text = "Otras comunidades",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                items(others, key = { it.id }) { community ->
                    CommunityRow(
                        community = community,
                        canInteract = isSignedIn,
                        onClick = { onCommunityClick(community.id) },
                        onJoinToggle = { viewModel.toggleMembership(community) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            if (featured.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionTitle(
                        text = "Publicaciones destacadas",
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                items(featured, key = { it.post.id }) { item ->
                    FeedPostCard(
                        post = item.post,
                        communityName = item.community.name,
                        communityEmoji = item.community.emoji,
                        onClick = { onPostClick(item.community.id, item.post.id) },
                        onAuthorClick = onAuthorClick,
                        onLikeClick = { viewModel.toggleLike(item.community.id, item.post.id) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreateCommunityDialog(
            onConfirm = { name, desc, emoji, photoFile ->
                viewModel.createCommunity(name, desc, emoji, photoFile)
                showCreate = false
            },
            onDismiss = { showCreate = false },
        )
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

@Composable
private fun PopularCommunityCard(
    community: Community,
    canInteract: Boolean,
    onClick: () -> Unit,
    onJoinToggle: () -> Unit,
) {
    // Ancho y alto FIJOS para que todas las tarjetas del carrusel midan
    // exactamente lo mismo. Dentro repartimos zonas: foto (16:9), header
    // (titulo + miembros), descripcion (2 lineas) y boton al fondo.
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .width(280.dp)
            .height(320.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Zona foto / placeholder con emoji.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (community.photoUrl != null) {
                    AsyncImage(
                        model = community.photoUrl,
                        contentDescription = community.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = community.emoji, fontSize = 48.sp)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = community.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${community.memberCount} miembros",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = community.description.ifBlank { " " },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                if (canInteract) {
                    if (community.isMember) {
                        OutlinedButton(
                            onClick = onJoinToggle,
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = androidx.compose.ui.graphics.Color(0xFFE53935),
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = androidx.compose.ui.graphics.Color(0xFFE53935),
                            ),
                        ) { Text("Salir") }
                    } else {
                        Button(
                            onClick = onJoinToggle,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Unirme") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityRow(
    community: Community,
    canInteract: Boolean,
    onClick: () -> Unit,
    onJoinToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar circular: foto si la hay, si no emoji con fondo.
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (community.photoUrl != null) {
                    AsyncImage(
                        model = community.photoUrl,
                        contentDescription = community.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(text = community.emoji, fontSize = 28.sp)
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = community.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${community.memberCount} miembros",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (canInteract) {
                if (community.isMember) {
                    OutlinedButton(
                        onClick = onJoinToggle,
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = androidx.compose.ui.graphics.Color(0xFFE53935),
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = androidx.compose.ui.graphics.Color(0xFFE53935),
                        ),
                    ) { Text("Salir") }
                } else {
                    Button(onClick = onJoinToggle) { Text("Unirme") }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Groups,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Aun no hay comunidades",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = "Crea la primera con el boton +.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun CreateCommunityDialog(
    onConfirm: (name: String, description: String, emoji: String, photoFile: File?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var emoji by rememberSaveable { mutableStateOf("🌱") }
    var photoFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) photoFile = copyUriToCache(context, uri)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva comunidad") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Tile clickable para elegir foto: muestra preview o
                // placeholder.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            pickPhoto.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    val current = photoFile
                    if (current != null) {
                        AsyncImage(
                            model = current,
                            contentDescription = "Portada",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Tocar para anadir portada",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = emoji,
                    onValueChange = { if (it.length <= 2) emoji = it },
                    label = { Text("Emoji") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripcion") },
                    minLines = 2,
                    maxLines = 4,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onConfirm(
                        name.trim(),
                        description.trim(),
                        emoji.trim().ifBlank { "🌱" },
                        photoFile,
                    )
                },
            ) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
