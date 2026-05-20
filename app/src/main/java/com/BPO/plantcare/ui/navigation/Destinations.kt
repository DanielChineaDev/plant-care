package com.BPO.plantcare.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.ui.graphics.vector.ImageVector
import com.BPO.plantcare.R

/**
 * Destinos del bottom nav (4 pestanas + 1 FAB central).
 *
 * El FAB central (Identificar planta) NO es un destino navegable porque
 * abre la pantalla de identificacion como detalle, no como tab.
 * Calendario, Mi Perfil, Comunidades, Herramientas y Configuracion viven
 * en el drawer lateral.
 */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Home(route = "home", labelRes = R.string.nav_home, icon = Icons.Outlined.Home),
    MyPlants(route = "my_plants", labelRes = R.string.nav_plants, icon = Icons.Outlined.Spa),
    Search(route = "search", labelRes = R.string.nav_search, icon = Icons.Outlined.Search),
    Messages(
        route = "messages",
        labelRes = R.string.nav_messages,
        icon = Icons.Outlined.ChatBubbleOutline,
    ),
}
