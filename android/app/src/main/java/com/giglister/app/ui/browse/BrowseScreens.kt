@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.giglister.app.ui.browse

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.giglister.app.data.api.ApiClient
import com.giglister.app.data.model.*
import com.giglister.app.ui.*
import com.giglister.app.ui.components.*
import com.giglister.app.util.errorMessage
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.YearMonth

@Composable fun AreaButton(app: GigState) {
    var show by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) { Text(app.area.label) }
    if (show) AreaDialog(app, onDismiss = { show = false }, onApply = { app.changeArea(it); show = false })
}

@Composable fun AreaDialog(app: GigState, onDismiss: () -> Unit, onApply: (SearchArea) -> Unit) {
    var city by rememberSaveable { mutableStateOf(app.area.city) }
    var radius by rememberSaveable { mutableIntStateOf(app.area.radius) }
    var resolved by remember { mutableStateOf<SearchArea?>(null) }
    val context = LocalContext.current
    val action = rememberAction(app)
    fun gps() = action.run { onApply(app.gps(radius)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) gps() else app.notice = "Standort nicht freigegeben. Gib stattdessen eine Stadt ein."
    }
    AlertDialog(onDismissRequest = { if (!action.busy) onDismiss() }, title = { Text("Wo soll es hingehen?") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Finde Live-Musik in deiner Umgebung oder plane deinen nächsten Konzertabend.")
            OutlinedTextField(city, { city = it; resolved = null }, label = { Text("Stadt") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(10, 25, 50).forEach { km -> FilterChip(radius == km, { radius = km }, label = { Text("$km km") }) } }
            OutlinedButton(onClick = {
                if (listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) gps()
                else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }, enabled = !action.busy, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.MyLocation, null); Spacer(Modifier.width(8.dp)); Text("Standort verwenden") }
            if (resolved != null) Text("Die Stadt konnte nicht auf der Karte gefunden werden. Du kannst trotzdem nach dem Stadtnamen suchen.")
            if (action.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }, confirmButton = {
        TextButton(enabled = !action.busy && (city.isNotBlank() || (app.area.hasCoordinates && city == app.area.city)), onClick = { action.run {
            if (city == app.area.city && app.area.hasCoordinates) onApply(app.area.copy(radius = radius))
            else if (resolved != null) onApply(resolved!!.copy(radius = radius))
            else {
                val area = app.findCity(city, radius)
                if (area.hasCoordinates) onApply(area) else resolved = area
            }
        } }) { Text(if (resolved == null) "Übernehmen" else "Nur Stadt suchen") }
    }, dismissButton = { TextButton(enabled = !action.busy, onClick = { onApply(SearchArea()) }) { Text("Alle Orte") } })
}

@Composable fun DateFilters(app: GigState) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DateFilter.entries.forEach { filter -> FilterChip(selected = app.filter == filter, onClick = { app.filter = filter }, label = { Text(filter.label) }) }
    }
}

@Composable fun ConcertsScreen(app: GigState, navigate: (String) -> Unit) {
    val (from, to) = dateRange(app.filter)
    val baseQuery = app.area.query() + mapOf("from" to from.toString(), "to" to to.toString())
    val query = baseQuery + app.genreQuery()
    var page by remember(query, app.revision) { mutableIntStateOf(0) }
    var events by remember(query, app.revision) { mutableStateOf<List<EventResponse>>(emptyList()) }
    var hasMore by remember(query, app.revision) { mutableStateOf(false) }
    var loading by remember(query, app.revision) { mutableStateOf(true) }
    var error by remember(query, app.revision) { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(query, page, attempt, app.revision) {
        loading = true; error = null
        try {
            val response = ApiClient.api.events(query + mapOf("page" to page.toString(), "size" to "30"))
            events = (if (page == 0) response.content else events + response.content).distinctBy { it.id }
            hasMore = page + 1 < response.totalPages
        } catch (e: CancellationException) { throw e } catch (e: Exception) { error = errorMessage(e) } finally { loading = false }
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageTitle("Dein nächster\nKonzertabend.", "Kleine Bühnen. Große Abende. Live-Musik entdecken.") }
        item { AreaButton(app) }
        item { DateFilters(app) }
        item { ConcertGenreFilter(app, baseQuery) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { navigate("calendar") }) { Icon(Icons.Default.CalendarMonth, null); Spacer(Modifier.width(8.dp)); Text("Kalender") }
            if (app.loggedIn) TextButton(onClick = { navigate("event/new") }) { Text("+ Konzert") }
        } }
        concerts(events, { navigate("event/$it") })
        if (loading) item { Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        if (error != null) item { ErrorPanel(error!!) { attempt++ } }
        if (!loading && error == null && events.isEmpty()) item { EmptyMessage("Hier ist es noch ruhig. Probiere einen anderen Zeitraum oder Standort.") }
        if (!loading && error == null && hasMore) item { OutlinedButton(onClick = { page++ }, modifier = Modifier.fillMaxWidth()) { Text("Weitere Konzerte laden") } }
    }
}

@Composable fun SearchScreen(navigate: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(4.dp))
        PageTitle("Was suchst du?", "Konzerte, Bands und Orte an einem Platz.")
        OutlinedTextField(query, { query = it }, label = { Text("Band, Konzert oder Ort") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { submitted = query.trim() }),
            trailingIcon = { TextButton(onClick = { submitted = query.trim() }) { Text("Suchen") } })
        if (submitted.isBlank()) EmptyMessage("Such zum Beispiel nach deiner Lieblingsband oder deinem Lieblingsclub.")
        else LoadContent(submitted, { ApiClient.api.search(submitted) }) { result ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                if (result.events.isEmpty() && result.bands.isEmpty() && result.locations.isEmpty()) item { EmptyMessage("Keine Treffer für „$submitted“.") }
                if (result.events.isNotEmpty()) item { SectionTitle("Konzerte · ${result.events.size}") }
                items(result.events, key = { "event${it.id}" }) { ConcertCard(it) { navigate("event/${it.id}") } }
                if (result.bands.isNotEmpty()) item { SectionTitle("Bands · ${result.bands.size}") }
                items(result.bands, key = { "band${it.id}" }) { EntityRow(it.name, it.city, it.logoUrl) { navigate("bands/${it.id}") } }
                if (result.locations.isNotEmpty()) item { SectionTitle("Orte · ${result.locations.size}") }
                items(result.locations, key = { "location${it.id}" }) { EntityRow(it.name, it.city) { navigate("locations/${it.id}") } }
            }
        }
    }
}

@Composable fun CalendarScreen(app: GigState, navigate: (String) -> Unit) {
    var monthString by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthString)
    var selected by rememberSaveable(monthString) { mutableStateOf<String?>(null) }
    val query = app.area.query() + mapOf("from" to month.atDay(1).toString(), "to" to month.atEndOfMonth().toString())
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { monthString = month.minusMonths(1).toString() }) { Text("‹", style = MaterialTheme.typography.headlineLarge) }
            Text(dateLabel(month.atDay(1).toString(), "MMMM yyyy"), style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { monthString = month.plusMonths(1).toString() }) { Text("›", style = MaterialTheme.typography.headlineLarge) }
        }
        AreaButton(app)
        LoadContent(query to app.revision, { allEvents(query) }) { events ->
            val counts = events.groupingBy { it.date }.eachCount()
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                item {
                    Row { listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEach { Text(it, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelLarge) } }
                    val offset = month.atDay(1).dayOfWeek.value - 1
                    val cells = (0 until ((offset + month.lengthOfMonth() + 6) / 7) * 7).toList()
                    cells.chunked(7).forEach { week -> Row {
                        week.forEach { cell ->
                            val day = cell - offset + 1
                            if (day !in 1..month.lengthOfMonth()) Spacer(Modifier.weight(1f).height(56.dp))
                            else {
                                val date = month.atDay(day).toString()
                                val count = counts[date] ?: 0
                                Surface(onClick = { selected = if (selected == date) null else date }, modifier = Modifier.weight(1f).heightIn(min = 56.dp).padding(2.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), color = if (selected == date) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                                    Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(day.toString(), color = if (date == LocalDate.now().toString()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                        Text(if (count > 0) "$count live" else "", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    } }
                }
                if (selected != null) item { TextButton(onClick = { selected = null }) { Text("Alle Konzerte im Monat") } }
                val shown = if (selected == null) events else events.filter { it.date == selected }
                if (shown.isEmpty()) item { EmptyMessage("Für diesen Zeitraum sind keine Konzerte gelistet.") }
                concerts(shown, { navigate("event/$it") })
            }
        }
    }
}
