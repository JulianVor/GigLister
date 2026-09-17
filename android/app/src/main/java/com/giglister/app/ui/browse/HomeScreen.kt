package com.giglister.app.ui.browse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.giglister.app.data.api.ApiClient
import com.giglister.app.data.model.*
import com.giglister.app.ui.*
import com.giglister.app.ui.components.*
import java.time.LocalDate

@Composable fun HomeScreen(app: GigState, navigate: (String) -> Unit) {
    LoadContent(Triple(app.area, app.revision, app.me?.id), {
        val data = ApiClient.api.discover(app.area.query())
        val today = LocalDate.now().toString()
        val mapEvents = if (app.area.hasCoordinates) allEvents(app.area.query() + mapOf("from" to today, "to" to today)) else data.todayNearby
        data to mapEvents
    }) { (data, mapEvents) ->
        val me = app.me
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { PageTitle(me?.let { "Hallo ${it.username}" } ?: "Konzerte in\ndeiner Nähe", "Deine Musik. Deine nächsten Abende.") }
            item { AreaButton(app) }
            item { SectionTitle("Deine nächsten Konzerte") }
            if (me == null) item { TextButton(onClick = { navigate("login") }) { Text("Anmelden und Konzerte merken") } }
            else {
                val saved = upcomingSaved(me, LocalDate.now().toString())
                if (saved.isEmpty()) item { EmptyMessage("Noch keine kommenden Konzerte gemerkt.") }
                items(saved, key = { "saved-${it.id}" }) { event ->
                    EntityRow(event.eventSeries?.name ?: event.displayTitle, dateLabel(event.date) + (event.startTime?.take(5)?.let { " · $it" } ?: "")) {
                        navigate(event.eventSeries?.let { "festivals/${it.id}?saved=true" } ?: "event/${event.id}")
                    }
                }
                item { SectionTitle("Gefolgte Bands") }
                if (me.followedBands.isEmpty()) item { EmptyMessage("Folge deinen Lieblingsbands über ihr Profil.") }
                else item { LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(me.followedBands, key = { it.id }) { band ->
                        OutlinedCard(onClick = { navigate("bands/${band.id}") }, modifier = Modifier.width(185.dp)) {
                            Column(Modifier.padding(14.dp)) {
                                if (band.logoUrl != null) AsyncImage(band.logoUrl, null, Modifier.size(48.dp), contentScale = ContentScale.Fit)
                                Text(band.name, style = MaterialTheme.typography.titleMedium)
                                Text(band.nextEventDate?.let { dateLabel(it) } ?: "Noch kein Termin", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } }
            }
            item { SectionTitle("Heute in deiner Nähe") }
            if (app.area.hasCoordinates) item { ConcertMap(app.area, mapEvents) { navigate("locations/$it") } }
            else items(data.todayNearby, key = { "today-${it.id}" }) { ConcertCard(it) { navigate("event/${it.id}") } }
            if (mapEvents.isEmpty()) item { EmptyMessage("Heute ist noch nichts gelistet. Entdecke die kommenden Tage.") }
            item { TextButton(onClick = { navigate("places") }) { Text("Karte und weitere Tage") } }
            listOf((if (me == null) "Beliebt in deiner Nähe" else "Das könnte dich interessieren") to data.recommendedForYou,
                "Dieses Wochenende" to data.thisWeekend, "Neue Konzerte" to data.newEvents).forEachIndexed { index, (label, events) ->
                item { SectionTitle(label) }
                if (events.isEmpty()) item { EmptyMessage("Hier sind noch keine Konzerte gelistet.") }
                items(events, key = { "section-$index-${it.id}" }) { ConcertCard(it) { navigate("event/${it.id}") } }
            }
            item { SectionTitle("Orte mit kommenden Shows") }
            items(data.locationsWithUpcomingShows) { EntityRow(it.name, "${it.city} · ${it.upcomingEventCount} Konzerte") { navigate("locations/${it.id}") } }
            item { SectionTitle("Bands, die bald spielen") }
            items(data.bandsPlayingSoon) { EntityRow(it.name, it.city, it.logoUrl) { navigate("bands/${it.id}") } }
        }
    }
}
