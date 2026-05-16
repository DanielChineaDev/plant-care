package com.BPO.plantcare.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.BPO.plantcare.ui.screens.calendar.CalendarScreen
import com.BPO.plantcare.ui.screens.home.HomeScreen
import com.BPO.plantcare.ui.screens.myplants.MyPlantsScreen
import com.BPO.plantcare.ui.screens.profile.ProfileScreen
import com.BPO.plantcare.ui.screens.search.SearchScreen

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
        composable(TopLevelDestination.Home.route) { HomeScreen() }
        composable(TopLevelDestination.MyPlants.route) { MyPlantsScreen() }
        composable(TopLevelDestination.Calendar.route) { CalendarScreen() }
        composable(TopLevelDestination.Search.route) { SearchScreen() }
        composable(TopLevelDestination.Profile.route) { ProfileScreen() }
    }
}
