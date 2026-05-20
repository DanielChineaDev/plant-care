package com.BPO.plantcare.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * True cuando el ancho de ventana es "Expanded" (tablets en horizontal,
 * plegables abiertos, etc.). Lo provee MainActivity calculando el
 * WindowSizeClass. Las pantallas que soportan maestro-detalle (p. ej.
 * Plantas) lo leen para decidir si pintan una o dos columnas.
 */
val LocalIsExpandedScreen = staticCompositionLocalOf { false }
