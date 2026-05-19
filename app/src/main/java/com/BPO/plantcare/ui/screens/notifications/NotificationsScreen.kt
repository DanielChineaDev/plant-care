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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                title = { Text("Notificaciones") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (notifications.any { !it.read }) {
                        TextButton(onClick = viewModel::markAllAsRead) {
                            Text("Marcar todas")
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
                    contentDescription = "Leida",
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
                text = "Sin notificaciones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Te avisaremos cuando alguien interactue con tus publicaciones o se una a tus comunidades.",
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

private fun title(n: AppNotification): String {
    val who = n.fromName?.takeIf { it.isNotBlank() } ?: "Alguien"
    return when (n.type) {
        AppNotificationType.PostLike -> "$who le ha dado like a tu publicacion"
        AppNotificationType.PostComment -> "$who ha comentado tu publicacion"
        AppNotificationType.CommunityJoin -> "$who se ha unido a tu comunidad"
        AppNotificationType.Unknown -> "$who hizo algo"
    }
}

private fun formatTime(ts: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - ts
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 1 -> "ahora"
        minutes < 60 -> "hace ${minutes} min"
        hours < 24 -> "hace ${hours} h"
        days < 7 -> "hace ${days} d"
        else -> DateFormat.getDateInstance(DateFormat.SHORT).format(Date(ts))
    }
}
