package com.giglister.app.ui.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.giglister.app.data.api.ApiClient
import com.giglister.app.data.model.*
import com.giglister.app.ui.GigState
import com.giglister.app.ui.browse.statusLabel
import com.giglister.app.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable fun EntityEditor(kind: String, id: Long?, app: GigState, navigate: (String) -> Unit) {
    if (id == null) EntityForm(kind, null, app, navigate)
    else LoadContent(kind to id, { app.reloadMe(); ApiClient.api.entity(kind, id) }) { entity ->
        if (app.me.canEdit(if (kind == "bands") "BAND" else "LOCATION", id)) EntityForm(kind, entity, app, navigate)
        else EmptyMessage("Für dieses Profil hast du keine Bearbeitungsrechte.")
    }
}

private val entitySaver = Saver<EntityDetails, String>(save = { Json.encodeToString(it) }, restore = { Json.decodeFromString<EntityDetails>(it) })
@Composable private fun EntityForm(kind: String, initial: EntityDetails?, app: GigState, navigate: (String) -> Unit) {
    val band = kind == "bands"
    val type = if (band) "BAND" else "LOCATION"
    var draft by rememberSaveable(kind, initial?.id, stateSaver = entitySaver) { mutableStateOf(initial ?: EntityDetails(0, "", status = "DRAFT")) }
    var genres by rememberSaveable { mutableStateOf(initial?.genres?.joinToString(", ").orEmpty()) }
    var createdId by rememberSaveable { mutableStateOf<Long?>(null) }
    var uploads by remember { mutableIntStateOf(0) }
    var saved by rememberSaveable { mutableStateOf(false) }
    val action = rememberAction(app)
    val canManage = initial == null || app.me.canManage(type, initial.id)
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.imePadding()) {
        item { PageTitle(if (initial == null) if (band) "Deine Band.\nEure Bühne." else "Ein Ort für\nLive-Musik." else "${initial.name}\nbearbeiten") }
        item { FormField("Name *", draft.name, { draft = draft.copy(name = it) }) }
        item { FormField(if (band) "Stadt" else "Stadt *", draft.city.orEmpty(), { draft = draft.copy(city = it, latitude = null, longitude = null) }) }
        if (initial == null && createdId == null && draft.name.length >= 2) item {
            LoadContent(draft.name to draft.city, { delay(350); ApiClient.api.duplicates(kind, draft.name.trim(), draft.city?.optional()) }) { candidates ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (candidates.isNotEmpty()) Text("Schon vorhanden? Prüfe diese Profile vor dem Anlegen.", style = MaterialTheme.typography.titleMedium)
                    candidates.forEach { candidate -> EntityRow(candidate.name, candidate.city) { navigate("$kind/${candidate.id}") } }
                }
            }
        }
        item { FormField("Land", draft.country.orEmpty(), { draft = draft.copy(country = it) }) }
        if (band) {
            item { FormField("Genres (durch Komma getrennt)", genres, { genres = it }) }
            item { FormField("Über die Band", draft.shortDescription.orEmpty(), { draft = draft.copy(shortDescription = it) }, multiline = true) }
        } else {
            item { FormField("Straße und Hausnummer", draft.address.orEmpty(), { draft = draft.copy(address = it, latitude = null, longitude = null) }) }
            item { FormField("Postleitzahl", draft.postalCode.orEmpty(), { draft = draft.copy(postalCode = it, latitude = null, longitude = null) }) }
        }
        item { FormField("Website (https://…)", draft.website.orEmpty(), { draft = draft.copy(website = it) }) }
        item { ImageField("Titelbild", draft.titleImageUrl, app, { uploads += if (it) 1 else -1 }) { draft = draft.copy(titleImageUrl = it) } }
        if (band) item { ImageField("Profilbild", draft.profileImageUrl, app, { uploads += if (it) 1 else -1 }) { draft = draft.copy(profileImageUrl = it) } }
        item { ImageField("Logo", draft.logoUrl, app, { uploads += if (it) 1 else -1 }) { draft = draft.copy(logoUrl = it) } }
        if (canManage) item {
            SectionTitle("Veröffentlichung")
            listOf("DRAFT", "PUBLISHED", "ARCHIVED").forEach { status ->
                FilterChip(selected = draft.status == status, onClick = { draft = draft.copy(status = status) }, label = { Text(statusLabel(status)) }, modifier = Modifier.padding(end = 8.dp))
            }
        }
        item { Button(enabled = !action.busy && uploads == 0 && !saved, modifier = Modifier.fillMaxWidth(), onClick = { action.run {
            require(draft.name.isNotBlank()) { "Bitte einen Namen angeben." }
            if (!band) require(!draft.city.isNullOrBlank()) { "Bitte eine Stadt angeben." }
            validateWebUrl(draft.website)
            val id = initial?.id ?: createdId
            val result = if (band) {
                val input = BandInput(draft.name.trim(), draft.city?.optional(), draft.country?.optional(), draft.shortDescription?.optional(), draft.website?.optional(), draft.logoUrl, draft.titleImageUrl, genres.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct(), draft.profileImageUrl)
                if (id == null) ApiClient.api.createBand(input) else ApiClient.api.updateBand(id, input)
            } else {
                val input = LocationInput(draft.name.trim(), draft.city!!.trim(), draft.address?.optional(), draft.postalCode?.optional(), draft.country?.optional(), draft.website?.optional(), draft.logoUrl, draft.titleImageUrl, draft.latitude, draft.longitude)
                if (id == null) ApiClient.api.createLocation(input) else ApiClient.api.updateLocation(id, input)
            }
            createdId = result.id
            if (canManage && result.status != draft.status) ApiClient.api.entityStatus(kind, result.id, StatusInput(draft.status))
            saved = true
            app.notice = "Profil gespeichert."
            navigate("$kind/${result.id}"); app.changed()
        } }) { Text(if (action.busy) "Wird gespeichert …" else "Profil speichern") } }
        if (saved) item { TextButton(onClick = { navigate("$kind/$createdId") }) { Text("Gespeichertes Profil öffnen") } }
        if (initial != null && canManage) item { PermissionsEditor(kind, initial.id, app) }
    }
}

@Composable private fun PermissionsEditor(kind: String, id: Long, app: GigState) {
    var version by remember { mutableIntStateOf(0) }
    var userId by rememberSaveable { mutableStateOf("") }
    var level by rememberSaveable { mutableStateOf("EDIT") }
    var revoke by remember { mutableStateOf<PermissionResponse?>(null) }
    val action = rememberAction(app)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Gemeinsam verwalten")
        Text("Bearbeiten erlaubt die Profilpflege. Verwalten erlaubt zusätzlich Veröffentlichung und die Vergabe von Rechten.")
        LoadContent(version, { ApiClient.api.permissions(kind, id) }) { permissions ->
            permissions.forEach { permission ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text(permission.username, style = MaterialTheme.typography.titleMedium); Text(if (permission.permission == "MANAGE") "Verwalten" else "Bearbeiten") }
                    TextButton(enabled = !action.busy, onClick = { revoke = permission }) { Text("Entfernen") }
                }
            }
        }
        FormField("Nutzer-ID", userId, { userId = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(level == "EDIT", { level = "EDIT" }, label = { Text("Bearbeiten") })
            FilterChip(level == "MANAGE", { level = "MANAGE" }, label = { Text("Verwalten") })
        }
        OutlinedButton(enabled = !action.busy, onClick = { action.run {
            val target = userId.toLongOrNull()
            require(target != null && target > 0) { "Bitte eine gültige Nutzer-ID eingeben." }
            ApiClient.api.grant(kind, id, PermissionInput(target, level)); userId = ""; version++; app.changed()
        } }) { Text("Nutzer hinzufügen") }
    }
    if (revoke != null) AlertDialog(onDismissRequest = { revoke = null }, title = { Text("Berechtigung entfernen?") }, text = { Text("${revoke!!.username} verliert den Zugriff auf dieses Profil.${if (revoke!!.userId == app.me?.id) " Das ist dein eigener Zugriff." else ""}") }, confirmButton = {
        TextButton(enabled = !action.busy, onClick = { action.run { ApiClient.api.revoke(kind, id, revoke!!.userId); revoke = null; version++; app.changed() } }) { Text("Entfernen") }
    }, dismissButton = { TextButton(onClick = { revoke = null }) { Text("Abbrechen") } })
}
