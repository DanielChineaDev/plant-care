package com.BPO.plantcare

import android.content.Context
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
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.BPO.plantcare.core.notification.FcmService
import com.BPO.plantcare.domain.repository.AuthState
import com.BPO.plantcare.ui.auth.AuthGateViewModel
import com.BPO.plantcare.ui.navigation.BottomBarViewModel
import com.BPO.plantcare.ui.navigation.LocalIsExpandedScreen
import com.BPO.plantcare.ui.navigation.PlantCareBottomBar
import com.BPO.plantcare.ui.navigation.PlantCareDrawerContent
import com.BPO.plantcare.ui.navigation.PlantCareNavHost
import com.BPO.plantcare.ui.navigation.Routes
import com.BPO.plantcare.ui.navigation.TopLevelDestination
import com.BPO.plantcare.ui.screens.auth.AuthScreen
import com.BPO.plantcare.ui.theme.PlantCareTheme
import com.BPO.plantcare.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingChatUid by mutableStateOf<String?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            com.BPO.plantcare.core.locale.LocaleHelper.wrap(newBase),
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingChatUid = intent.getStringExtra(FcmService.EXTRA_CHAT_UID)
        enableEdgeToEdge()
        setContent {
            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
            val isExpanded = widthSizeClass == WindowWidthSizeClass.Expanded
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val theme by themeViewModel.theme.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalIsExpandedScreen provides isExpanded) {
                PlantCareTheme(
                    palette = theme.palette,
                    dynamicColor = theme.dynamicColor,
                ) {
                    PlantCareRoot(
                        pendingChatUid = pendingChatUid,
                        onDeepLinkConsumed = { pendingChatUid = null },
                    )
                }
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
 * Gate raiz sobre AuthState.
 */
@Composable
private fun PlantCareRoot(
    pendingChatUid: String?,
    onDeepLinkConsumed: () -> Unit,
    gateViewModel: AuthGateViewModel = hiltViewModel(),
) {
    val authState by gateViewModel.authState.collectAsStateWithLifecycle()

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
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
        ) {
            com.BPO.plantcare.ui.components.PlantCareLogo(size = 96.dp)
            CircularProgressIndicator()
        }
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
    // Pantallas "inmersivas" sin bottom bar: el visor fullscreen de fotos y
    // la pantalla de identificar (camara/resultado a pantalla completa).
    val hideNavChrome = currentRoute?.startsWith("photoviewer") == true ||
        currentRoute == Routes.IDENTIFY

    val counts by bottomBarViewModel.counts.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    HandleDeepLink(navController, pendingChatUid, onDeepLinkConsumed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Gesto del drawer disponible en toda la app (excepto visor fullscreen)
        // para que se pueda abrir desde cualquier pantalla deslizando desde
        // el borde izquierdo.
        gesturesEnabled = !hideNavChrome,
        drawerContent = {
            PlantCareDrawerContent(
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    // Grafo plano: para destinos top-level limpiamos la pila
                    // hasta la raiz (sin save/restore, que en un grafo plano
                    // restauraria la pantalla anterior). Asi el destino del
                    // drawer siempre nos lleva a su seccion.
                    val isTopLevel = TopLevelDestination.entries.any { it.route == route }
                    if (isTopLevel) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(route) { launchSingleTop = true }
                    }
                },
            )
        },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            // No queremos que el Scaffold root anada padding por las
            // status/nav bars al content: cada pantalla tiene su propio
            // Scaffold con TopAppBar que YA respeta status bars, y el
            // bottomBar respeta nav bars por su cuenta. Aplicar insets
            // aqui duplicaba el espacio superior en todas las pantallas.
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!hideNavChrome) {
                    PlantCareBottomBar(
                        navController = navController,
                        counts = counts,
                        onIdentifyClick = { navController.navigate(Routes.IDENTIFY) },
                    )
                }
            },
        ) { innerPadding ->
            // padding(innerPadding) reserva el alto del bottom bar; ademas
            // consumeWindowInsets avisa a los Scaffold internos de cada
            // pantalla de que esos insets YA estan aplicados, para que no
            // vuelvan a anadir el inset de la barra de navegacion del sistema
            // (lo que dejaba un hueco entre el contenido y el bottom bar en
            // pantallas como detalle de planta o buscar).
            PlantCareNavHost(
                navController = navController,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            )
        }
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
