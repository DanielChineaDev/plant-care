package com.BPO.plantcare.ui.screens.communityfeed

import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.BPO.plantcare.R
import com.BPO.plantcare.core.storage.copyUriToCache
import com.BPO.plantcare.domain.model.Community
import com.BPO.plantcare.domain.model.CommunityMember
import com.BPO.plantcare.domain.model.CommunityPost
import com.BPO.plantcare.domain.model.PollOption
import com.BPO.plantcare.domain.model.PostTag
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val LIKE_RED = 0xFFE53935

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityFeedScreen(
    onBack: () -> Unit,
    onPostClick: (communityId: String, postId: String) -> Unit,
    onAuthorClick: (uid: String) -> Unit,
    onAuthorNameClick: (uid: String) -> Unit,
    viewModel: CommunityFeedViewModel = hiltViewModel(),
) {
    val community by viewModel.community.collectAsStateWithLifecycle()
    val posts by viewModel.filteredPosts.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val tagFilter by viewModel.tagFilter.collectAsStateWithLifecycle()
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreatePost by remember { mutableStateOf(false) }
    var showEditCommunity by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val errorGeneric = stringResource(R.string.error_generic)
    val postPublished = stringResource(R.string.community_post_published)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val msg = when (event) {
                is FeedEvent.Error -> event.message.ifBlank { errorGeneric }
                FeedEvent.PostCreated -> postPublished
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(community?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (isAdmin && community != null) {
                        IconButton(onClick = { showEditCommunity = true }) {
                            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.community_edit))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 0 && isSignedIn && community?.isMember == true) {
                FloatingActionButton(onClick = { showCreatePost = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.community_new_post))
                }
            }
        },
    ) { padding ->
        val current = community
        if (current == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CoverHero(
                community = current,
                canInteract = isSignedIn,
                onJoinToggle = viewModel::toggleMembership,
            )
            TabRow(selectedTabIndex = selectedTab) {
                listOf(
                    stringResource(R.string.community_tab_posts),
                    stringResource(R.string.community_tab_members),
                    stringResource(R.string.community_tab_about),
                ).forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }
            when (selectedTab) {
                0 -> PostsTab(
                    posts = posts,
                    tagFilter = tagFilter,
                    canInteract = isSignedIn,
                    isMember = current.isMember,
                    isAdmin = isAdmin,
                    onTagFilterChange = viewModel::setTagFilter,
                    onPostClick = { onPostClick(current.id, it.id) },
                    onAuthorClick = { onAuthorClick(it) },
                    onAuthorNameClick = { onAuthorNameClick(it) },
                    onLikeClick = { viewModel.toggleLike(it.id) },
                    onToggleFeatured = { viewModel.toggleFeatured(it) },
                )
                1 -> MembersTab(
                    members = members,
                    isAdmin = isAdmin,
                    onAuthorClick = onAuthorNameClick,
                    onKick = { viewModel.removeMember(it) },
                )
                else -> AboutTab(community = current)
            }
        }
    }

    if (showCreatePost) {
        CreatePostDialog(
            onConfirm = { text, photo, pollOptions, tag ->
                viewModel.createPost(text, photo, pollOptions, tag)
                showCreatePost = false
            },
            onDismiss = { showCreatePost = false },
        )
    }

    val editTarget = community
    if (showEditCommunity && editTarget != null) {
        EditCommunityDialog(
            community = editTarget,
            onConfirm = { name, desc, photo ->
                viewModel.updateCommunity(name, desc, photo)
                showEditCommunity = false
            },
            onDismiss = { showEditCommunity = false },
        )
    }
}

/** Portada hero: foto de fondo (o emoji), nombre, miembros y boton unirse. */
@Composable
private fun CoverHero(
    community: Community,
    canInteract: Boolean,
    onJoinToggle: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
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
                Text(text = community.emoji, fontSize = 64.sp)
            }
        }
        // Scrim para legibilidad del texto sobre la foto.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC000000)),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = community.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = stringResource(R.string.community_members_count, community.memberCount.toInt()),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
            if (canInteract) {
                if (community.isMember) {
                    OutlinedButton(
                        onClick = onJoinToggle,
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White,
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                    ) { Text(stringResource(R.string.leave_action)) }
                } else {
                    Button(onClick = onJoinToggle) { Text(stringResource(R.string.join_action)) }
                }
            }
        }
    }
}

@Composable
private fun PostsTab(
    posts: List<CommunityPost>,
    tagFilter: PostTag?,
    canInteract: Boolean,
    isMember: Boolean,
    isAdmin: Boolean,
    onTagFilterChange: (PostTag?) -> Unit,
    onPostClick: (CommunityPost) -> Unit,
    onAuthorClick: (uid: String) -> Unit,
    onAuthorNameClick: (uid: String) -> Unit,
    onLikeClick: (CommunityPost) -> Unit,
    onToggleFeatured: (CommunityPost) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TagFilterRow(selected = tagFilter, onSelect = onTagFilterChange)
        }
        if (posts.isEmpty()) {
            item {
                EmptyFeed(
                    isSignedIn = canInteract,
                    isMember = isMember,
                    filtered = tagFilter != null,
                )
            }
        } else {
            items(posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    canInteract = canInteract,
                    isAdmin = isAdmin,
                    modifier = Modifier.animateItem(),
                    onClick = { onPostClick(post) },
                    onAuthorClick = { onAuthorClick(post.authorUid) },
                    onAuthorNameClick = { onAuthorNameClick(post.authorUid) },
                    onLikeClick = { onLikeClick(post) },
                    onToggleFeatured = { onToggleFeatured(post) },
                )
            }
        }
    }
}

@Composable
private fun TagFilterRow(
    selected: PostTag?,
    onSelect: (PostTag?) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.community_tag_all)) },
            )
        }
        items(PostTag.entries.toList()) { tag ->
            FilterChip(
                selected = selected == tag,
                onClick = { onSelect(if (selected == tag) null else tag) },
                label = { Text("${tag.emoji} ${stringResource(tag.labelRes)}") },
            )
        }
    }
}

@Composable
private fun PostCard(
    post: CommunityPost,
    canInteract: Boolean,
    isAdmin: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onAuthorNameClick: () -> Unit,
    onLikeClick: () -> Unit,
    onToggleFeatured: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (post.authorPhoto != null) {
                    AsyncImage(
                        model = post.authorPhoto,
                        contentDescription = post.authorName,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onAuthorClick),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onAuthorClick),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onAuthorNameClick),
                ) {
                    Text(
                        text = post.authorName.ifBlank { stringResource(R.string.community_user) },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = relativeTime(post.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (post.featured) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = stringResource(R.string.community_featured),
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (isAdmin) {
                    IconButton(onClick = onToggleFeatured, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (post.featured) Icons.Filled.Star
                            else Icons.Outlined.StarBorder,
                            contentDescription = if (post.featured) stringResource(R.string.community_unfeature)
                            else stringResource(R.string.community_feature),
                            tint = Color(0xFFFFC107),
                        )
                    }
                }
            }
            if (post.tag != null) {
                Spacer(modifier = Modifier.size(8.dp))
                AssistChip(
                    onClick = {},
                    label = { Text("${post.tag.emoji} ${stringResource(post.tag.labelRes)}") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                )
            }
            if (post.text.isNotBlank()) {
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = post.text, style = MaterialTheme.typography.bodyLarge)
            }
            if (post.photoUrl != null) {
                Spacer(modifier = Modifier.size(8.dp))
                AsyncImage(
                    model = post.photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLikeClick()
                    },
                    enabled = canInteract,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (post.isLikedByMe) Icons.Filled.Favorite
                        else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (post.isLikedByMe) stringResource(R.string.community_unlike)
                        else stringResource(R.string.community_like),
                        tint = if (post.isLikedByMe) Color(LIKE_RED)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = post.likeCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.size(16.dp))
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = post.commentCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MembersTab(
    members: List<CommunityMember>,
    isAdmin: Boolean,
    onAuthorClick: (uid: String) -> Unit,
    onKick: (uid: String) -> Unit,
) {
    if (members.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.community_no_members),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(members, key = { it.uid }) { member ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAuthorClick(member.uid) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (member.photoUrl != null) {
                        AsyncImage(
                            model = member.photoUrl,
                            contentDescription = member.name,
                            modifier = Modifier.size(40.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(CircleShape),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = member.name.ifBlank { stringResource(R.string.user_default) },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (member.isCreator) {
                            Text(
                                text = stringResource(R.string.community_founder),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (isAdmin && !member.isCreator) {
                        IconButton(onClick = { onKick(member.uid) }) {
                            Icon(
                                imageVector = Icons.Outlined.PersonRemove,
                                contentDescription = stringResource(R.string.community_expel),
                                tint = Color(LIKE_RED),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutTab(community: Community) {
    val dateFmt = remember { SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "ES")) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (community.description.isNotBlank()) {
            Text(
                text = stringResource(R.string.community_about_description),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = community.description, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = stringResource(R.string.community_members_count, community.memberCount.toInt()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (community.createdAt > 0) {
            Text(
                text = stringResource(R.string.community_created_on, dateFmt.format(Date(community.createdAt))),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyFeed(isSignedIn: Boolean, isMember: Boolean, filtered: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (filtered) stringResource(R.string.feed_empty_filtered_title)
                else stringResource(R.string.feed_empty_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            val sub = when {
                filtered -> stringResource(R.string.feed_empty_filtered_sub)
                !isSignedIn -> stringResource(R.string.feed_empty_signin)
                !isMember -> stringResource(R.string.feed_empty_join)
                else -> stringResource(R.string.feed_empty_first)
            }
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CreatePostDialog(
    onConfirm: (
        text: String,
        photoFile: File?,
        pollOptions: List<PollOption>?,
        tag: PostTag?,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    var photoFile by remember { mutableStateOf<File?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var pollMode by rememberSaveable { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf<PostTag?>(null) }
    val pollOptionsState = rememberSaveable(
        saver = androidx.compose.runtime.saveable.listSaver(
            save = { it.toList() },
            restore = { it.toMutableList() },
        ),
    ) { mutableListOf("", "") }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val file = pendingCameraFile
        if (success && file != null) photoFile = file
        pendingCameraFile = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) photoFile = copyUriToCache(context, uri)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.community_new_post)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.post_make_poll),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    androidx.compose.material3.Switch(
                        checked = pollMode,
                        onCheckedChange = { pollMode = it },
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = {
                        Text(
                            if (pollMode) stringResource(R.string.post_question)
                            else stringResource(R.string.post_what),
                        )
                    },
                    minLines = if (pollMode) 1 else 3,
                    maxLines = 6,
                )
                // Selector de etiqueta/categoria.
                Text(text = stringResource(R.string.post_tag), style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedTag == null,
                            onClick = { selectedTag = null },
                            label = { Text(stringResource(R.string.post_tag_none)) },
                        )
                    }
                    items(PostTag.entries.toList()) { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = {
                                selectedTag = if (selectedTag == tag) null else tag
                            },
                            label = { Text("${tag.emoji} ${stringResource(tag.labelRes)}") },
                        )
                    }
                }
                if (pollMode) {
                    Text(text = stringResource(R.string.post_options), style = MaterialTheme.typography.labelLarge)
                    pollOptionsState.forEachIndexed { index, opt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedTextField(
                                value = opt,
                                onValueChange = { pollOptionsState[index] = it.take(80) },
                                label = { Text(stringResource(R.string.post_option_n, index + 1)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            if (pollOptionsState.size > 2) {
                                IconButton(onClick = { pollOptionsState.removeAt(index) }) {
                                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.post_remove_option))
                                }
                            }
                        }
                    }
                    if (pollOptionsState.size < 4) {
                        TextButton(onClick = { pollOptionsState.add("") }) {
                            Text(stringResource(R.string.post_add_option))
                        }
                    }
                }
                if (!pollMode && photoFile != null) {
                    Box {
                        AsyncImage(
                            model = photoFile,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.4f)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                        IconButton(
                            onClick = { photoFile = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(32.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.remove_photo),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                if (!pollMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val file = File(
                                    context.cacheDir,
                                    "post_${System.currentTimeMillis()}.jpg",
                                ).apply { createNewFile() }
                                pendingCameraFile = file
                                val uri: Uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file,
                                )
                                cameraLauncher.launch(uri)
                            },
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(stringResource(R.string.camera))
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                                    ),
                                )
                            },
                        ) {
                            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(stringResource(R.string.gallery))
                        }
                    }
                }
            }
        },
        confirmButton = {
            val validOptions = pollOptionsState.map { it.trim() }.filter { it.isNotEmpty() }
            val pollValid = pollMode && validOptions.size >= 2 && text.isNotBlank()
            val normalValid = !pollMode && (text.isNotBlank() || photoFile != null)
            Button(
                enabled = pollValid || normalValid,
                onClick = {
                    val options = if (pollMode) {
                        validOptions.mapIndexed { idx, t -> PollOption(id = "opt_$idx", text = t) }
                    } else null
                    onConfirm(text, if (pollMode) null else photoFile, options, selectedTag)
                },
            ) { Text(stringResource(R.string.publish)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun EditCommunityDialog(
    community: Community,
    onConfirm: (name: String, description: String, photoFile: File?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(community.name) }
    var description by rememberSaveable { mutableStateOf(community.description) }
    var photoFile by remember { mutableStateOf<File?>(null) }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) photoFile = copyUriToCache(context, uri)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.community_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    val preview = photoFile
                    when {
                        preview != null -> AsyncImage(
                            model = preview,
                            contentDescription = stringResource(R.string.community_cover),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        community.photoUrl != null -> AsyncImage(
                            model = community.photoUrl,
                            contentDescription = stringResource(R.string.community_cover),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Image, contentDescription = null)
                            Text(
                                text = stringResource(R.string.change_cover),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.field_name)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.field_description)) },
                    minLines = 2,
                    maxLines = 5,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim(), description.trim(), photoFile) },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private const val MS_PER_MIN = 60_000L

private fun relativeTime(timestamp: Long): String {
    if (timestamp <= 0) return "ahora"
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / MS_PER_MIN
    return when {
        minutes < 1 -> "ahora"
        minutes < 60 -> "hace $minutes min"
        minutes < 60 * 24 -> "hace ${minutes / 60} h"
        else -> "hace ${minutes / (60 * 24)} d"
    }
}
