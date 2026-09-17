@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.giglister.app.ui.browse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.giglister.app.data.api.ApiClient
import com.giglister.app.data.model.*
import com.giglister.app.ui.GigState
import com.giglister.app.ui.components.*
import kotlinx.serialization.json.*

@Composable fun GenrePreferences(app: GigState) {
    val action = rememberAction(app)
    LoadContent(Unit, { ApiClient.api.genres() }) { options ->
        var selected by rememberSaveable(app.me?.id) { mutableStateOf(app.me?.preferredGenres.orEmpty()) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Wähle deine Lieblingsgenres für persönliche Konzertvorschläge.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (options + selected).distinct().forEach { genre ->
                    FilterChip(genre in selected, { selected = if (genre in selected) selected - genre else selected + genre }, label = { Text(genre) })
                }
            }
            Button(enabled = !action.busy, onClick = { action.run {
                ApiClient.api.updateProfile(ProfileUpdateRequest(preferredGenres = selected)); app.changed(); app.genrePrompt = false
                app.notice = "Lieblingsgenres gespeichert."
            } }) { Text("Genres speichern") }
        }
    }
}

@Composable fun PasswordScreen(app: GigState, required: Boolean = false, done: () -> Unit = {}) {
    // Passwords intentionally stay out of saved-instance state.
    var current by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeated by remember { mutableStateOf("") }
    val action = rememberAction(app)
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.imePadding()) {
        item { PageTitle("Passwort ändern", if (required) "Setze zuerst ein eigenes Passwort. Dein aktuelles Passwort ist das temporäre Passwort, das du erhalten hast." else "Lege ein neues Passwort für dein Konto fest.") }
        item { OutlinedTextField(current, { current = it }, label = { Text("Aktuelles Passwort") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(password, { password = it }, label = { Text("Neues Passwort") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(repeated, { repeated = it }, label = { Text("Passwort wiederholen") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { Button(enabled = !action.busy, onClick = { action.run {
            require(current.isNotBlank() && password.length >= 8) { "Bitte aktuelles Passwort und ein neues Passwort mit mindestens 8 Zeichen eingeben." }
            require(password == repeated) { "Die neuen Passwörter stimmen nicht überein." }
            ApiClient.api.changePassword(ChangePasswordInput(current, password))
            current = ""; password = ""; repeated = ""; app.changed(); app.notice = "Passwort geändert."; done()
        } }) { Text("Passwort speichern") } }
        if (required) item { TextButton(enabled = !action.busy, onClick = { action.run { app.logout() } }) { Text("Abmelden") } }
    }
}

@Composable fun SubmissionsScreen(app: GigState, navigate: (String) -> Unit) {
    LoadContent(app.revision, { ApiClient.api.submissions() }) { submissions ->
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { PageTitle("Meine Vorschläge", "Hier siehst du, ob deine Konzertvorschläge bereits geprüft wurden.") }
            if (submissions.isEmpty()) item { EmptyMessage("Noch keine Vorschläge eingereicht.") }
            items(submissions, key = { it.id }) { submission ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val title = (submission.payload["title"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
                        ?: (submission.payload["bands"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull }?.joinToString(" + ")?.takeIf { it.isNotBlank() }
                    Text(title ?: "Vorschlag #${submission.id}", style = MaterialTheme.typography.titleMedium)
                    (submission.payload["date"] as? JsonPrimitive)?.contentOrNull?.let { Text(dateLabel(it)) }
                    Text(when (submission.status) { "PENDING" -> "Wird geprüft"; "APPROVED" -> "Angenommen"; "REJECTED" -> "Abgelehnt"; else -> submission.status })
                    submission.rejectionReason?.let { Text(it) }
                    if (submission.status == "APPROVED" && submission.resultEntityId != null) TextButton(onClick = { navigate("event/${submission.resultEntityId}") }) { Text("Konzert öffnen") }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable fun ConcertGenreFilter(app: GigState, query: Map<String, String>) {
    var show by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { show = true }) { Text(if (app.genres.isEmpty()) "Genres" else "Genres · ${app.genres.size}") }
    if (show) AlertDialog(onDismissRequest = { show = false }, title = { Text("Musikrichtungen") }, text = {
        LoadContent(query, { ApiClient.api.genreFilters(query) }) { options ->
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                item { Text("Mehrere Genres erweitern die Suche: Eine Übereinstimmung genügt.") }
                items((options.map { it.genre } + app.genres).distinct()) { genre ->
                    FilterChip(genre in app.genres, { app.genres = if (genre in app.genres) app.genres - genre else app.genres + genre }, label = { Text(genre + (options.find { it.genre == genre }?.eventCount?.let { " ($it)" } ?: "")) })
                }
                if (options.isEmpty() && app.genres.isEmpty()) item { Text("Keine Genres für diesen Zeitraum verfügbar.") }
            }
        }
    }, confirmButton = { TextButton(onClick = { show = false }) { Text("Fertig") } }, dismissButton = { TextButton(onClick = { app.genres = emptyList(); show = false }) { Text("Alle Genres") } })
}
