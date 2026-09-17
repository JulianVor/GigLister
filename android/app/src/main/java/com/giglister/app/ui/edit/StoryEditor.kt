@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.giglister.app.ui.edit

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.giglister.app.data.api.ApiClient
import com.giglister.app.data.model.*
import com.giglister.app.ui.GigState
import com.giglister.app.ui.browse.StoryCanvas
import com.giglister.app.ui.components.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlin.math.*

@Serializable private data class StoryDraft(val photo: StoryInput = StoryInput(), val layers: List<StoryLayer> = emptyList(), val aspect: Float? = null)
private val storySaver = Saver<StoryDraft, String>(save = { storyJson.encodeToString(it) }, restore = { storyJson.decodeFromString<StoryDraft>(it) })

@Composable fun StoryEditorScreen(id: Long, app: GigState, done: () -> Unit) {
    LoadContent(id, { app.reloadMe(); ApiClient.api.entity("bands", id) }) { band ->
        if (!app.me.canEdit("BAND", id)) EmptyMessage("Für Stories dieser Band brauchst du Bearbeitungsrechte.")
        else StoryEditor(band, app, done)
    }
}

@Composable private fun StoryEditor(band: EntityDetails, app: GigState, done: () -> Unit) {
    var draft by rememberSaveable(band.id, stateSaver = storySaver) { mutableStateOf(StoryDraft()) }
    var selected by rememberSaveable { mutableStateOf("photo") }
    var uploading by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var bandDialog by remember { mutableStateOf(false) }
    var posted by rememberSaveable { mutableStateOf(false) }
    val action = rememberAction(app)
    val context = LocalContext.current
    LaunchedEffect(draft.photo.imageUrl, retry) {
        imageError = false
        if (draft.photo.imageUrl.isBlank() || draft.aspect != null) return@LaunchedEffect
        try {
            val result = context.imageLoader.execute(ImageRequest.Builder(context).data(draft.photo.imageUrl).size(768).allowHardware(false).build()) as? SuccessResult ?: error("Bild nicht geladen")
            val drawable = result.drawable
            val aspect = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.coerceAtLeast(1)
            val color = withContext(Dispatchers.Default) {
                val bitmap = drawable.toBitmap(48, 48)
                val buckets = mutableMapOf<Int, LongArray>()
                for (y in 0 until 48) for (x in 0 until 48) {
                    val pixel = bitmap.getPixel(x, y)
                    if (android.graphics.Color.alpha(pixel) < 128) continue
                    val r = android.graphics.Color.red(pixel); val g = android.graphics.Color.green(pixel); val b = android.graphics.Color.blue(pixel)
                    val bucket = buckets.getOrPut((r / 24) * 121 + (g / 24) * 11 + b / 24) { LongArray(4) }
                    bucket[0]++; bucket[1] += r; bucket[2] += g; bucket[3] += b
                }
                buckets.values.maxByOrNull { it[0] }?.let { "#%02x%02x%02x".format(it[1] / it[0], it[2] / it[0], it[3] / it[0]) } ?: "#111111"
            }
            val fit = storyFit(aspect)
            draft = draft.copy(aspect = aspect, photo = draft.photo.copy(imgWidthPct = fit.first, imgHeightPct = fit.second, imgBackgroundColor = color))
        } catch (e: CancellationException) { throw e } catch (_: Exception) { imageError = true }
    }
    fun updateLayer(block: (StoryLayer) -> StoryLayer) { draft = draft.copy(layers = draft.layers.map { if (it.id == selected) block(it) else it }) }
    fun scaleTo(value: Float) {
        if (selected == "photo") { val fit = storyFit(draft.aspect ?: (9f / 16f)); draft = draft.copy(photo = draft.photo.copy(imgWidthPct = fit.first * value, imgHeightPct = fit.second * value)) }
        else updateLayer { it.copy(scale = value) }
    }
    fun rotateTo(value: Float) { if (selected == "photo") draft = draft.copy(photo = draft.photo.copy(imgRotationDeg = value)) else updateLayer { it.copy(rotationDeg = value) } }
    val latestDraft by rememberUpdatedState(draft)
    val latestSelected by rememberUpdatedState(selected)
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.imePadding()) {
        item { PageTitle("Euer Moment.\nEure Story.", "${band.name} · für 24 Stunden sichtbar") }
        item { ImageField("Story-Foto", draft.photo.imageUrl.ifBlank { null }, app, { uploading = it }) { url ->
            draft = draft.copy(photo = StoryInput(imageUrl = url.orEmpty()), aspect = null); selected = "photo"; posted = false
        } }
        if (draft.photo.imageUrl.isNotBlank()) {
            if (imageError) item { Text("Das Bild konnte nicht geladen werden."); TextButton(onClick = { retry++ }) { Text("Erneut laden") } }
            else if (draft.aspect == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    StoryCanvas(draft.photo, draft.layers, Modifier.height(360.dp).aspectRatio(9f / 16f).testTag("story-editor-preview")
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, rotation ->
                                val d = latestDraft; val photo = latestSelected == "photo"
                                val layer = d.layers.find { it.id == latestSelected }
                                if (photo && d.aspect == null) return@detectTransformGestures
                                val oldScale = if (photo) d.photo.imgWidthPct / storyFit(d.aspect!!).first else layer?.scale ?: return@detectTransformGestures
                                val newScale = (oldScale * zoom).coerceIn(if (photo) 1f else .4f, if (photo) 5f else 4f)
                                val ratio = newScale / oldScale
                                val x = (if (photo) d.photo.imgCenterXPct else layer!!.centerXPct) * size.width / 100
                                val y = (if (photo) d.photo.imgCenterYPct else layer!!.centerYPct) * size.height / 100
                                val radians = Math.toRadians(rotation.toDouble()); val dx = x - centroid.x; val dy = y - centroid.y
                                val nx = ((centroid.x + pan.x + (dx * cos(radians) - dy * sin(radians)) * ratio) * 100 / size.width).toFloat().coerceIn(-50f, 150f)
                                val ny = ((centroid.y + pan.y + (dx * sin(radians) + dy * cos(radians)) * ratio) * 100 / size.height).toFloat().coerceIn(-50f, 150f)
                                if (photo) draft = d.copy(photo = d.photo.copy(imgCenterXPct = nx, imgCenterYPct = ny, imgWidthPct = d.photo.imgWidthPct * ratio, imgHeightPct = d.photo.imgHeightPct * ratio, imgRotationDeg = d.photo.imgRotationDeg + rotation))
                                else draft = d.copy(layers = d.layers.map { if (it.id == latestSelected) it.copy(centerXPct = nx, centerYPct = ny, scale = newScale, rotationDeg = it.rotationDeg + rotation) else it })
                            }
                        }, selected = selected) { selected = it.id }
                }
            }
            item { Text("Ebene auswählen, dann verschieben oder mit zwei Fingern zoomen und drehen.", style = MaterialTheme.typography.bodySmall) }
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected == "photo", { selected = "photo" }, label = { Text("Foto") })
                draft.layers.forEachIndexed { index, layer -> FilterChip(selected == layer.id, { selected = layer.id }, label = { Text(layer.bandName ?: "Text ${index + 1}") }) }
                OutlinedButton(onClick = { val layer = StoryLayer(); draft = draft.copy(layers = draft.layers + layer); selected = layer.id }) { Text("+ Text") }
                OutlinedButton(onClick = { bandDialog = true }) { Text("+ Band") }
            } }
            val layer = draft.layers.find { it.id == selected }
            if (layer != null && layer.bandId == null) item { FormField("Story-Text", layer.text, { value -> updateLayer { it.copy(text = value) } }, multiline = true) }
            item {
                val photo = selected == "photo"
                val scale = if (photo) draft.photo.imgWidthPct / storyFit(draft.aspect ?: (9f / 16f)).first else layer?.scale ?: 1f
                Text("Zoom"); Slider(scale.coerceIn(if (photo) 1f else .4f, if (photo) 5f else 4f), { scaleTo(it) }, valueRange = if (photo) 1f..5f else .4f..4f)
                Text("Drehen"); Slider(((if (photo) draft.photo.imgRotationDeg else layer?.rotationDeg ?: 0f) + 180).mod(360f) - 180, { rotateTo(it) }, valueRange = -180f..180f)
                if (layer != null) {
                    Text("Farbe")
                    Box(Modifier.fillMaxWidth().height(6.dp).background(Brush.horizontalGradient((0..7).map { Color(storyColor(it * 100f / 7)) })))
                    Slider(layer.colorPos ?: 0f, { value -> updateLayer { it.copy(colorPos = value) } }, valueRange = 0f..100f)
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(layer.hasBox, { value -> updateLayer { it.copy(hasBox = value) } }); Text("Kasten hinter der Ebene") }
                    TextButton(onClick = { draft = draft.copy(layers = draft.layers.filter { it.id != selected }); selected = "photo" }) { Text("Ebene entfernen") }
                }
                TextButton(onClick = {
                    if (photo) { val fit = storyFit(draft.aspect ?: (9f / 16f)); draft = draft.copy(photo = draft.photo.copy(imgWidthPct = fit.first, imgHeightPct = fit.second, imgCenterXPct = 50f, imgCenterYPct = 50f, imgRotationDeg = 0f)) }
                    else updateLayer { it.copy(centerXPct = 50f, centerYPct = 50f, scale = 1f, rotationDeg = 0f) }
                }) { Text("Position zurücksetzen") }
            }
        }
        item { Button(enabled = draft.aspect != null && draft.photo.imageUrl.isNotBlank() && !uploading && !action.busy && !posted, modifier = Modifier.fillMaxWidth(), onClick = { action.run {
            ApiClient.api.createStory(band.id, draft.photo.withLayers(draft.layers)); posted = true
            app.changed(); app.notice = "Story veröffentlicht – für 24 Stunden sichtbar."; done()
        } }) { Text(if (action.busy) "Wird veröffentlicht …" else if (posted) "Story veröffentlicht" else "Story veröffentlichen") } }
        if (posted) item { TextButton(onClick = done) { Text("Zum Bandprofil") } }
    }
    if (bandDialog) StoryBandPicker(band.id, { bandDialog = false }) { option ->
        val layer = StoryLayer(bandId = option.id, bandName = option.name, profileImageUrl = option.profileImageUrl, logoUrl = option.logoUrl)
        draft = draft.copy(layers = draft.layers + layer); selected = layer.id; bandDialog = false
    }
}

@Composable private fun StoryBandPicker(ownId: Long, dismiss: () -> Unit, choose: (StoryBandOption) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text("Band markieren") }, text = {
        Column {
            FormField("Band suchen", query, { query = it })
            if (query.trim().isNotEmpty()) LoadContent(query, { delay(300); ApiClient.api.storyBands(query.trim()).filter { it.id != ownId } }) { bands ->
                Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                    if (bands.isEmpty()) Text("Keine passende Band gefunden.")
                    bands.forEach { band -> TextButton(onClick = { choose(band) }) { Text(band.name) } }
                }
            }
        }
    }, confirmButton = {}, dismissButton = { TextButton(onClick = dismiss) { Text("Abbrechen") } })
}


