package com.BPO.plantcare.ui.screens.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.runtime.Composable
import com.BPO.plantcare.ui.screens.PlaceholderScreen

@Composable
fun HomeScreen() {
    PlaceholderScreen(
        title = "Identifica una planta",
        subtitle = "Saca una foto y descubre la especie, sus cuidados y cómo mantenerla feliz.",
        icon = Icons.Outlined.CameraAlt,
    )
}
