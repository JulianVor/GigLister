@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.giglister.app.ui.browse

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.giglister.app.data.api.ApiClient
import com.giglister.app.data.model.*
import com.giglister.app.ui.*
import com.giglister.app.ui.components.*
import kotlinx.coroutines.tasks.await

@Composable fun EventDetailScreen(id: Long, app: GigState, navigate: (String) -> Unit) {
    val context = LocalContext.current
    val action = rememberAction(app)
    var cancelDialog by remember { mutableStateOf(false) }
    LoadContent(id to app.revision, { ApiClient.api.event(id) }) { event ->
        val saved = app.me?.savedEvents?.any { it.id == id } == true
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (event.titleImageUrl != null || (event.bandImageDisplay == "PHOTO" && event.bands.any { it.titleImageUrl != null })) {
                item { ConcertArtwork(event, Modifier.fillMaxWidth().height(210.dp), labels = false) }
            }
            if (event.status == "CANCELLED") item { EmptyMessage("Dieses Konzert wurde abgesagt.") }
            item { SectionTitle(dateLabel(event.date, "EEEE, d. MMMM yyyy") + (event.startTime?.take(5)?.let { " · $it" } ?: "")) }
            item { PageTitle(event.displayTitle) }
            event.eventSeries?.let { festival -> item { TextButton(onClick = { navigate("festivals/${festival.id}") }) { Text("Festival: ${festival.name}") } } }
            item { EntityRow(event.location.name, event.location.city, event.location.titleImageUrl) {
                if (event.location.linkable || app.loggedIn) navigate("locations/${event.location.id}")
                else app.notice = "Für diesen Ort ist noch kein öffentliches Profil verfügbar."
            } }
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!event.effectiveTicketUrl.isNullOrBlank()) Button(onClick = { openLink(context, event.effectiveTicketUrl) }) { Text("Tickets") }
                OutlinedButton(enabled = !action.busy, onClick = {
                    if (!app.loggedIn) navigate("login") else action.run { if (saved) ApiClient.api.unsave(id) else ApiClient.api.save(id); app.changed() }
                }) { Text(if (saved) "Gemerkt ✓" else "Merken") }
                if (app.me.canEdit(event)) OutlinedButton(onClick = { navigate("event/$id/edit") }) { Text("Bearbeiten") }
            } }
            if (!event.description.isNullOrBlank()) item { Text(event.description, style = MaterialTheme.typography.bodyLarge) }
            item { SectionTitle("Das Line-up") }
            items(event.orderedBands()) { band ->
                Column {
                    EntityRow(band.name, listOfNotNull(band.startTime?.take(5), band.genres.joinToString(" · ").takeIf { it.isNotBlank() }).joinToString(" · "), band.logoUrl) {
                        if (band.linkable || app.loggedIn) navigate("bands/${band.id}") else app.notice = "Für diese Band ist noch kein öffentliches Profil verfügbar."
                    }
                    if (event.eventSeries != null) {
                        val actSaved = app.me?.savedActs?.any { it.eventId == id && it.bandId == band.id } == true
                        TextButton(enabled = !action.busy, onClick = {
                            if (!app.loggedIn) navigate("login") else action.run {
                                if (actSaved) ApiClient.api.unsaveAct(id, band.id) else ApiClient.api.saveAct(id, band.id)
                                app.changed()
                            }
                        }) { Text(if (actSaved) "${band.name}: Auftritt gemerkt ✓" else "${band.name}: Auftritt merken") }
                    }
                }
            }
            item {
                LoadContent(event.location.id to app.loggedIn, { ApiClient.api.entity("locations", event.location.id) }) { venue ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionTitle("Dein Weg zur Bühne")
                        Text(listOfNotNull(venue.name, venue.address, listOfNotNull(venue.postalCode, venue.city).joinToString(" ")).joinToString("\n"))
                        OutlinedButton(onClick = { openLink(context, routeUrl(venue)) }) { Text("Route planen") }
                        val others = venue.upcomingEvents.filter { it.id != id }.take(4)
                        if (others.isNotEmpty()) SectionTitle("Auch hier live")
                        others.forEach { ConcertCard(it) { navigate("event/${it.id}") } }
                    }
                }
            }
            if (app.me.canEdit(event)) {
                item { TextButton(onClick = { cancelDialog = true }) { Text(if (event.status == "CANCELLED") "Konzert wieder aktivieren" else "Konzert absagen", color = MaterialTheme.colorScheme.error) } }
                item { DeleteContentButton("events", id, "Konzert", app, navigate) }
            }
        }
        if (cancelDialog) AlertDialog(onDismissRequest = { cancelDialog = false }, title = { Text(if (event.status == "CANCELLED") "Konzert wieder aktivieren?" else "Konzert absagen?") }, text = { Text(if (event.status == "CANCELLED") "Das Konzert wird wieder als veröffentlicht angezeigt." else "Das Konzert wird für Besucher als abgesagt angezeigt.") },
            confirmButton = { TextButton(enabled = !action.busy, onClick = { action.run { ApiClient.api.eventStatus(id, StatusInput(if (event.status == "CANCELLED") "PUBLISHED" else "CANCELLED")); cancelDialog = false; app.changed() } }) { Text("Bestätigen") } },
            dismissButton = { TextButton(onClick = { cancelDialog = false }) { Text("Zurück") } })
    }
}

fun routeUrl(venue: EntityDetails): String {
    val destination = if (venue.latitude != null && venue.longitude != null) "${venue.latitude},${venue.longitude}" else listOfNotNull(venue.name, venue.address, venue.postalCode, venue.city).joinToString(" ")
    return "https://www.google.com/maps/dir/?api=1&destination=${android.net.Uri.encode(destination)}"
}

@Composable fun EntityDetailScreen(kind: String, id: Long, app: GigState, navigate: (String) -> Unit) {
    val context = LocalContext.current
    val action = rememberAction(app)
    val isBand = kind == "bands"
    val type = if (isBand) "BAND" else "LOCATION"
    var claim by remember { mutableStateOf(false) }
    var claimMessage by remember { mutableStateOf("") }
    var claimSent by remember { mutableStateOf(false) }
    LoadContent(Triple(kind, id, app.revision), { ApiClient.api.entity(kind, id) }) { entity ->
        val following = app.me?.followedBands?.any { it.id == id } == true
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (entity.titleImageUrl != null) item { AsyncImage(entity.titleImageUrl, null, Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Crop) }
            if (isBand) item { BandStories(entity, app, navigate) }
            item { PageTitle(entity.name, listOfNotNull(entity.city, entity.country).joinToString(" · ")) }
            if (app.me.canEdit(type, id)) item { Text("Status: ${statusLabel(entity.status)}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (isBand && entity.genres.isNotEmpty()) item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { entity.genres.forEach { SuggestionChip(onClick = {}, label = { Text(it) }) } } }
            if (!entity.shortDescription.isNullOrBlank()) item { Text(entity.shortDescription, style = MaterialTheme.typography.bodyLarge) }
            if (!isBand && !entity.address.isNullOrBlank()) item { Text(listOfNotNull(entity.address, entity.postalCode).joinToString(" · ")) }
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isBand) Button(enabled = !action.busy, onClick = {
                    if (!app.loggedIn) navigate("login") else action.run { if (following) ApiClient.api.unfollow(id) else ApiClient.api.follow(id); app.changed() }
                }) { Text(if (following) "Gefolgt ✓" else "Band folgen") }
                if (!entity.website.isNullOrBlank()) OutlinedButton(onClick = { openLink(context, entity.website) }) { Text("Website") }
                if (!isBand) OutlinedButton(onClick = { openLink(context, routeUrl(entity)) }) { Text("Route") }
                if (app.me.canEdit(type, id)) OutlinedButton(onClick = { navigate("$kind/$id/edit") }) { Text("Bearbeiten") }
            } }
            item { SectionTitle("Nächste Konzerte") }
            if (entity.upcomingEvents.isEmpty()) item { EmptyMessage("Noch keine kommenden Konzerte gelistet.") }
            concerts(entity.upcomingEvents, { navigate("event/$it") }, "upcoming")
            entity.pastEventsByYear.toSortedMap(compareByDescending { it.toIntOrNull() ?: 0 }).forEach { (year, events) ->
                item { var expanded by remember { mutableStateOf(false) }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { expanded = !expanded }) { Text("${if (expanded) "−" else "+"} $year · ${events.size} vergangene Konzerte") }
                        if (expanded) events.forEach { ConcertCard(it) { navigate("event/${it.id}") } }
                    }
                }
            }
            if (entity.unclaimed && app.loggedIn) item {
                OutlinedButton(enabled = !claimSent, onClick = { claim = true }, modifier = Modifier.fillMaxWidth()) { Text(if (claimSent) "Antrag eingereicht" else "Das ist ${if (isBand) "meine Band" else "mein Ort"}") }
            }
            if (app.me.canManage(type, id)) item { DeleteContentButton(kind, id, if (isBand) "Band" else "Ort", app, navigate) }
        }
        if (claim) AlertDialog(onDismissRequest = { if (!action.busy) claim = false }, title = { Text("Profil beanspruchen") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Beschreibe kurz deine Verbindung zu ${entity.name}. Dein Antrag wird anschließend geprüft.")
                OutlinedTextField(claimMessage, { claimMessage = it }, label = { Text("Nachricht") })
            }
        }, confirmButton = { TextButton(enabled = !action.busy, onClick = { action.run {
            ApiClient.api.claim(kind, id, ClaimInput(claimMessage)); claimSent = true; claim = false; app.notice = "Dein Antrag wurde eingereicht."
        } }) { Text("Antrag senden") } }, dismissButton = { TextButton(onClick = { claim = false }) { Text("Abbrechen") } })
    }
}

fun statusLabel(status: String): String = when (status) { "DRAFT" -> "Entwurf"; "PUBLISHED" -> "Veröffentlicht"; "ARCHIVED" -> "Archiviert"; "STUB" -> "Unvollständig"; "CANCELLED" -> "Abgesagt"; else -> status }

@Composable fun MyGigListerScreen(app: GigState, navigate: (String) -> Unit) {
    val action = rememberAction(app)
    var locationDialog by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) action.run { val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await(); ApiClient.api.registerDeviceToken(DeviceTokenRequest(token)); app.notice = "Benachrichtigungen aktiviert." }
        else app.notice = "Benachrichtigungen bleiben deaktiviert."
    }
    if (!app.loggedIn) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            PageTitle("Deine Musik.\nDeine Abende.", "Merke Konzerte, folge Bands und behalte deine nächsten Shows im Blick.")
            Button(onClick = { navigate("login") }, modifier = Modifier.fillMaxWidth()) { Text("Anmelden") }
            OutlinedButton(onClick = { navigate("register") }, modifier = Modifier.fillMaxWidth()) { Text("Konto erstellen") }
            Text("Entdecken funktioniert auch ohne Konto.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LoadContent(app.revision, { app.reloadMe(); ApiClient.api.myEvents() }) { myEvents ->
        val me = app.me ?: return@LoadContent
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { PageTitle("Mein GigLister", "${me.username} · ${me.email}") }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { navigate("genres") }) { Text("Lieblingsgenres") }
                OutlinedButton(onClick = { navigate("password") }) { Text("Passwort ändern") }
            } }
            item { OutlinedButton(onClick = { navigate("submissions") }) { Text("Meine Vorschläge") } }
            item { SectionTitle("Dein Standort") }
            item { Text(SearchArea(me.homeCity.orEmpty(), me.homeLatitude, me.homeLongitude, me.radiusKm ?: 25).label) }
            item { OutlinedButton(enabled = !action.busy, onClick = { locationDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Heimatort und Umkreis ändern") } }
            item { OutlinedButton(enabled = !action.busy, onClick = {
                if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else action.run { val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await(); ApiClient.api.registerDeviceToken(DeviceTokenRequest(token)); app.notice = "Benachrichtigungen aktiviert." }
            }, modifier = Modifier.fillMaxWidth()) { Text("Über neue Konzerte informieren") } }
            item { SectionTitle("Gemerkt · ${me.savedEvents.size}") }
            if (me.savedEvents.isEmpty()) item { EmptyMessage("Deine nächsten Lieblingsabende warten noch auf dich. Tippe bei einem Konzert auf Merken.") }
            concerts(me.savedEvents, { navigate("event/$it") }, "saved")
            item { SectionTitle("Gefolgte Bands") }
            if (me.followedBands.isEmpty()) item { EmptyMessage("Folge einer Band über ihr Profil.") }
            items(me.followedBands) { EntityRow(it.name, it.nextEventDate?.let { date -> "Nächstes Konzert: ${dateLabel(date)}" } ?: "Noch kein neuer Termin") { navigate("bands/${it.id}") } }
            item { SectionTitle("Meine Bands, Orte und Festivals") }
            items(me.managedEntities) { entity -> EntityRow(entity.name, "${when(entity.entityType) { "BAND" -> "Band"; "EVENT_SERIES" -> "Festival"; else -> "Ort" }} · ${if (entity.permission == "MANAGE") "Verwalten" else "Bearbeiten"}") {
                navigate("${when(entity.entityType) { "BAND" -> "bands"; "EVENT_SERIES" -> "festivals"; else -> "locations" }}/${entity.entityId}")
            } }
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { navigate("bands/new") }) { Text("+ Band") }
                OutlinedButton(onClick = { navigate("locations/new") }) { Text("+ Ort") }
                OutlinedButton(onClick = { navigate("festivals/new") }) { Text("+ Festival") }
                Button(onClick = { navigate("event/new") }) { Text("+ Konzert") }
            } }
            if (myEvents.isNotEmpty()) item { SectionTitle("Veranstaltungen meiner Bands") }
            concerts(myEvents, { navigate("event/$it") }, "my")
            item { TextButton(enabled = !action.busy, onClick = { action.run { app.logout() } }, modifier = Modifier.fillMaxWidth()) { Text("Abmelden") } }
        }
    }
    if (locationDialog) AreaDialog(app, { locationDialog = false }) { area -> action.run {
        require(area.hasCoordinates) { "Für den gespeicherten Heimatort wird eine genaue Position benötigt. Bitte Stadt erneut suchen oder GPS verwenden." }
        ApiClient.api.updateProfile(ProfileUpdateRequest(homeCity = area.city, homeLatitude = area.lat, homeLongitude = area.lon, radiusKm = area.radius))
        app.changeArea(area); locationDialog = false; app.changed(); app.notice = "Heimatort und Suchradius gespeichert."
    } }
}

@Composable private fun DeleteContentButton(kind: String, id: Long, label: String, app: GigState, navigate: (String) -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    val action = rememberAction(app)
    TextButton(onClick = { confirm = true }) { Text("$label löschen", color = MaterialTheme.colorScheme.error) }
    if (confirm) AlertDialog(onDismissRequest = { if (!action.busy) confirm = false }, title = { Text("$label endgültig löschen?") }, text = {
        Text(if (kind == "events") "Das Konzert wird dauerhaft entfernt." else "Das Profil wird dauerhaft entfernt. Sind noch Konzerte zugeordnet, verweigert der Server das Löschen.")
    }, confirmButton = { TextButton(enabled = !action.busy, onClick = { action.run {
        ApiClient.api.deleteEntity(kind, id); confirm = false; navigate("home"); app.changed(); app.notice = "$label gelöscht."
    } }) { Text("Endgültig löschen") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Abbrechen") } })
}
