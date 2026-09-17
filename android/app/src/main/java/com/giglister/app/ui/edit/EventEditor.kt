package com.giglister.app.ui.edit

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.giglister.app.data.api.ApiClient
import com.giglister.app.data.model.*
import com.giglister.app.ui.GigState
import com.giglister.app.ui.components.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime

@Composable fun EventEditor(id: Long?, app: GigState, navigate: (String) -> Unit) {
    if (id == null) EventForm(null, app, navigate)
    else LoadContent(id, { app.reloadMe(); ApiClient.api.event(id) }) { event ->
        if (app.me.canEdit(event)) EventForm(event, app, navigate) else EmptyMessage("Für dieses Konzert hast du keine Bearbeitungsrechte.")
    }
}

private val eventSaver = Saver<EventInput, String>(save = { Json.encodeToString(it) }, restore = { Json.decodeFromString<EventInput>(it) })
@Composable private fun EventForm(event: EventResponse?, app: GigState, navigate: (String) -> Unit) {
    var input by rememberSaveable(event?.id, stateSaver = eventSaver) { mutableStateOf(EventInput(
        date = event?.date ?: LocalDate.now().toString(),
        location = event?.location?.let { EntityRef(it.id, it.name, it.city) } ?: EntityRef(),
        bands = event?.bands?.map { EntityRef(it.id, it.name, it.city, startTime = it.startTime) } ?: emptyList(),
        title = event?.title, startTime = event?.startTime, description = event?.description, ticketUrl = event?.ticketUrl,
        titleImageUrl = event?.titleImageUrl, bandImageDisplay = event?.bandImageDisplay ?: "PHOTO", eventSeriesId = event?.eventSeries?.id
    )) }
    var uploads by remember { mutableIntStateOf(0) }
    var savedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var submittedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val action = rememberAction(app)
    val context = LocalContext.current
    if (submittedId != null) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PageTitle("Vorschlag eingereicht", "Dein Konzert wird geprüft und nach Freigabe veröffentlicht.")
            Button(onClick = { navigate("submissions") }) { Text("Meine Vorschläge") }
            TextButton(onClick = { navigate("home") }) { Text("Zur Startseite") }
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.imePadding()) {
        item { PageTitle(if (event == null) "Bring Musik\nauf die Karte." else "Konzert bearbeiten", "Wann, wo und wer? Hier finden Fans ihren nächsten Abend.") }
        item { SectionTitle("01 · Wann?") }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val date = LocalDate.parse(input.date)
                DatePickerDialog(context, { _, year, month, day -> input = input.copy(date = LocalDate.of(year, month + 1, day).toString()) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
            }, modifier = Modifier.weight(1f)) { Text(dateLabel(input.date, "d. MMM yyyy")) }
            OutlinedButton(onClick = {
                val time = input.startTime?.let { LocalTime.parse(it) } ?: LocalTime.of(20, 0)
                TimePickerDialog(context, { _, hour, minute -> input = input.copy(startTime = LocalTime.of(hour, minute).toString()) }, time.hour, time.minute, true).show()
            }, modifier = Modifier.weight(1f)) { Text(input.startTime?.take(5) ?: "Uhrzeit (optional)") }
        } }
        if (input.startTime != null) item { TextButton(onClick = { input = input.copy(startTime = null) }) { Text("Uhrzeit entfernen") } }
        item { SectionTitle("02 · Wo?") }
        item { EntitySelector("Ort", "locations", input.location.takeIf { it.id != null || it.name != null }, app) { input = input.copy(location = it) } }
        item { SectionTitle("03 · Wer spielt?") }
        input.bands.forEachIndexed { index, band -> item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    EntitySelector("Band ${index + 1}", "bands", band, app) { value -> input = input.copy(bands = input.bands.toMutableList().apply { set(index, value.copy(startTime = band.startTime)) }) }
                    TextButton(onClick = {
                        val time = LocalTime.parse(band.startTime ?: input.startTime ?: "20:00")
                        TimePickerDialog(context, { _, hour, minute -> input = input.copy(bands = input.bands.toMutableList().apply { set(index, band.copy(startTime = LocalTime.of(hour, minute).toString())) }) }, time.hour, time.minute, true).show()
                    }) { Text(band.startTime?.let { "Auftritt: ${it.take(5)}" } ?: "Eigene Auftrittszeit") }
                    if (band.startTime != null) TextButton(onClick = { input = input.copy(bands = input.bands.toMutableList().apply { set(index, band.copy(startTime = null)) }) }) { Text("Auftrittszeit entfernen") }
                }
                TextButton(onClick = { input = input.copy(bands = input.bands.filterIndexed { i, _ -> i != index }) }) { Text("−") }
            }
        } }
        item { EntitySelector("Weitere Band", "bands", null, app) { band ->
            if (band.id != null && input.bands.any { it.id == band.id }) app.notice = "Diese Band steht bereits im Line-up."
            else input = input.copy(bands = input.bands + band)
        } }
        item { SectionTitle("04 · Der Abend") }
        item { FestivalSelector(input.eventSeriesId, app.revision) { input = input.copy(eventSeriesId = it) } }
        item { FormField("Titel (optional, sonst Line-up)", input.title.orEmpty(), { input = input.copy(title = it) }) }
        item { FormField("Ticketlink (https://…)", input.ticketUrl.orEmpty(), { input = input.copy(ticketUrl = it) }) }
        item { FormField("Beschreibung", input.description.orEmpty(), { input = input.copy(description = it) }, multiline = true) }
        item { ImageField("Konzertbild", input.titleImageUrl, app, { uploads += if (it) 1 else -1 }) { input = input.copy(titleImageUrl = it) } }
        item { Text("Ohne eigenes Konzertbild:", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("PHOTO" to "Bandfotos", "LOGO" to "Bandlogos").forEach { (mode, label) -> FilterChip(input.bandImageDisplay == mode, { input = input.copy(bandImageDisplay = mode) }, label = { Text(label) }) } }
        }
        item { Button(enabled = !action.busy && uploads == 0 && savedId == null, onClick = { action.run {
            require(input.location.id != null || !input.location.name.isNullOrBlank()) { "Bitte einen Ort auswählen." }
            require(input.bands.isNotEmpty()) { "Bitte mindestens eine Band hinzufügen." }
            LocalDate.parse(input.date); input.startTime?.let { LocalTime.parse(it) }; validateWebUrl(input.ticketUrl)
            val clean = input.copy(title = input.title?.optional(), description = input.description?.optional(), ticketUrl = input.ticketUrl?.optional())
            val result = if (event == null) {
                val created = ApiClient.api.createEvent(clean)
                if (!created.published) {
                    submittedId = requireNotNull(created.submission?.id) { "Die Einreichung konnte nicht bestätigt werden." }
                    app.changed()
                    return@run
                }
                requireNotNull(created.event) { "Die Veröffentlichung konnte nicht bestätigt werden." }
            } else ApiClient.api.updateEvent(event.id, clean)
            savedId = result.id
            app.notice = "Konzert gespeichert."
            navigate("event/${result.id}")
            app.changed()
        } }, modifier = Modifier.fillMaxWidth()) { Text(if (action.busy) "Wird gespeichert …" else if (event == null) "Konzert einreichen" else "Änderungen speichern") } }
        if (event == null) item { Text("Mit direkten Rechten an einer beteiligten Band oder am Ort wird das Konzert sofort veröffentlicht. Sonst wird dein Vorschlag zuerst geprüft.", style = MaterialTheme.typography.bodySmall) }
        if (savedId != null) item { TextButton(onClick = { navigate("event/$savedId") }) { Text("Gespeichertes Konzert öffnen") } }
    }
}

@Composable private fun FestivalSelector(id: Long?, revision: Int, onSelect: (Long?) -> Unit) {
    var show by remember { mutableStateOf(false) }
    LoadContent(revision, { ApiClient.api.festivals() }) { festivals ->
        OutlinedButton(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) { Text(festivals.find { it.id == id }?.name ?: if (id != null) "Festival #$id" else "Festival (optional)") }
        if (show) AlertDialog(onDismissRequest = { show = false }, title = { Text("Festival auswählen") }, text = {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 360.dp)) {
                item { TextButton(onClick = { onSelect(null); show = false }) { Text("Kein Festival") } }
                festivals.forEach { festival -> item { TextButton(onClick = { onSelect(festival.id); show = false }) { Text(festival.name) } } }
            }
        }, confirmButton = { TextButton(onClick = { show = false }) { Text("Schließen") } })
    }
}
