package com.BPO.plantcare.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.BPO.plantcare.ui.screens.calendar.CalendarScreen
import com.BPO.plantcare.ui.screens.catalogdetail.CatalogPlantDetailScreen
import com.BPO.plantcare.ui.screens.chat.ChatScreen
import com.BPO.plantcare.ui.screens.chatslist.ChatsListScreen
import com.BPO.plantcare.ui.screens.communities.CommunitiesListScreen
import com.BPO.plantcare.ui.screens.communityfeed.CommunityFeedScreen
import com.BPO.plantcare.ui.screens.postdetail.PostDetailScreen
import com.BPO.plantcare.ui.screens.diagnosis.DiagnosisDetailScreen
import com.BPO.plantcare.ui.screens.diagnosis.DiagnosisListScreen
import com.BPO.plantcare.ui.screens.home.HomeScreen
import com.BPO.plantcare.ui.screens.identify.IdentifyScreen
import com.BPO.plantcare.ui.screens.lightmeter.LightMeterScreen
import com.BPO.plantcare.ui.screens.myplants.MyPlantsScreen
import com.BPO.plantcare.ui.screens.myprofile.MyProfileScreen
import com.BPO.plantcare.ui.screens.globalsearch.GlobalSearchScreen
import com.BPO.plantcare.ui.screens.notifications.NotificationsScreen
import com.BPO.plantcare.ui.screens.photoviewer.PhotoViewerScreen
import com.BPO.plantcare.ui.screens.plantdetail.PlantDetailScreen
import com.BPO.plantcare.ui.screens.profile.ProfileScreen
import com.BPO.plantcare.ui.screens.publicprofile.PublicProfileScreen
import com.BPO.plantcare.ui.screens.search.SearchScreen
import com.BPO.plantcare.ui.screens.tools.ToolsScreen

object Routes {
    const val IDENTIFY = "identify"
    const val LIGHT_METER = "light_meter"
    const val DIAGNOSIS_LIST = "diagnosis"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
    const val MY_PROFILE = "my_profile"
    const val EDIT_PROFILE = "edit_profile"
    const val TOOLS = "tools"
    const val NOTIFICATIONS = "notifications"
    const val GLOBAL_SEARCH = "global_search"

    private const val DIAGNOSIS_DETAIL = "diagnosis_detail"
    fun diagnosisDetail(id: String) = "$DIAGNOSIS_DETAIL/$id"
    const val DIAGNOSIS_DETAIL_PATTERN = "$DIAGNOSIS_DETAIL/{${NavArgs.DIAGNOSIS_ID}}"

    private const val COMMUNITY_FEED = "community_feed"
    fun communityFeed(id: String) = "$COMMUNITY_FEED/$id"
    const val COMMUNITY_FEED_PATTERN = "$COMMUNITY_FEED/{${NavArgs.COMMUNITY_ID}}"

    private const val POST_DETAIL = "post_detail"
    fun postDetail(communityId: String, postId: String) = "$POST_DETAIL/$communityId/$postId"
    const val POST_DETAIL_PATTERN =
        "$POST_DETAIL/{${NavArgs.COMMUNITY_ID}}/{${NavArgs.POST_ID}}"

    private const val CHAT = "chat"
    fun chat(otherUid: String) = "$CHAT/$otherUid"
    const val CHAT_PATTERN = "$CHAT/{${NavArgs.OTHER_UID}}"

    private const val PUBLIC_PROFILE = "public_profile"
    fun publicProfile(uid: String) = "$PUBLIC_PROFILE/$uid"
    const val PUBLIC_PROFILE_PATTERN = "$PUBLIC_PROFILE/{${NavArgs.OTHER_UID}}"

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
    const val DIAGNOSIS_ID = "diagnosisId"
    const val COMMUNITY_ID = "communityId"
    const val POST_ID = "postId"
    const val OTHER_UID = "otherUid"
}

private const val ANIM = 280

// Transiciones reutilizables para pantallas de detalle (slide horizontal "push-from-right").
private val slideEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(tween(ANIM)) { fullWidth -> fullWidth } + fadeIn(tween(ANIM))
}
private val slideExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(ANIM))
}
private val slidePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(ANIM))
}
private val slidePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(tween(ANIM)) { fullWidth -> fullWidth } + fadeOut(tween(ANIM))
}

@Composable
fun PlantCareNavHost(
    navController: NavHostController,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.Home.route,
        modifier = modifier,
        // Tabs: cross-fade suave.
        enterTransition = { fadeIn(tween(ANIM)) },
        exitTransition = { fadeOut(tween(ANIM)) },
        popEnterTransition = { fadeIn(tween(ANIM)) },
        popExitTransition = { fadeOut(tween(ANIM)) },
    ) {
        // ===== Top-level (bottom nav) =====
        composable(TopLevelDestination.Home.route) {
            HomeScreen(
                onOpenDrawer = onOpenDrawer,
                onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                onGlobalSearchClick = { navController.navigate(Routes.GLOBAL_SEARCH) },
                onIdentifyClick = { navController.navigate(Routes.IDENTIFY) },
                onPlantClick = { id -> navController.navigate(Routes.plantDetail(id)) },
                onCommunitiesClick = { navController.navigate(TopLevelDestination.Communities.route) },
                onPostClick = { cid, pid -> navController.navigate(Routes.postDetail(cid, pid)) },
                onAuthorClick = { uid -> navController.navigate(Routes.publicProfile(uid)) },
            )
        }
        composable(TopLevelDestination.MyPlants.route) {
            MyPlantsScreen(
                onOpenDrawer = onOpenDrawer,
                onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                onPlantClick = { id -> navController.navigate(Routes.plantDetail(id)) },
                onIdentifyClick = { navController.navigate(Routes.IDENTIFY) },
            )
        }
        composable(TopLevelDestination.Communities.route) {
            CommunitiesListScreen(
                onOpenDrawer = onOpenDrawer,
                onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                onCommunityClick = { id -> navController.navigate(Routes.communityFeed(id)) },
                onPostClick = { cid, pid -> navController.navigate(Routes.postDetail(cid, pid)) },
                onAuthorClick = { uid -> navController.navigate(Routes.publicProfile(uid)) },
            )
        }
        composable(TopLevelDestination.Search.route) {
            SearchScreen(
                onOpenDrawer = onOpenDrawer,
                onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                onPlantClick = { name -> navController.navigate(Routes.catalogDetail(name)) },
            )
        }
        composable(TopLevelDestination.Messages.route) {
            ChatsListScreen(
                onOpenDrawer = onOpenDrawer,
                onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                onChatClick = { uid -> navController.navigate(Routes.chat(uid)) },
            )
        }

        // ===== Accesibles desde drawer =====
        composable(
            Routes.CALENDAR,
            enterTransition = slideEnter,
            exitTransition = slideExit,
            popEnterTransition = slidePopEnter,
            popExitTransition = slidePopExit,
        ) {
            CalendarScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.SETTINGS,
            enterTransition = slideEnter,
            exitTransition = slideExit,
            popEnterTransition = slidePopEnter,
            popExitTransition = slidePopExit,
        ) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onOpenLightMeter = { navController.navigate(Routes.LIGHT_METER) },
                onOpenDiagnosis = { navController.navigate(Routes.DIAGNOSIS_LIST) },
            )
        }

        composable(
            Routes.MY_PROFILE,
            enterTransition = slideEnter,
            exitTransition = slideExit,
            popEnterTransition = slidePopEnter,
            popExitTransition = slidePopExit,
        ) {
            MyProfileScreen(
                onBack = { navController.popBackStack() },
                onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
            )
        }

        composable(
            Routes.EDIT_PROFILE,
            enterTransition = slideEnter,
            exitTransition = slideExit,
            popEnterTransition = slidePopEnter,
            popExitTransition = slidePopExit,
        ) {
            com.BPO.plantcare.ui.screens.editprofile.EditProfileScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            Routes.GLOBAL_SEARCH,
            enterTransition = slideEnter,
            exitTransition = slideExit,
            popEnterTransition = slidePopEnter,
            popExitTransition = slidePopExit,
        ) {
            GlobalSearchScreen(
                onBack = { navController.popBackStack() },
                onSpeciesClick = { name -> navController.navigate(Routes.catalogDetail(name)) },
                onCommunityClick = { cid -> navController.navigate(Routes.communityFeed(cid)) },
                onUserClick = { uid -> navController.navigate(Routes.publicProfile(uid)) },
            )
        }

        composable(
            Routes.NOTIFICATIONS,
            enterTransition = slideEnter,
            exitTransition = slideExit,
            popEnterTransition = slidePopEnter,
            popExitTransition = slidePopExit,
        ) {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onPostClick = { cid, pid -> navController.navigate(Routes.postDetail(cid, pid)) },
                onCommunityClick = { cid -> navController.navigate(Routes.communityFeed(cid)) },
                onProfileClick = { uid -> navController.navigate(Routes.publicProfile(uid)) },
            )
        }

        composable(
            Routes.TOOLS,
            enterTransition = slideEnter,
            exitTransition = slideExit,
            popEnterTransition = slidePopEnter,
            popExitTransition = slidePopExit,
        ) {
            ToolsScreen(
                onBack = { navController.popBackStack() },
                onOpenLightMeter = { navController.navigate(Routes.LIGHT_METER) },
                onOpenDiagnosis = { navController.navigate(Routes.DIAGNOSIS_LIST) },
            )
        }

        // ===== Detalles =====
        composable(
            Routes.IDENTIFY,
            enterTransition = slideEnter,
            exitTransition = slideExit,
            popEnterTransition = slidePopEnter,
            popExitTransition = slidePopExit,
        ) {
            IdentifyScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.LIGHT_METER,
            enterTransition = slideEnter,
            exitTransition = slideExit,
            popEnterTransition = slidePopEnter,
            popExitTransition = slidePopExit,
        ) {
            LightMeterScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = onOpenDrawer,
            )
        }

        composable(
            Routes.DIAGNOSIS_LIST,
            enterTransition = slideEnter,
            exitTransition = slideExit,
            popEnterTransition = slidePopEnter,
            popExitTransition = slidePopExit,
        ) {
            DiagnosisListScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = onOpenDrawer,
                onDiagnosisClick = { id -> navController.navigate(Routes.diagnosisDetail(id)) },
            )
        }

        composable(
            route = Routes.DIAGNOSIS_DETAIL_PATTERN,
            arguments = listOf(navArgument(NavArgs.DIAGNOSIS_ID) { type = NavType.StringType }),
            enterTransition = slideEnter,
            exitTransition = slideExit,
            popEnterTransition = slidePopEnter,
            popExitTransition = slidePopExit,
        ) {
            DiagnosisDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = onOpenDrawer,
            )
        }

        composable(
            route = Routes.COMMUNITY_FEED_PATTERN,
            arguments = listOf(navArgument(NavArgs.COMMUNITY_ID) { type = NavType.StringType }),
            enterTransition = slideEnter,
            exitTransition = slideExit,
            popEnterTransition = slidePopEnter,
            popExitTransition = slidePopExit,
        ) {
            CommunityFeedScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = onOpenDrawer,
                onPostClick = { cid, pid -> navController.navigate(Routes.postDetail(cid, pid)) },
                onAuthorClick = { uid -> navController.navigate(Routes.chat(uid)) },
                onAuthorNameClick = { uid -> navController.navigate(Routes.publicProfile(uid)) },
            )
        }

        composable(
            route = Routes.POST_DETAIL_PATTERN,
            arguments = listOf(
                navArgument(NavArgs.COMMUNITY_ID) { type = NavType.StringType },
                navArgument(NavArgs.POST_ID) { type = NavType.StringType },
            ),
            enterTransition = slideEnter,
            exitTransition = slideExit,
            popEnterTransition = slidePopEnter,
            popExitTransition = slidePopExit,
        ) {
            PostDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = onOpenDrawer,
                onAuthorClick = { uid -> navController.navigate(Routes.chat(uid)) },
                onAuthorNameClick = { uid -> navController.navigate(Routes.publicProfile(uid)) },
            )
        }

        composable(
            route = Routes.PUBLIC_PROFILE_PATTERN,
            arguments = listOf(navArgument(NavArgs.OTHER_UID) { type = NavType.StringType }),
            enterTransition = slideEnter,
            exitTransition = slideExit,
            popEnterTransition = slidePopEnter,
            popExitTransition = slidePopExit,
        ) {
            PublicProfileScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = onOpenDrawer,
                onMessageClick = { uid -> navController.navigate(Routes.chat(uid)) },
            )
        }

        composable(
            route = Routes.CHAT_PATTERN,
            arguments = listOf(navArgument(NavArgs.OTHER_UID) { type = NavType.StringType }),
            enterTransition = slideEnter,
            exitTransition = slideExit,
            popEnterTransition = slidePopEnter,
            popExitTransition = slidePopExit,
        ) {
            ChatScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = onOpenDrawer,
            )
        }

        composable(
            route = Routes.PLANT_DETAIL_PATTERN,
            arguments = listOf(navArgument(NavArgs.PLANT_ID) { type = NavType.LongType }),
            enterTransition = slideEnter,
            exitTransition = slideExit,
            popEnterTransition = slidePopEnter,
            popExitTransition = slidePopExit,
        ) {
            PlantDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = onOpenDrawer,
                onPhotoClick = { plantId, photoId ->
                    navController.navigate(Routes.photoViewer(plantId, photoId))
                },
            )
        }

        composable(
            route = Routes.CATALOG_DETAIL_PATTERN,
            arguments = listOf(navArgument(NavArgs.SCIENTIFIC_NAME) { type = NavType.StringType }),
            enterTransition = slideEnter,
            exitTransition = slideExit,
            popEnterTransition = slidePopEnter,
            popExitTransition = slidePopExit,
        ) {
            CatalogPlantDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenDrawer = onOpenDrawer,
            )
        }

        composable(
            route = Routes.PHOTO_VIEWER_PATTERN,
            arguments = listOf(
                navArgument(NavArgs.PLANT_ID) { type = NavType.LongType },
                navArgument(NavArgs.PHOTO_ID) { type = NavType.LongType },
            ),
            enterTransition = { fadeIn(tween(ANIM)) },
            exitTransition = { fadeOut(tween(ANIM)) },
            popEnterTransition = { fadeIn(tween(ANIM)) },
            popExitTransition = { fadeOut(tween(ANIM)) },
        ) {
            PhotoViewerScreen(onBack = { navController.popBackStack() })
        }
    }
}
