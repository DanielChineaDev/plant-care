package com.BPO.plantcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.BPO.plantcare.ui.navigation.PlantCareBottomBar
import com.BPO.plantcare.ui.navigation.PlantCareNavHost
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
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { PlantCareBottomBar(navController) },
    ) { innerPadding ->
        PlantCareNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
