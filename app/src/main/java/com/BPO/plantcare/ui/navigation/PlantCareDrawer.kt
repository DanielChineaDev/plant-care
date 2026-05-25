package com.BPO.plantcare.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.BPO.plantcare.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.BPO.plantcare.domain.model.Community
import com.BPO.plantcare.domain.model.UserProfile

/**
 * Contenido del drawer lateral. Estructura:
 *   - Cabecera con avatar/nombre del usuario logueado.
 *   - "Otras secciones": Mi Perfil, Calendario, Identificar planta, Herramientas.
 *   - "Tus comunidades": lista de las comunidades a las que esta unido.
 *   - Al fondo: Configuracion.
 *
 * Cada item llama a [onNavigate] con la ruta y la pantalla padre se encarga
 * de cerrar el drawer y navegar.
 */
@Composable
fun PlantCareDrawerContent(
    onNavigate: (String) -> Unit,
    viewModel: DrawerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PlantCareDrawerContent(
        profile = state.profile,
        joinedCommunities = state.joinedCommunities,
        onNavigate = onNavigate,
    )
}

@Composable
private fun PlantCareDrawerContent(
    profile: UserProfile?,
    joinedCommunities: List<Community>,
    onNavigate: (String) -> Unit,
) {
    ModalDrawerSheet(
        modifier = Modifier.widthIn(max = 320.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            DrawerHeader(profile = profile)

            HorizontalDivider()

            SectionHeader(text = stringResource(R.string.drawer_section_other))

            DrawerItem(
                icon = Icons.Outlined.Person,
                label = stringResource(R.string.drawer_my_profile),
                onClick = { onNavigate(Routes.MY_PROFILE) },
            )
            DrawerItem(
                icon = Icons.Outlined.Groups,
                label = stringResource(R.string.drawer_communities),
                onClick = { onNavigate(Routes.COMMUNITIES) },
            )
            DrawerItem(
                icon = Icons.Outlined.CalendarMonth,
                label = stringResource(R.string.drawer_calendar),
                onClick = { onNavigate(Routes.CALENDAR) },
            )
            DrawerItem(
                icon = Icons.Outlined.Build,
                label = stringResource(R.string.drawer_tools),
                onClick = { onNavigate(Routes.TOOLS) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader(text = stringResource(R.string.drawer_section_communities))

            if (joinedCommunities.isEmpty()) {
                Text(
                    text = stringResource(R.string.drawer_no_communities),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp),
                )
            } else {
                joinedCommunities.forEach { community ->
                    DrawerCommunityItem(
                        community = community,
                        onClick = { onNavigate(Routes.communityFeed(community.id)) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()

            if (profile?.isAdmin == true) {
                DrawerItem(
                    icon = Icons.Outlined.Shield,
                    label = stringResource(R.string.drawer_moderation),
                    onClick = { onNavigate(Routes.MODERATION) },
                )
            }
            DrawerItem(
                icon = Icons.Outlined.Settings,
                label = stringResource(R.string.drawer_settings),
                onClick = { onNavigate(Routes.SETTINGS) },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DrawerHeader(profile: UserProfile?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(20.dp),
    ) {
        // Marca arriba (logo + nombre).
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(
                    id = com.BPO.plantcare.R.drawable.ic_plantcare_logo,
                ),
                contentDescription = "PlantCare",
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = "PlantCare",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        // Bloque del usuario.
        Row(verticalAlignment = Alignment.CenterVertically) {
            val photo = profile?.photoUrl
            if (photo != null) {
                AsyncImage(
                    model = photo,
                    contentDescription = profile.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile?.displayName ?: stringResource(R.string.user_default),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                profile?.email?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        selected = false,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(),
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@Composable
private fun DrawerCommunityItem(community: Community, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = {
            Text(text = community.emoji, fontSize = 22.sp)
        },
        label = { Text(community.name) },
        selected = false,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(),
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}
