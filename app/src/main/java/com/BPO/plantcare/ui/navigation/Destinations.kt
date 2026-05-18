package com.BPO.plantcare.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Destinos del bottom nav (5 pestanas).
 *
 * Calendario, Mi Perfil, Identificar, Herramientas y Configuracion ya no
 * estan aqui: viven en el drawer lateral.
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home(route = "home", label = "Inicio", icon = Icons.Outlined.Home),
    MyPlants(route = "my_plants", label = "Mis plantas", icon = Icons.Outlined.Spa),
    Communities(route = "communities", label = "Comunidades", icon = Icons.Outlined.Groups),
    Search(route = "search", label = "Buscar", icon = Icons.Outlined.Search),
    Messages(route = "messages", label = "Mensajes", icon = Icons.Outlined.ChatBubbleOutline),
}
