@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.giglister.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.giglister.app.data.AuthRepository
import com.giglister.app.location.LocationRepository
import com.giglister.app.ui.browse.*
import com.giglister.app.ui.edit.*
import com.giglister.app.ui.login.*
import com.giglister.app.ui.components.*

@Composable fun GigListerNavHost(authRepository: AuthRepository, locationRepository: LocationRepository, incomingRoute: String? = null, consumeRoute: () -> Unit = {}) {
    val context = LocalContext.current
    val app: GigState = viewModel(factory = SimpleViewModelFactory { GigState(authRepository, locationRepository, context) })
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val tabs = listOf("home" to "Start", "events" to "Konzerte", "festivals" to "Festivals", "places" to "Orte", "me" to "Mein GigLister")
    val icons = listOf(Icons.Default.Home, Icons.Default.ConfirmationNumber, Icons.Default.Festival, Icons.Default.Place, Icons.Default.Person)
    val isMain = tabs.any { it.first == current }
    val snack = remember { SnackbarHostState() }
    val navigate: (String) -> Unit = { route -> nav.navigate(route) { launchSingleTop = true; if (route == "home") popUpTo("home") { inclusive = false } } }
    LaunchedEffect(incomingRoute) { if (incomingRoute != null) { nav.navigate(incomingRoute) { launchSingleTop = true }; consumeRoute() } }
    LaunchedEffect(app.notice) { app.notice?.let { val message = it; snack.showSnackbar(message); if (app.notice == message) app.notice = null } }
    if (app.me?.mustChangePassword == true) {
        Scaffold(snackbarHost = { SnackbarHost(snack) }) { padding -> Box(Modifier.padding(padding)) { PasswordScreen(app, required = true) } }
        return
    }
    if (app.genrePrompt && app.loggedIn) AlertDialog(onDismissRequest = { app.genrePrompt = false }, title = { Text("Lieblingsgenres festlegen?") }, text = { Text("Mit deinen Musikrichtungen passen die Konzertvorschläge besser zu dir.") }, confirmButton = {
        TextButton(onClick = { app.genrePrompt = false; navigate("genres") }) { Text("Genres auswählen") }
    }, dismissButton = { TextButton(onClick = { app.genrePrompt = false }) { Text("Später") } })
    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = { TopAppBar(title = { Text(if (isMain) "GIGLISTER" else when {
            current == "calendar" -> "Kalender"
            current?.endsWith("edit") == true -> "Bearbeiten"
            current?.endsWith("new") == true -> "Neu auf GigLister"
            else -> "GIGLISTER"
        }, style = MaterialTheme.typography.titleLarge) }, navigationIcon = {
            if (!isMain) IconButton(onClick = { if (!nav.popBackStack()) navigate("events") }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") }
        }, actions = {
            if (isMain) IconButton(onClick = { navigate("search") }) { Icon(Icons.Default.Search, "Suche") }
            if (isMain && app.loggedIn) IconButton(onClick = { navigate("event/new") }) { Icon(Icons.Default.Add, "Konzert anlegen") }
        }) },
        bottomBar = { if (isMain) NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            tabs.forEachIndexed { i, (route, label) -> NavigationBarItem(selected = current == route, onClick = {
                nav.navigate(route) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true }
            }, icon = { Icon(icons[i], null) }, label = { Text(label, maxLines = 1) }) }
        } }
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen(app, navigate) }
            composable("festivals") { FestivalsScreen(app, navigate) }
            composable("festivals/new") { AuthGate(app, navigate) { FestivalEditor(null, app, navigate) } }
            composable("festivals/{id}?saved={saved}", arguments = listOf(navArgument("id") { type = NavType.LongType }, navArgument("saved") { type = NavType.BoolType; defaultValue = false })) { FestivalScreen(it.arguments!!.getLong("id"), it.arguments!!.getBoolean("saved"), app, navigate) }
            composable("festivals/{id}/edit", arguments = listOf(navArgument("id") { type = NavType.LongType })) { AuthGate(app, navigate) { FestivalEditor(it.arguments!!.getLong("id"), app, navigate) } }
            composable("genres") { AuthGate(app, navigate) { androidx.compose.foundation.lazy.LazyColumn(contentPadding = PaddingValues(20.dp)) { item { PageTitle("Lieblingsgenres"); GenrePreferences(app) } } } }
            composable("password") { AuthGate(app, navigate) { PasswordScreen(app, done = { nav.popBackStack() }) } }
            composable("submissions") { AuthGate(app, navigate) { SubmissionsScreen(app, navigate) } }
            composable("events") { ConcertsScreen(app, navigate) }
            composable("places") { PlacesScreen(app, navigate) }
            composable("discover") { HomeScreen(app, navigate) }
            composable("search") { SearchScreen(navigate) }
            composable("me") { MyGigListerScreen(app, navigate) }
            composable("calendar") { CalendarScreen(app, navigate) }
            composable("login") { LoginScreen(authRepository, onLoggedIn = { profile ->
                app.acceptLogin(profile)
                if (!nav.popBackStack("login", inclusive = true)) navigate("me")
            }, onGoToRegister = { navigate("register") }, onForgot = { navigate("forgot") }) }
            composable("register") { RegisterScreen(authRepository) { nav.navigate("login") { popUpTo("register") { inclusive = true }; launchSingleTop = true } } }
            composable("forgot") { RecoveryScreen(app, "forgot", null) { nav.popBackStack() } }
            composable("reset?token={token}", arguments = listOf(navArgument("token") { defaultValue = "" })) { RecoveryScreen(app, "reset", it.arguments?.getString("token")) { nav.navigate("me") { popUpTo("events"); launchSingleTop = true } } }
            composable("verify?token={token}", arguments = listOf(navArgument("token") { defaultValue = "" })) { RecoveryScreen(app, "verify", it.arguments?.getString("token")) { nav.navigate("me") { popUpTo("events"); launchSingleTop = true } } }
            composable("event/new") { AuthGate(app, navigate) { EventEditor(null, app, navigate) } }
            composable("event/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { EventDetailScreen(it.arguments!!.getLong("id"), app, navigate) }
            composable("event/{id}/edit", arguments = listOf(navArgument("id") { type = NavType.LongType })) { AuthGate(app, navigate) { EventEditor(it.arguments!!.getLong("id"), app, navigate) } }
            listOf("bands", "locations").forEach { kind ->
                composable("$kind/new") { AuthGate(app, navigate) { EntityEditor(kind, null, app, navigate) } }
                composable("$kind/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { EntityDetailScreen(kind, it.arguments!!.getLong("id"), app, navigate) }
                composable("$kind/{id}/edit", arguments = listOf(navArgument("id") { type = NavType.LongType })) { AuthGate(app, navigate) { EntityEditor(kind, it.arguments!!.getLong("id"), app, navigate) } }
            }
        }
    }
}

@Composable private fun AuthGate(app: GigState, navigate: (String) -> Unit, content: @Composable () -> Unit) {
    if (app.loggedIn) content() else Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Melde dich an, um Inhalte anzulegen oder zu bearbeiten.")
        Button(onClick = { navigate("login") }) { Text("Anmelden") }
    }
}
