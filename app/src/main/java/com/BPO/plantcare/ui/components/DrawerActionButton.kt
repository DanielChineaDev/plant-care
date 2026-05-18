package com.BPO.plantcare.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

/**
 * Boton hamburguesa que abre el drawer lateral. Se usa en el slot `actions`
 * de las TopAppBars de pantallas de detalle (las que ya tienen ArrowBack
 * como navigationIcon) para que el menu lateral siga accesible sin perder
 * el back.
 */
@Composable
fun DrawerActionButton(onOpenDrawer: () -> Unit) {
    IconButton(onClick = onOpenDrawer) {
        Icon(Icons.Outlined.Menu, contentDescription = "Menu")
    }
}
