package com.cretemobility.app.core.navigation

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cretemobility.app.ui.departures.DeparturesScreen
import com.cretemobility.app.ui.favorites.FavoritesScreen
import com.cretemobility.app.ui.journey.JourneyPlannerScreen
import com.cretemobility.app.ui.journey.JourneyResultsScreen
import com.cretemobility.app.ui.map.MapScreen
import com.cretemobility.app.ui.onboarding.OnboardingScreen
import com.cretemobility.app.ui.settings.SettingsScreen
import com.cretemobility.app.ui.splash.SplashScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object JourneyPlanner : Screen("journey_planner")
    object JourneyResults : Screen("journey_results?origin={origin}&destination={destination}&time={time}") {
        fun createRoute(origin: String, destination: String, time: String) =
            "journey_results?origin=$origin&destination=$destination&time=$time"
    }
    object Departures : Screen("departures/{stopId}") {
        fun createRoute(stopId: String) = "departures/$stopId"
    }
    object Map : Screen("map?lat={lat}&lng={lng}&stopId={stopId}") {
        fun createRoute(lat: Double? = null, lng: Double? = null, stopId: String? = null): String {
            val params = mutableListOf<String>()
            lat?.let { params.add("lat=$it") }
            lng?.let { params.add("lng=$it") }
            stopId?.let { params.add("stopId=$it") }
            return if (params.isEmpty()) "map" else "map?${params.joinToString("&")}"
        }
    }
    object Favorites : Screen("favorites")
    object Settings : Screen("settings")
}

@Composable
fun CreteMobilityNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate(Screen.JourneyPlanner.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.JourneyPlanner.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.JourneyPlanner.route) {
            JourneyPlannerScreen(
                onNavigateToResults = { origin, destination, time ->
                    navController.navigate(
                        Screen.JourneyResults.createRoute(origin, destination, time)
                    )
                },
                onNavigateToMap = { lat, lng ->
                    navController.navigate(Screen.Map.createRoute(lat, lng))
                },
                onNavigateToDepartures = { stopId ->
                    navController.navigate(Screen.Departures.createRoute(stopId))
                },
                onNavigateToFavorites = {
                    navController.navigate(Screen.Favorites.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.JourneyResults.route,
            arguments = listOf(
                navArgument("origin") { type = NavType.StringType; defaultValue = "" },
                navArgument("destination") { type = NavType.StringType; defaultValue = "" },
                navArgument("time") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStack ->
            JourneyResultsScreen(
                originJson = backStack.arguments?.getString("origin") ?: "",
                destinationJson = backStack.arguments?.getString("destination") ?: "",
                time = backStack.arguments?.getString("time") ?: "",
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMap = { lat, lng ->
                    navController.navigate(Screen.Map.createRoute(lat, lng))
                }
            )
        }

        composable(
            route = Screen.Departures.route,
            arguments = listOf(
                navArgument("stopId") { type = NavType.StringType }
            )
        ) { backStack ->
            DeparturesScreen(
                stopId = backStack.arguments?.getString("stopId") ?: "",
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMap = { lat, lng ->
                    navController.navigate(Screen.Map.createRoute(lat, lng))
                }
            )
        }

        composable(
            route = Screen.Map.route,
            arguments = listOf(
                navArgument("lat") { type = NavType.FloatType; defaultValue = 35.3387f },
                navArgument("lng") { type = NavType.FloatType; defaultValue = 25.1442f },
                navArgument("stopId") { nullable = true; defaultValue = null; type = NavType.StringType }
            )
        ) { backStack ->
            MapScreen(
                initialLat = backStack.arguments?.getFloat("lat")?.toDouble() ?: 35.3387,
                initialLng = backStack.arguments?.getFloat("lng")?.toDouble() ?: 25.1442,
                stopId = backStack.arguments?.getString("stopId"),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDepartures = { stopId ->
                    navController.navigate(Screen.Departures.createRoute(stopId))
                }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlanner = { origin, destination ->
                    navController.navigate(
                        Screen.JourneyResults.createRoute(origin, destination, "now")
                    )
                },
                onNavigateToDepartures = { stopId ->
                    navController.navigate(Screen.Departures.createRoute(stopId))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
