package com.BPO.plantcare.ui.screens.postdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.BPO.plantcare.domain.model.Comment
import com.BPO.plantcare.domain.model.CommunityPost
import com.BPO.plantcare.ui.components.MarkdownText
import com.BPO.plantcare.ui.components.PollVoteCard
import com.BPO.plantcare.ui.components.ReportDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    onBack: () -> Unit,
    onAuthorClick: (uid: String) -> Unit,
    onAuthorNameClick: (uid: String) -> Unit,
    viewModel: PostDetailViewModel = hiltViewModel(),
) {
    val post by viewModel.post.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    val currentUid by viewModel.currentUid.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var commentText by rememberSaveable { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val msg = when (event) {
                is PostDetailEvent.Error -> event.message.ifBlank { "Error" }
                PostDetailEvent.CommentPosted -> {
                    commentText = ""
                    "Comentario publicado"
                }
                PostDetailEvent.Reported -> "Gracias, hemos recibido tu reporte"
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (showReportDialog) {
        ReportDialog(
            title = "Reportar publicacion",
            onConfirm = { reason, notes ->
                viewModel.reportPost(reason, notes)
                showReportDialog = false
            },
            onDismiss = { showReportDialog = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Publicación") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (isSignedIn) {
                        IconButton(onClick = { showReportDialog = true }) {
                            Icon(Icons.Outlined.Flag, contentDescription = "Reportar")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (isSignedIn) {
                CommentInput(
                    value = commentText,
                    onChange = { commentText = it },
                    onSend = { viewModel.addComment(commentText) },
                )
            }
        },
    ) { padding ->
        val current = post
        if (current == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PostHeader(
                    post = current,
                    canInteract = isSignedIn,
                    onAuthorClick = { onAuthorClick(current.authorUid) },
                    onAuthorNameClick = { onAuthorNameClick(current.authorUid) },
                    onLikeClick = viewModel::toggleLike,
                    onPollVote = viewModel::voteOnPoll,
                )
            }
            item {
                Text(
                    text = "Comentarios (${current.commentCount})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (comments.isEmpty()) {
                item {
                    Text(
                        text = "Aun no hay comentarios.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(comments, key = { it.id }) { comment ->
                    CommentRow(
                        comment = comment,
                        isMine = comment.authorUid == currentUid,
                        onDelete = { viewModel.deleteComment(comment.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PostHeader(
    post: CommunityPost,
    canInteract: Boolean,
    onAuthorClick: () -> Unit,
    onAuthorNameClick: () -> Unit,
    onLikeClick: () -> Unit,
    onPollVote: (String) -> Unit = {},
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (post.authorPhoto != null) {
                    AsyncImage(
                        model = post.authorPhoto,
                        contentDescription = post.authorName,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onAuthorClick),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onAuthorClick),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onAuthorNameClick),
                ) {
                    Text(
                        text = post.authorName.ifBlank { "Anonimo" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = relativeTime(post.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (post.text.isNotBlank()) {
                Spacer(modifier = Modifier.size(12.dp))
                // Markdown simple: **bold**, *italic* y [texto](url).
                MarkdownText(text = post.text, style = MaterialTheme.typography.bodyLarge)
            }
            post.poll?.let { poll ->
                Spacer(modifier = Modifier.size(12.dp))
                PollVoteCard(
                    poll = poll,
                    myVote = post.myPollVote,
                    enabled = canInteract,
                    onVote = onPollVote,
                )
            }
            if (post.photoUrl != null) {
                Spacer(modifier = Modifier.size(12.dp))
                AsyncImage(
                    model = post.photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.2f)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onLikeClick,
                    enabled = canInteract,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (post.isLikedByMe) Icons.Filled.Favorite
                        else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = if (post.isLikedByMe) androidx.compose.ui.graphics.Color(0xFFE53935)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${post.likeCount}",
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
                    text = "${post.commentCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CommentRow(comment: Comment, isMine: Boolean, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (comment.authorPhoto != null) {
                AsyncImage(
                    model = comment.authorPhoto,
                    contentDescription = comment.authorName,
                    modifier = Modifier.size(28.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp).clip(CircleShape),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.authorName.ifBlank { "Anonimo" },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = relativeTime(comment.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.size(4.dp))
                Text(text = comment.text, style = MaterialTheme.typography.bodyMedium)
            }
            if (isMine) {
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Borrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentInput(value: String, onChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .navigationBarsPadding()
            .imePadding(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text("Escribe un comentario") },
            modifier = Modifier.weight(1f),
            maxLines = 4,
        )
        Spacer(modifier = Modifier.size(8.dp))
        FilledIconButton(
            onClick = onSend,
            enabled = value.isNotBlank(),
        ) {
            Icon(Icons.Outlined.Send, contentDescription = "Enviar")
        }
    }
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
