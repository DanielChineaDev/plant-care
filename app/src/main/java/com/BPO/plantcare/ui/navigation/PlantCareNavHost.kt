package com.BPO.plantcare.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.BPO.plantcare.ui.screens.calendar.CalendarScreen
import com.BPO.plantcare.ui.screens.catalogdetail.CatalogPlantDetailScreen
import com.BPO.plantcare.ui.screens.home.HomeScreen
import com.BPO.plantcare.ui.screens.identify.IdentifyScreen
import com.BPO.plantcare.ui.screens.myplants.MyPlantsScreen
import com.BPO.plantcare.ui.screens.photoviewer.PhotoViewerScreen
import com.BPO.plantcare.ui.screens.plantdetail.PlantDetailScreen
import com.BPO.plantcare.ui.screens.profile.ProfileScreen
import com.BPO.plantcare.ui.screens.search.SearchScreen

object Routes {
    const val IDENTIFY = "identify"

    private const val PLANT_DETAIL = "plant"
    fun plantDetail(plantId: Long) = "$PLANT_DETAIL/$plantId"
    const val PLANT_DETAIL_PATTERN = "$PLANT_DETAIL/{${NavArgs.PLANT_ID}}"

    private const val CATALOG_DETAIL = "catalog"
    fun catalogDetail(scientificName: String) =
        "$CATALOG_DETAIL/${Uri.encode(scientificName)}"
    const val CATALOG_DETAIL_PATTERN = "$CATALOG_DETAIL/{${NavArgs.SCIENTIFIC_NAME}}"

    private const val PHOTO_VIEWER = "photoviewer"
    fun photoViewer(plantId: Long, photoId: Long) =
        "$PHOTO_VIEWER/$plantId/$photoId"
    const val PHOTO_VIEWER_PATTERN =
        "$PHOTO_VIEWER/{${NavArgs.PLANT_ID}}/{${NavArgs.PHOTO_ID}}"
}

object NavArgs {
    const val PLANT_ID = "plantId"
    const val PHOTO_ID = "photoId"
    const val SCIENTIFIC_NAME = "scientificName"
}

@Composable
fun PlantCareNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.Home.route,
        modifier = modifier,
    ) {
        composable(TopLevelDestination.Home.route) {
            HomeScreen(onIdentifyClick = { navController.navigate(Routes.IDENTIFY) })
        }
        composable(TopLevelDestination.MyPlants.route) {
            MyPlantsScreen(onPlantClick = { id -> navController.navigate(Routes.plantDetail(id)) })
        }
        composable(TopLevelDestination.Calendar.route) { CalendarScreen() }
        composable(TopLevelDestination.Search.route) {
            SearchScreen(onPlantClick = { name -> navController.navigate(Routes.catalogDetail(name)) })
        }
        composable(TopLevelDestination.Profile.route) { ProfileScreen() }

        composable(Routes.IDENTIFY) {
            IdentifyScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.PLANT_DETAIL_PATTERN,
            arguments = listOf(navArgument(NavArgs.PLANT_ID) { type = NavType.LongType }),
        ) {
            PlantDetailScreen(
                onBack = { navController.popBackStack() },
                onPhotoClick = { plantId, photoId ->
                    navController.navigate(Routes.photoViewer(plantId, photoId))
                },
            )
        }

        composable(
            route = Routes.CATALOG_DETAIL_PATTERN,
            arguments = listOf(navArgument(NavArgs.SCIENTIFIC_NAME) { type = NavType.StringType }),
        ) {
            CatalogPlantDetailScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.PHOTO_VIEWER_PATTERN,
            arguments = listOf(
                navArgument(NavArgs.PLANT_ID) { type = NavType.LongType },
                navArgument(NavArgs.PHOTO_ID) { type = NavType.LongType },
            ),
        ) {
            PhotoViewerScreen(onBack = { navController.popBackStack() })
        }
    }
}
