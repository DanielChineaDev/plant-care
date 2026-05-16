package com.BPO.plantcare.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home(route = "home", label = "Inicio", icon = Icons.Outlined.Home),
    MyPlants(route = "my_plants", label = "Mis plantas", icon = Icons.Outlined.Spa),
    Calendar(route = "calendar", label = "Calendario", icon = Icons.Outlined.CalendarMonth),
    Search(route = "search", label = "Buscar", icon = Icons.Outlined.Search),
    Profile(route = "profile", label = "Perfil", icon = Icons.Outlined.Person),
}
