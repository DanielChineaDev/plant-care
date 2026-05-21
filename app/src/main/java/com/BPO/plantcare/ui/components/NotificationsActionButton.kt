package com.BPO.plantcare.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.BPO.plantcare.R

/**
 * Icono campana con badge de no leidas para usar en `actions` de las
 * TopAppBars de las pantallas top-level (Inicio, Plantas, Comunidad,
 * Mensajes). Hace tap -> [onClick] que navega al centro de notifs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsActionButton(
    onClick: () -> Unit,
    viewModel: NotificationsBadgeViewModel = hiltViewModel(),
) {
    val unread by viewModel.unreadCount.collectAsStateWithLifecycle()
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (unread > 0) {
                    Badge { Text(if (unread > 99) "99+" else unread.toString()) }
                }
            },
        ) {
            Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.notifications_title))
        }
    }
}
