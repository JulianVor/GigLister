package com.giglister.app.ui.browse

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.giglister.app.data.api.ApiClient
import com.giglister.app.data.model.*
import com.giglister.app.ui.GigState
import com.giglister.app.ui.components.*
import com.giglister.app.ui.edit.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class FestivalSlot(val event: EventResponse, val time: String?, val label: String, val genre: String?)
fun festivalSlots(events: List<EventResponse>): List<FestivalSlot> = events.flatMap { event ->
    val timed = event.bands.filter { it.startTime != null }
    if (timed.isEmpty()) listOf(FestivalSlot(event, event.startTime, event.displayTitle, event.bands.singleOrNull()?.genres?.firstOrNull()))
    else timed.map { FestivalSlot(event, it.startTime, it.name, it.genres.firstOrNull()) } +
        event.bands.filter { it.startTime == null }.let { remaining ->
            if (remaining.isEmpty()) emptyList() else listOf(FestivalSlot(event, event.startTime, remaining.joinToString(" + ") { it.name }, remaining.singleOrNull()?.genres?.firstOrNull()))
        }
}.sortedBy { runningMinutes(it.time, it.event.startTime) }

fun festivalTimeGroups(events: List<EventResponse>): List<Pair<String?, List<FestivalSlot>>> =
    festivalSlots(events).groupBy { runningMinutes(it.time, it.event.startTime) }.values.map { rows -> rows.first().time?.take(5) to rows }

@Composable fun FestivalsScreen(app: GigState, navigate: (String) -> Unit) {
    LoadContent(app.revision, { ApiClient.api.festivals() }) { festivals ->
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { PageTitle("Festivals", "Mehrere Bühnen. Ein gemeinsamer Abend.") }
            if (app.loggedIn) item { OutlinedButton(onClick = { navigate("festivals/new") }) { Text("+ Festival") } }
            if (festivals.isEmpty()) item { EmptyMessage("Noch keine Festivals gelistet.") }
            items(festivals, key = { it.id }) { festival ->
                Card(onClick = { navigate("festivals/${festival.id}") }) {
                    if (festival.titleImageUrl != null) AsyncImage(festival.titleImageUrl, null, Modifier.fillMaxWidth().height(150.dp), contentScale = ContentScale.Crop)
                    Text(festival.name, Modifier.padding(18.dp), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

@Composable fun FestivalScreen(id: Long, initiallySaved: Boolean, app: GigState, navigate: (String) -> Unit) {
    val context = LocalContext.current
    var savedOnly by rememberSaveable(id, initiallySaved) { mutableStateOf(initiallySaved) }
    var venue by rememberSaveable(id) { mutableStateOf<Long?>(null) }
    LoadContent(id to app.revision, { ApiClient.api.festival(id) }) { festival ->
        val useSaved = app.loggedIn && savedOnly
        val savedIds = app.me?.savedEvents.orEmpty().map { it.id }.toSet()
        val visible = festival.events.filter { !useSaved || it.id in savedIds }
            .map { if (useSaved) it.onlySavedActs(app.me?.savedActs.orEmpty()) else it }
        val venues = festival.events.map { it.location }.distinctBy { it.id }
        val days = visible.filter { venue == null || it.location.id == venue }.groupBy { it.date }.toSortedMap()
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (festival.titleImageUrl != null) item { AsyncImage(festival.titleImageUrl, null, Modifier.fillMaxWidth().height(200.dp), contentScale = ContentScale.Crop) }
            item { PageTitle(festival.name, festival.description) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!festival.ticketUrl.isNullOrBlank()) Button(onClick = { openLink(context, festival.ticketUrl) }) { Text("Tickets") }
                if (app.me.canEdit("EVENT_SERIES", id)) OutlinedButton(onClick = { navigate("festivals/$id/edit") }) { Text("Bearbeiten") }
            } }
            item { SectionTitle("Konzerte dieses Festivals") }
            if (app.loggedIn) item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!useSaved, { savedOnly = false }, label = { Text("Alle Konzerte") })
                FilterChip(useSaved, { savedOnly = true }, label = { Text("Gemerkte Konzerte") })
            } }
            if (venues.size > 1) item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(venue == null, { venue = null }, label = { Text("Alle Orte") })
                venues.forEach { location -> FilterChip(venue == location.id, { venue = if (venue == location.id) null else location.id }, label = { Text(location.name) }) }
            } }
            if (days.isEmpty()) item { EmptyMessage(if (useSaved) "Noch keine passenden Konzerte dieses Festivals gemerkt." else "Keine Konzerte für diese Auswahl.") }
            days.forEach { (date, events) ->
                item { SectionTitle(dateLabel(date, "EEEE, d. MMMM yyyy")) }
                val grouped = festivalTimeGroups(events)
                val grid = !useSaved && festival.timetableStyle == "GRID" && grouped.any { it.second.size > 1 }
                if (grid) item {
                    val columns = events.map { it.location }.distinctBy { it.id }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.width(70.dp)) {
                            Text("Zeit", Modifier.height(52.dp))
                            grouped.forEach { (time, rows) -> Text(time ?: "Offen", Modifier.height((116 * (rows.groupBy { it.event.location.id }.values.maxOfOrNull { it.size } ?: 1)).dp)) }
                        }
                        columns.forEach { location -> Column(Modifier.width(200.dp)) {
                            Text(location.name, Modifier.height(52.dp), style = MaterialTheme.typography.titleMedium)
                            grouped.forEach { (_, rows) ->
                                Column(Modifier.height((116 * (rows.groupBy { it.event.location.id }.values.maxOfOrNull { it.size } ?: 1)).dp)) {
                                    rows.filter { it.event.location.id == location.id }.forEach { slot -> SlotCard(slot, navigate, Modifier.height(112.dp)) }
                                }
                            }
                        } }
                    }
                } else grouped.forEach { (time, rows) ->
                    item { Text((time ?: "Uhrzeit offen") + if (rows.size > 1) " · Zeitgleich" else "", style = MaterialTheme.typography.titleMedium) }
                    items(rows) { SlotCard(it, navigate) }
                }
            }
        }
    }
}

@Composable private fun SlotCard(slot: FestivalSlot, navigate: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(onClick = { navigate("event/${slot.event.id}") }, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(slot.label, style = MaterialTheme.typography.titleMedium)
            Text(slot.event.location.name, style = MaterialTheme.typography.bodySmall)
            slot.genre?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
            if (slot.event.status == "CANCELLED") Text("Abgesagt", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable fun FestivalEditor(id: Long?, app: GigState, navigate: (String) -> Unit) {
    if (id == null) FestivalForm(null, app, navigate)
    else LoadContent(id, { app.reloadMe(); ApiClient.api.festival(id) }) { festival ->
        if (app.me.canEdit("EVENT_SERIES", id)) FestivalForm(festival, app, navigate) else EmptyMessage("Du hast keine Bearbeitungsrechte für dieses Festival.")
    }
}
private val festivalSaver = Saver<FestivalInput, String>({ Json.encodeToString(it) }, { Json.decodeFromString(it) })
@Composable private fun FestivalForm(initial: FestivalResponse?, app: GigState, navigate: (String) -> Unit) {
    var input by rememberSaveable(initial?.id, stateSaver = festivalSaver) { mutableStateOf(initial?.let { FestivalInput(it.name, it.description, it.titleImageUrl, it.ticketUrl, it.timetableStyle) } ?: FestivalInput()) }
    var uploads by remember { mutableIntStateOf(0) }
    var saved by rememberSaveable { mutableStateOf(false) }
    val action = rememberAction(app)
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.imePadding()) {
        item { PageTitle(if (initial == null) "Festival anlegen" else "Festival bearbeiten") }
        item { FormField("Name *", input.name, { input = input.copy(name = it) }) }
        item { FormField("Beschreibung", input.description.orEmpty(), { input = input.copy(description = it) }, multiline = true) }
        item { FormField("Ticketlink", input.ticketUrl.orEmpty(), { input = input.copy(ticketUrl = it) }) }
        item { Text("Dieser Ticketlink gilt auch für die zugeordneten Konzerte.") }
        item { ImageField("Festivalbild", input.titleImageUrl, app, { uploads += if (it) 1 else -1 }) { input = input.copy(titleImageUrl = it) } }
        item { SectionTitle("Spielplan"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("LIST" to "Liste", "GRID" to "Raster").forEach { (value, label) -> FilterChip(input.timetableStyle == value, { input = input.copy(timetableStyle = value) }, label = { Text(label) }) }
        } }
        item { Text("Konzerte werden über ihr Konzertformular zugeordnet. Gemerkte Konzerte erscheinen immer als Liste.") }
        item { Button(enabled = !action.busy && uploads == 0 && !saved, onClick = { action.run {
            require(input.name.isNotBlank()) { "Bitte einen Namen eingeben." }; validateWebUrl(input.ticketUrl)
            val clean = input.copy(name = input.name.trim(), description = input.description?.optional(), ticketUrl = input.ticketUrl?.optional())
            val result = if (initial == null) ApiClient.api.createFestival(clean) else ApiClient.api.updateFestival(initial.id, clean)
            saved = true; app.changed(); navigate("festivals/${result.id}")
        } }) { Text("Festival speichern") } }
    }
}
