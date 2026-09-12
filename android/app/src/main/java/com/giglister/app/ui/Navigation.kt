package com.giglister.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.giglister.app.data.AuthRepository
import com.giglister.app.location.LocationRepository
import com.giglister.app.ui.events.EventsScreen
import com.giglister.app.ui.login.LoginScreen
import com.giglister.app.ui.login.RegisterScreen
import com.giglister.app.ui.profile.ProfileScreen

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val EVENTS = "events"
    const val PROFILE = "profile"
}

/** Root of the app's navigation - which graph starts (auth vs. the logged-in tabs) is
 * decided once, from whether a token was already restored at app startup
 * (GigListerApp.onCreate); logging in/out then navigates between the two by hand rather
 * than needing a second decision point later. */
@Composable
fun GigListerNavHost(authRepository: AuthRepository, locationRepository: LocationRepository) {
    val navController = rememberNavController()
    val startDestination = if (authRepository.isLoggedIn) Routes.EVENTS else Routes.LOGIN

    val bottomNavRoutes = setOf(Routes.EVENTS, Routes.PROFILE)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.EVENTS,
                        onClick = {
                            navController.navigate(Routes.EVENTS) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                        label = { Text("Konzerte") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.PROFILE,
                        onClick = {
                            navController.navigate(Routes.PROFILE) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        label = { Text("Profil") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    authRepository = authRepository,
                    onLoggedIn = {
                        navController.navigate(Routes.EVENTS) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onGoToRegister = { navController.navigate(Routes.REGISTER) }
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    authRepository = authRepository,
                    onBackToLogin = { navController.popBackStack() }
                )
            }
            composable(Routes.EVENTS) {
                EventsScreen(locationRepository = locationRepository)
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    authRepository = authRepository,
                    locationRepository = locationRepository,
                    onLoggedOut = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0)
                        }
                    }
                )
            }
        }
    }
}
