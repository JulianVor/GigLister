@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.giglister.app.ui.browse

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.giglister.app.data.api.ApiClient
import com.giglister.app.data.model.*
import com.giglister.app.ui.GigState
import com.giglister.app.ui.components.*
import com.giglister.app.util.errorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private val ring = Brush.sweepGradient(listOf(Color(0xffe8481c), Color(0xffc2277a), Color(0xff7a3aa8), Color(0xff2f6fae), Color(0xff2f9d6c), Color(0xffd9a91c), Color(0xffe8481c)))

@Composable fun StoryAvatar(name: String, photo: String?, logo: String?, active: Boolean, size: Dp = 64.dp, onClick: () -> Unit) {
    Box(Modifier.size(size).then(if (active) Modifier.border(BorderStroke(3.dp, ring)) else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant)).padding(6.dp).clickable(onClick = onClick).semantics { contentDescription = if (active) "Story von $name öffnen" else "Profil von $name öffnen" }, contentAlignment = Alignment.Center) {
        if (photo != null || logo != null) AsyncImage(photo ?: logo, null, Modifier.fillMaxSize(), contentScale = if (photo != null) ContentScale.Crop else ContentScale.Fit)
        else Text(name.take(1), style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable fun BandStories(band: EntityDetails, app: GigState, navigate: (String) -> Unit) {
    val canEdit = app.me.canEdit("BAND", band.id)
    Column {
        LoadContent(band.id to app.revision, { ApiClient.api.stories(band.id) }) { stories ->
            val active = stories.any { it.active() }
            Row(verticalAlignment = Alignment.CenterVertically) {
                StoryAvatar(band.name, band.profileImageUrl, band.logoUrl, active, 80.dp) { if (active) navigate("bands/${band.id}/stories") }
                if (active) TextButton(onClick = { navigate("bands/${band.id}/stories") }) { Text("Story ansehen") }
                else Text("Keine aktuelle Story", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (canEdit) OutlinedButton(onClick = { navigate("bands/${band.id}/stories/new") }) { Text("Story erstellen") }
    }
}

/** Percentage placement is independent of screen density and font scaling, just like cqw on web. */
@Composable private fun StoryPosition(x: Float, y: Float, rotation: Float, content: @Composable () -> Unit) {
    Layout(content, Modifier.fillMaxSize()) { measurables, constraints ->
        val child = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0, maxWidth = (constraints.maxWidth * .9f).toInt()))
        layout(constraints.maxWidth, constraints.maxHeight) {
            child.placeWithLayer((constraints.maxWidth * x / 100 - child.width / 2).toInt(), (constraints.maxHeight * y / 100 - child.height / 2).toInt()) { rotationZ = rotation }
        }
    }
}

@Composable fun StoryCanvas(draft: StoryInput, layers: List<StoryLayer>, modifier: Modifier = Modifier, selected: String? = null, layerClick: (StoryLayer) -> Unit = {}) {
    val background = runCatching { Color(android.graphics.Color.parseColor(draft.imgBackgroundColor)) }.getOrDefault(Color(0xff111111))
    BoxWithConstraints(modifier.clipToBounds().background(background)) {
        val width = maxWidth; val height = maxHeight
        AsyncImage(draft.imageUrl, "Story-Foto", Modifier.align(Alignment.Center)
            .requiredSize(width * (draft.imgWidthPct / 100), height * (draft.imgHeightPct / 100))
            .graphicsLayer { translationX = width.toPx() * (draft.imgCenterXPct - 50) / 100; translationY = height.toPx() * (draft.imgCenterYPct - 50) / 100; rotationZ = draft.imgRotationDeg }, contentScale = ContentScale.FillBounds)
        layers.forEach { layer ->
            val fontDp = width * ((if (layer.bandId == null) .07f else .05f) * layer.scale.coerceIn(.1f, 10f))
            val font = with(LocalDensity.current) { fontDp.toSp() }
            StoryPosition(layer.centerXPct, layer.centerYPct, layer.rotationDeg) {
                Box(Modifier.then(if (selected == layer.id) Modifier.border(1.dp, Color.White) else Modifier)
                    .clickable { layerClick(layer) }.padding(horizontal = fontDp * .6f, vertical = fontDp * .5f), contentAlignment = Alignment.Center) {
                    if (layer.hasBox) {
                        Box(Modifier.matchParentSize().graphicsLayer { rotationZ = -2f }.background(Color.Black.copy(alpha = .85f)))
                        Box(Modifier.matchParentSize().graphicsLayer { rotationZ = 1.5f; scaleX = 1.04f; scaleY = .96f }.background(Color.Black.copy(alpha = .85f)))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (layer.bandId != null) {
                            if (layer.profileImageUrl != null || layer.logoUrl != null) AsyncImage(layer.profileImageUrl ?: layer.logoUrl, null, Modifier.size(fontDp * 1.8f).border(1.dp, Color.White), contentScale = if (layer.profileImageUrl != null) ContentScale.Crop else ContentScale.Fit)
                            else Text(layer.bandName.orEmpty().take(1), color = Color.White, fontSize = font)
                        }
                        Text(layer.bandName ?: layer.text, color = Color(storyColor(layer.colorPos)), fontSize = if (layer.bandId != null) font * .85f else font,
                            style = MaterialTheme.typography.titleLarge.copy(shadow = Shadow(Color.Black.copy(alpha = .6f), blurRadius = 6f)), lineHeight = font * 1.25f, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable fun StoryViewerScreen(id: Long, app: GigState, navigate: (String) -> Unit, close: () -> Unit) {
    LoadContent(id, { ApiClient.api.entity("bands", id) to ApiClient.api.stories(id).filter { it.active() } }) { (band, initial) ->
        var stories by remember { mutableStateOf(initial) }
        var index by remember { mutableIntStateOf(0) }
        var elapsed by remember { mutableIntStateOf(0) }
        var held by remember { mutableStateOf(false) }
        var paused by remember { mutableStateOf(false) }
        var confirm by remember { mutableStateOf(false) }
        var deleteError by remember { mutableStateOf<String?>(null) }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        var resumed by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
        DisposableEffect(lifecycle) {
            val observer = LifecycleEventObserver { _, _ -> resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
            lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
        }
        val action = rememberAction(app)
        val story = stories.getOrNull(index)
        fun next() { if (index < stories.lastIndex) { index++; elapsed = 0 } else close() }
        LaunchedEffect(story?.id) {
            elapsed = 0
            while (story != null) {
                delay(50)
                if (!story.active()) { next(); break }
                if (resumed && !held && !paused && !confirm && !action.busy) elapsed += 50
                if (elapsed >= 6000) { next(); break }
            }
        }
        if (story == null) Column(Modifier.padding(20.dp)) { EmptyMessage("Keine aktuelle Story. Stories verschwinden nach 24 Stunden."); TextButton(onClick = close) { Text("Zurück zum Profil") } }
        else Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding(), contentAlignment = Alignment.Center) {
                val w = minOf(maxWidth, maxHeight * (9f / 16f))
                val fitFallback = story.imgWidthPct == null || story.imgHeightPct == null
                var fallbackAspect by remember(story.id) { mutableFloatStateOf(9f / 16f) }
                val fit = storyFit(fallbackAspect)
                val draft = StoryInput(story.imageUrl, story.text, story.imgWidthPct ?: fit.first, story.imgHeightPct ?: fit.second, story.imgCenterXPct ?: 50f, story.imgCenterYPct ?: 50f, story.imgRotationDeg ?: 0f, story.imgBackgroundColor ?: "#111111")
                Box(Modifier.width(w).aspectRatio(9f / 16f)) {
                    // Capture hold/tap beneath clickable tags and header controls.
                    Box(Modifier.fillMaxSize().pointerInput(story.id) { detectTapGestures(onPress = { held = true; try { tryAwaitRelease() } finally { held = false } }, onTap = { if (it.x < size.width / 3) { index = (index - 1).coerceAtLeast(0); elapsed = 0 } else next() }) }) {
                        StoryCanvas(draft, parseStoryLayers(story.textLayersJson) + parseStoryLayers(story.bandTagsJson, true), Modifier.fillMaxSize()) { layer ->
                            layer.bandId?.let { close(); navigate("bands/$it") }
                        }
                        if (fitFallback) AsyncImage(story.imageUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit, onSuccess = { fallbackAspect = it.result.drawable.intrinsicWidth.toFloat() / it.result.drawable.intrinsicHeight.coerceAtLeast(1) })
                    }
                    Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .65f), Color.Transparent))).padding(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { stories.forEachIndexed { i, _ -> LinearProgressIndicator(progress = { if (i < index) 1f else if (i == index) elapsed / 6000f else 0f }, modifier = Modifier.weight(1f).height(3.dp), color = Color.White, trackColor = Color.White.copy(alpha = .3f)) } }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { close(); navigate("bands/$id") }, modifier = Modifier.weight(1f)) { AsyncImage(band.profileImageUrl ?: band.logoUrl, null, Modifier.size(28.dp), contentScale = if (band.profileImageUrl != null) ContentScale.Crop else ContentScale.Fit); Spacer(Modifier.width(8.dp)); Text(band.name, color = Color.White) }
                            TextButton(onClick = close) { Text("Schließen", color = Color.White) }
                        }
                    }
                    Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = .5f)), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = { index = (index - 1).coerceAtLeast(0); elapsed = 0 }) { Text("Vorherige", color = Color.White) }
                        TextButton(onClick = { paused = !paused }) { Text(if (paused) "Weiter" else "Pause", color = Color.White) }
                        TextButton(onClick = { next() }) { Text("Nächste", color = Color.White) }
                        if (app.me.canEdit("BAND", id)) TextButton(onClick = { confirm = true }) { Text("Löschen", color = Color.White) }
                    }
                }
            }
        }
        if (confirm && story != null) AlertDialog(onDismissRequest = { if (!action.busy) confirm = false }, title = { Text("Story löschen?") }, text = { Column { Text("Diese Story wird für alle Besucher entfernt."); deleteError?.let { Text(it, color = MaterialTheme.colorScheme.error) } } }, confirmButton = {
            TextButton(enabled = !action.busy, onClick = { action.run {
                deleteError = null
                try { ApiClient.api.deleteStory(id, story.id) }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { deleteError = errorMessage(e); return@run }
                stories = stories.filter { it.id != story.id }; index = index.coerceAtMost((stories.size - 1).coerceAtLeast(0)); elapsed = 0; confirm = false
                app.changed()
            } }) { Text("Endgültig löschen") }
        }, dismissButton = { TextButton(enabled = !action.busy, onClick = { confirm = false }) { Text("Abbrechen") } })
    }
}


