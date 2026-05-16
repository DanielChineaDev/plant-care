package com.BPO.plantcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.BPO.plantcare.ui.navigation.PlantCareBottomBar
import com.BPO.plantcare.ui.navigation.PlantCareNavHost
import com.BPO.plantcare.ui.navigation.TopLevelDestination
import com.BPO.plantcare.ui.theme.PlantCareTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlantCareTheme {
                PlantCareApp()
            }
        }
    }
}

@Composable
private fun PlantCareApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = TopLevelDestination.entries.any { it.route == currentRoute }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) PlantCareBottomBar(navController)
        },
    ) { innerPadding ->
        PlantCareNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
