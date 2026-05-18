package com.BPO.plantcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.BPO.plantcare.core.notification.FcmService
import com.BPO.plantcare.domain.repository.AuthState
import com.BPO.plantcare.ui.auth.AuthGateViewModel
import com.BPO.plantcare.ui.navigation.BottomBarViewModel
import com.BPO.plantcare.ui.navigation.PlantCareBottomBar
import com.BPO.plantcare.ui.navigation.PlantCareNavHost
import com.BPO.plantcare.ui.navigation.Routes
import com.BPO.plantcare.ui.navigation.TopLevelDestination
import com.BPO.plantcare.ui.screens.auth.AuthScreen
import com.BPO.plantcare.ui.theme.PlantCareTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Holder mutable de "pending deep link". Si llega un intent nuevo (via
    // onNewIntent) actualizamos esto y el compose LaunchedEffect navega.
    private var pendingChatUid by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingChatUid = intent.getStringExtra(FcmService.EXTRA_CHAT_UID)
        enableEdgeToEdge()
        setContent {
            PlantCareTheme {
                PlantCareRoot(
                    pendingChatUid = pendingChatUid,
                    onDeepLinkConsumed = { pendingChatUid = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(FcmService.EXTRA_CHAT_UID)?.let { pendingChatUid = it }
    }
}

/**
 * Root composable. Gate sobre [AuthState]:
 *   - Loading -> splash centrado.
 *   - SignedOut -> AuthScreen (login/registro).
 *   - SignedIn  -> app real con NavHost.
 *
 * Asi sin sesion no hay forma de entrar a pantallas con datos remotos y nos
 * ahorramos comprobar el user en cada ViewModel de feature.
 */
@Composable
private fun PlantCareRoot(
    pendingChatUid: String?,
    onDeepLinkConsumed: () -> Unit,
    gateViewModel: AuthGateViewModel = hiltViewModel(),
) {
    val authState by gateViewModel.authState.collectAsStateWithLifecycle()

    // Animamos por clase para que solo hagamos crossfade cuando cambia el tipo
    // de estado (no al recibir un nuevo perfil SignedIn -> SignedIn).
    AnimatedContent(
        targetState = authState::class,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
        label = "auth-gate",
    ) { stateClass ->
        when (stateClass) {
            AuthState.Loading::class -> SplashScreen()
            AuthState.SignedOut::class -> AuthScreen()
            else -> PlantCareApp(
                pendingChatUid = pendingChatUid,
                onDeepLinkConsumed = onDeepLinkConsumed,
            )
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PlantCareApp(
    pendingChatUid: String?,
    onDeepLinkConsumed: () -> Unit,
    bottomBarViewModel: BottomBarViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = TopLevelDestination.entries.any { it.route == currentRoute }
    val counts by bottomBarViewModel.counts.collectAsStateWithLifecycle()

    HandleDeepLink(navController, pendingChatUid, onDeepLinkConsumed)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) PlantCareBottomBar(navController, counts)
        },
    ) { innerPadding ->
        PlantCareNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun HandleDeepLink(
    navController: NavHostController,
    pendingChatUid: String?,
    onConsumed: () -> Unit,
) {
    LaunchedEffect(pendingChatUid) {
        val uid = pendingChatUid ?: return@LaunchedEffect
        navController.navigate(Routes.chat(uid)) {
            launchSingleTop = true
        }
        onConsumed()
    }
}
