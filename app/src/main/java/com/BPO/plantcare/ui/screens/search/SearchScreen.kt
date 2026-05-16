package com.BPO.plantcare.ui.screens.search

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import com.BPO.plantcare.ui.screens.PlaceholderScreen

@Composable
fun SearchScreen() {
    PlaceholderScreen(
        title = "Catálogo de plantas",
        subtitle = "Explora especies por origen, dificultad, luz y mucho más.",
        icon = Icons.Outlined.Search,
    )
}
