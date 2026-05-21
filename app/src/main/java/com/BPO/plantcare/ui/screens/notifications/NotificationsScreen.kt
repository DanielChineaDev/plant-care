package com.BPO.plantcare.ui.screens.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.BPO.plantcare.R
import com.BPO.plantcare.domain.model.AppNotification
import com.BPO.plantcare.domain.model.AppNotificationType
import java.text.DateFormat
import java.util.Date

/**
 * Centro de notificaciones in-app. Lista las notifs ordenadas por fecha,
 * permite marcarlas todas leidas y al tocar una navega al recurso
 * relacionado (post, comunidad, perfil...). El click marca como leida.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onPostClick: (communityId: String, postId: String) -> Unit,
    onCommunityClick: (communityId: String) -> Unit,
    onProfileClick: (uid: String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (notifications.any { !it.read }) {
                        TextButton(onClick = viewModel::markAllAsRead) {
                            Text(stringResource(R.string.notif_mark_all))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (notifications.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(notifications, key = { it.id }) { notif ->
                NotificationRow(
                    notification = notif,
                    onClick = {
                        viewModel.onClick(notif)
                        navigate(notif, onPostClick, onCommunityClick, onProfileClick)
                    },
                )
            }
        }
    }
}

private fun navigate(
    notification: AppNotification,
    onPostClick: (String, String) -> Unit,
    onCommunityClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
) {
    when (notification.type) {
        AppNotificationType.PostLike,
        AppNotificationType.PostComment -> {
            val cid = notification.communityId
            val pid = notification.postId
            if (cid != null && pid != null) onPostClick(cid, pid)
        }
        AppNotificationType.CommunityJoin -> {
            notification.communityId?.let(onCommunityClick)
        }
        AppNotificationType.Unknown -> {
            notification.fromUid?.let(onProfileClick)
        }
    }
}

@Composable
private fun NotificationRow(
    notification: AppNotification,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.read) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconFor(notification.type),
                    contentDescription = null,
                    tint = tintFor(notification.type),
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title(notification),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (notification.read) FontWeight.Normal else FontWeight.SemiBold,
                )
                notification.preview?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                Text(
                    text = formatTime(notification.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (notification.read) {
                Icon(
                    imageVector = Icons.Outlined.Done,
                    contentDescription = stringResource(R.string.notif_read),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = stringResource(R.string.notif_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.notif_empty_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
            )
        }
    }
}

private fun iconFor(type: AppNotificationType): ImageVector = when (type) {
    AppNotificationType.PostLike -> Icons.Outlined.Favorite
    AppNotificationType.PostComment -> Icons.Outlined.ChatBubbleOutline
    AppNotificationType.CommunityJoin -> Icons.Outlined.Groups
    AppNotificationType.Unknown -> Icons.Outlined.Notifications
}

@Composable
private fun tintFor(type: AppNotificationType) = when (type) {
    AppNotificationType.PostLike -> MaterialTheme.colorScheme.error
    AppNotificationType.PostComment -> MaterialTheme.colorScheme.primary
    AppNotificationType.CommunityJoin -> MaterialTheme.colorScheme.tertiary
    AppNotificationType.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun title(n: AppNotification): String {
    val who = n.fromName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.notif_someone)
    return when (n.type) {
        AppNotificationType.PostLike -> stringResource(R.string.notif_like, who)
        AppNotificationType.PostComment -> stringResource(R.string.notif_comment, who)
        AppNotificationType.CommunityJoin -> stringResource(R.string.notif_join, who)
        AppNotificationType.Unknown -> stringResource(R.string.notif_unknown, who)
    }
}

@Composable
private fun formatTime(ts: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - ts
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 1 -> stringResource(R.string.time_now)
        minutes < 60 -> stringResource(R.string.time_min, minutes.toInt())
        hours < 24 -> stringResource(R.string.time_hours, hours.toInt())
        days < 7 -> stringResource(R.string.time_days, days.toInt())
        else -> DateFormat.getDateInstance(DateFormat.SHORT).format(Date(ts))
    }
}
