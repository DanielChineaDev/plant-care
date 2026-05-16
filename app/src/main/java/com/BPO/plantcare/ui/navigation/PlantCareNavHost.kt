package com.BPO.plantcare.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.BPO.plantcare.ui.screens.calendar.CalendarScreen
import com.BPO.plantcare.ui.screens.home.HomeScreen
import com.BPO.plantcare.ui.screens.identify.IdentifyScreen
import com.BPO.plantcare.ui.screens.myplants.MyPlantsScreen
import com.BPO.plantcare.ui.screens.plantdetail.PlantDetailScreen
import com.BPO.plantcare.ui.screens.profile.ProfileScreen
import com.BPO.plantcare.ui.screens.search.SearchScreen

object Routes {
    const val IDENTIFY = "identify"
    private const val PLANT_DETAIL = "plant"
    fun plantDetail(plantId: Long) = "$PLANT_DETAIL/$plantId"
    const val PLANT_DETAIL_PATTERN = "$PLANT_DETAIL/{${NavArgs.PLANT_ID}}"

    val immersiveRoutes = setOf(IDENTIFY, PLANT_DETAIL_PATTERN)
}

object NavArgs {
    const val PLANT_ID = "plantId"
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
        composable(TopLevelDestination.Search.route) { SearchScreen() }
        composable(TopLevelDestination.Profile.route) { ProfileScreen() }

        composable(Routes.IDENTIFY) {
            IdentifyScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.PLANT_DETAIL_PATTERN,
            arguments = listOf(navArgument(NavArgs.PLANT_ID) { type = NavType.LongType }),
        ) {
            PlantDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
