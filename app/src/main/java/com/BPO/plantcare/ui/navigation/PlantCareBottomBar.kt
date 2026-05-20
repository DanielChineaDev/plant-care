package com.BPO.plantcare.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Bottom nav custom de 5 slots:
 *  - 4 destinos navegables (Home, MyPlants, Search, Messages).
 *  - 1 FAB grande central para "Identificar planta".
 *
 * Aplica windowInsetsPadding(navigationBars) para no quedar tapado por
 * la barra de gestos / botones del sistema. El FAB queda DENTRO de los
 * limites del Surface (sin offset negativo) para que no se recorte y
 * destaca por color + tamano.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantCareBottomBar(
    navController: NavHostController,
    counts: BottomBarCounts,
    onIdentifyClick: () -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Respetamos la barra de navegacion del sistema (gestos
                // o botones de Android) para que el contenido no quede
                // tapado por el handler de gestos.
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(68.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            val destinations = TopLevelDestination.entries
            // 5 slots: 0,1 normales | 2 = FAB Identify | 3,4 normales
            destinations.take(2).forEach { dest ->
                NavSlot(
                    destination = dest,
                    selected = backStackEntry?.destination?.hierarchy
                        ?.any { it.route == dest.route } == true,
                    showBadgeDot = badgeFor(dest, counts),
                    onClick = { navigateTo(navController, currentRoute, dest) },
                )
            }
            IdentifyFab(onClick = onIdentifyClick)
            destinations.drop(2).forEach { dest ->
                NavSlot(
                    destination = dest,
                    selected = backStackEntry?.destination?.hierarchy
                        ?.any { it.route == dest.route } == true,
                    showBadgeDot = badgeFor(dest, counts),
                    onClick = { navigateTo(navController, currentRoute, dest) },
                )
            }
        }
    }
}

@Composable
private fun NavSlot(
    destination: TopLevelDestination,
    selected: Boolean,
    showBadgeDot: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BadgedBox(
            badge = { if (showBadgeDot) Badge() },
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                tint = tint,
            )
        }
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/**
 * FAB central de "Identificar planta". Destaca por tamano (60dp) y color
 * primary, sin necesitar offset negativo que se recortaria con el clip
 * del Surface padre.
 */
@Composable
private fun IdentifyFab(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 6.dp,
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.CameraAlt,
                contentDescription = "Identificar planta",
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

private fun navigateTo(
    navController: NavHostController,
    currentRoute: String?,
    destination: TopLevelDestination,
) {
    if (currentRoute == destination.route) return
    // Grafo plano: todas las pantallas comparten Home como start destination.
    // Por eso NO usamos saveState/restoreState: en un grafo plano la pila
    // que se hace popUp queda guardada bajo la clave del start destination y
    // restoreState la vuelve a restaurar, dejandote en la pantalla anterior
    // (p. ej. Comunidades) en vez de en el tab pulsado.
    //
    // En su lugar limpiamos la pila hasta la raiz (sin incluirla) y navegamos
    // al destino: estemos donde estemos (detalle, comunidades, etc.) el tab
    // siempre nos lleva a su seccion.
    navController.navigate(destination.route) {
        popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
        launchSingleTop = true
    }
}

private fun badgeFor(destination: TopLevelDestination, counts: BottomBarCounts): Boolean =
    when (destination) {
        TopLevelDestination.MyPlants -> counts.plantsNeedAttention
        else -> false
    }

// Mantenemos imports usados explicitamente.
@Suppress("unused")
private val UNUSED_VECTOR_GUARD: ImageVector = Icons.Outlined.CameraAlt
