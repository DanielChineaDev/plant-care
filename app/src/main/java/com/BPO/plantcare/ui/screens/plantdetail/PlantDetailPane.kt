package com.BPO.plantcare.ui.screens.plantdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.BPO.plantcare.ui.navigation.NavArgs
import com.BPO.plantcare.ui.navigation.Routes

/**
 * Panel de detalle para el layout maestro-detalle en pantallas anchas
 * (tablets). Reutiliza [PlantDetailScreen] tal cual hospedandola en un
 * NavHost anidado, de forma que su ViewModel reciba el plantId por el
 * argumento de navegacion (igual que en la navegacion normal del movil),
 * sin necesidad de refactorizar el ViewModel.
 *
 * Si [plantId] es null muestra un placeholder ("selecciona una planta").
 * El boton de volver de la ficha llama a [onClose] (limpia la seleccion).
 * [onPhotoClick] usa el navController EXTERNO (la galeria es una pantalla
 * de pantalla completa fuera del panel).
 */
@Composable
fun PlantDetailPane(
    plantId: Long?,
    onClose: () -> Unit,
    onPhotoClick: (plantId: Long, photoId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (plantId == null) {
        EmptyDetailPane(modifier)
        return
    }
    val nav = rememberNavController()
    LaunchedEffect(plantId) {
        nav.navigate(Routes.plantDetail(plantId)) {
            // Mantenemos "pane_empty" al fondo y reemplazamos el detalle al
            // cambiar de seleccion.
            popUpTo(PANE_EMPTY) { inclusive = false }
            launchSingleTop = true
        }
    }
    NavHost(
        navController = nav,
        startDestination = PANE_EMPTY,
        modifier = modifier,
    ) {
        composable(PANE_EMPTY) { EmptyDetailPane() }
        composable(
            route = Routes.PLANT_DETAIL_PATTERN,
            arguments = listOf(navArgument(NavArgs.PLANT_ID) { type = NavType.LongType }),
        ) {
            PlantDetailScreen(
                onBack = onClose,
                onPhotoClick = onPhotoClick,
            )
        }
    }
}

@Composable
private fun EmptyDetailPane(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Spa,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Selecciona una planta para ver su ficha",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

private const val PANE_EMPTY = "pane_empty"
