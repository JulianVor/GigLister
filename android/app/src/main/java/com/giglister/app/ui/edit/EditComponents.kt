package com.giglister.app.ui.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@Composable fun FormField(label: String, value: String, onChange: (String) -> Unit, multiline: Boolean = false, enabled: Boolean = true) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = !multiline, minLines = if (multiline) 3 else 1, enabled = enabled)
}

fun String.optional(): String? = trim().ifEmpty { null }
fun validateWebUrl(value: String?) {
    if (value.isNullOrBlank()) return
    val uri = java.net.URI(value)
    require(uri.scheme in listOf("https", "http") && !uri.host.isNullOrBlank()) { "Bitte einen vollständigen Link mit https:// angeben." }
}

@Composable fun ImageField(label: String, value: String?, app: GigState, onBusy: (Boolean) -> Unit, onChange: (String?) -> Unit) {
    val context = LocalContext.current
    val action = rememberAction(app)
    fun upload(uri: Uri) {
        action.run {
            onBusy(true)
            try {
                val part = withContext(Dispatchers.IO) {
                    val mime = context.contentResolver.getType(uri)
                    require(mime in listOf("image/jpeg", "image/png", "image/webp", "image/gif")) { "Bitte JPG, PNG, WebP oder GIF wählen." }
                    val data = context.contentResolver.openInputStream(uri)?.use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            require(output.size() + count <= 5 * 1024 * 1024) { "Das Bild darf höchstens 5 MB groß sein." }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    } ?: error("Bild konnte nicht gelesen werden.")
                    MultipartBody.Part.createFormData("file", "bild.${mime!!.substringAfter('/')}", data.toRequestBody(mime.toMediaType()))
                }
                onChange(ApiClient.api.upload(part).url)
            } finally { onBusy(false) }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> uri?.let { upload(it) } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        if (!value.isNullOrBlank()) AsyncImage(value, label, Modifier.fillMaxWidth().height(160.dp), contentScale = ContentScale.Crop)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = !action.busy, onClick = { picker.launch("image/*") }) { Text(if (action.busy) "Bild wird geladen …" else "Bild auswählen") }
            if (value != null) TextButton(enabled = !action.busy, onClick = { onChange(null) }) { Text("Entfernen") }
        }
        TextButton(enabled = !action.busy, onClick = {
            val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
            val clip = clipboard.primaryClip
            val uri = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).uri else null
            if (uri?.scheme == "content") upload(uri)
            else app.notice = "Kein kopiertes Bild verfügbar. Wähle ein Bild aus deinen Dateien."
        }) { Text("Bild aus Zwischenablage") }
        Text("JPG, PNG, WebP oder GIF · bis 5 MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun EntitySelector(label: String, kind: String, value: EntityRef?, app: GigState, onChange: (EntityRef) -> Unit) {
    var show by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) { Text(value?.name ?: "$label auswählen oder hinzufügen") }
    if (show) {
        var name by rememberSaveable { mutableStateOf("") }
        var city by rememberSaveable { mutableStateOf(app.area.city) }
        var address by rememberSaveable { mutableStateOf("") }
        var postal by rememberSaveable { mutableStateOf("") }
        var candidates by remember { mutableStateOf<List<DuplicateCandidate>>(emptyList()) }
        var loading by remember { mutableStateOf(false) }
        var checked by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        val venue = kind == "locations"
        LaunchedEffect(name, city) {
            checked = false; candidates = emptyList(); error = null
            if (name.isBlank()) return@LaunchedEffect
            loading = true
            try { delay(350); candidates = ApiClient.api.duplicates(kind, name.trim(), city.optional()); checked = true }
            catch (e: CancellationException) { throw e } catch (e: Exception) { error = "Vorschläge konnten nicht geladen werden. Bitte Eingabe erneut ändern." }
            finally { loading = false }
        }
        AlertDialog(onDismissRequest = { show = false }, title = { Text(label) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FormField("Name", name, { name = it })
                FormField(if (venue) "Stadt *" else "Stadt (optional)", city, { city = it })
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (candidates.isNotEmpty()) Text("Meintest du eines dieser Profile?", style = MaterialTheme.typography.titleMedium)
                candidates.forEach { candidate -> TextButton(onClick = { onChange(EntityRef(candidate.id, candidate.name, candidate.city)); show = false }) { Text("${candidate.name} · ${candidate.city.orEmpty()}") } }
                if (venue) {
                    Text("Nur für einen neuen Ort:", style = MaterialTheme.typography.labelLarge)
                    FormField("Straße und Hausnummer *", address, { address = it })
                    FormField("Postleitzahl *", postal, { postal = it })
                }
                Text("Ein neues Profil wird mit diesem Konzert angelegt. Vorhandene Profile kannst du oben auswählen.", style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = { TextButton(enabled = checked && !loading && name.isNotBlank() && (!venue || (city.isNotBlank() && address.isNotBlank() && postal.isNotBlank())), onClick = {
            onChange(EntityRef(name = name.trim(), city = city.optional(), address = address.optional(), postalCode = postal.optional())); show = false
        }) { Text("Neu hinzufügen") } }, dismissButton = { TextButton(onClick = { show = false }) { Text("Abbrechen") } })
    }
}
