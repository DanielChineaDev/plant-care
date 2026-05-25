package com.BPO.plantcare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
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

    // El splash con el logo + spinner se muestra al menos 2s (mas si la
    // sesion tarda en resolverse) para que de tiempo a verlo en arranques
    // rapidos. Pasado ese minimo, transicion suave a la app o al login.
    var minElapsed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        minElapsed = true
    }
    val showSplash = authState is AuthState.Loading || !minElapsed

    AnimatedContent(
        targetState = showSplash,
        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
        label = "auth-gate",
    ) { splash ->
        if (splash) {
            SplashScreen()
        } else {
            when (authState) {
                is AuthState.SignedOut -> AuthScreen()
                else -> PlantCareApp(
                    pendingChatUid = pendingChatUid,
                    onDeepLinkConsumed = onDeepLinkConsumed,
                )
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    // Pantalla de carga premium, coherente con el windowBackground del
    // arranque en frio (degradado verde sage). El logo cuadrado oficial
    // aparece sobre una tarjeta redondeada blanca (igual que el icono del
    // launcher) con una animacion suave de entrada (escala + fade).
    val brandTop = androidx.compose.ui.graphics.Color(0xFF4E7A5E)
    val brandMid = androidx.compose.ui.graphics.Color(0xFF3E6347)
    val brandBottom = androidx.compose.ui.graphics.Color(0xFF2A4D32)

    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "splash")
    // Respiracion sutil del logo (escala 1.0 <-> 1.04).
    val breathe by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                1400,
                easing = androidx.compose.animation.core.FastOutSlowInEasing,
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "breathe",
    )
    // Entrada: fade + scale-in al montar.
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val enterAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(500),
        label = "enterAlpha",
    )
    val enterScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (appeared) 1f else 0.8f,
        animationSpec = androidx.compose.animation.core.tween(
            500,
            easing = androidx.compose.animation.core.FastOutSlowInEasing,
        ),
        label = "enterScale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(brandTop, brandMid, brandBottom),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
            modifier = Modifier.graphicsLayer {
                alpha = enterAlpha
                scaleX = enterScale
                scaleY = enterScale
            },
        ) {
            // Tarjeta redondeada blanca con el logo cuadrado oficial.
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                color = androidx.compose.ui.graphics.Color(0xFFF1F3F0),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .size(128.dp)
                    .graphicsLayer {
                        scaleX = breathe
                        scaleY = breathe
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(
                            id = R.drawable.ic_plantcare_logo,
                        ),
                        contentDescription = "PlantCare",
                        modifier = Modifier.size(104.dp),
                    )
                }
            }
            androidx.compose.material3.Text(
                text = "PlantCare",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White,
            )
            CircularProgressIndicator(
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp),
            )
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
