@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.giglister.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.giglister.app.data.model.EventResponse
import com.giglister.app.ui.GigState
import com.giglister.app.util.errorMessage
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun dateLabel(date: String, pattern: String = "EEE, d. MMMM"): String = runCatching {
    LocalDate.parse(date).format(DateTimeFormatter.ofPattern(pattern, Locale.GERMAN))
}.getOrDefault(date)

@Composable fun PageTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun SectionTitle(text: String) { Text(text.uppercase(Locale.GERMAN), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) }
@Composable fun EmptyMessage(text: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f), modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun ErrorPanel(message: String, retry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(message, color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = retry) { Text("Erneut versuchen") }
    }
}
@Composable fun <T> LoadContent(key: Any?, load: suspend () -> T, content: @Composable (T) -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    var value by remember(key) { mutableStateOf<T?>(null) }
    var error by remember(key) { mutableStateOf<String?>(null) }
    LaunchedEffect(key, attempt) {
        error = null
        try { value = load() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = errorMessage(e) }
    }
    when {
        error != null -> ErrorPanel(error!!) { attempt++ }
        value != null -> content(value!!)
        else -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}

class ActionRunner(private val scope: CoroutineScope, private val app: GigState) {
    var busy by mutableStateOf(false); private set
    fun run(block: suspend () -> Unit) {
        if (busy) return
        // Navigation and UI state changes must resume on Android's main thread,
        // including when a recreated composition supplies a different dispatcher.
        scope.launch(Dispatchers.Main.immediate) {
            busy = true
            try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) { app.handleFailure(e) } finally { busy = false }
        }
    }
}
@Composable fun rememberAction(app: GigState): ActionRunner {
    val scope = rememberCoroutineScope()
    return remember(scope, app) { ActionRunner(scope, app) }
}

private val bandColors = listOf(Color(0xFF756632), Color(0xFF35555A), Color(0xFF694456), Color(0xFF485645))
@Composable fun ConcertArtwork(event: EventResponse, modifier: Modifier = Modifier, labels: Boolean = true) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (!event.titleImageUrl.isNullOrBlank()) {
            AsyncImage(event.titleImageUrl, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        } else {
            val bands = event.bands.take(4).sortedBy { if (event.bandImageDisplay == "PHOTO" && it.titleImageUrl != null) 0 else 1 }
            if (bands.isEmpty()) Text("LIVE", Modifier.align(Alignment.Center), style = MaterialTheme.typography.displayMedium)
            BoxWithConstraints(Modifier.matchParentSize()) {
                val segmentWidth = maxWidth / bands.size.coerceAtLeast(1)
                val skew = maxWidth * .04f
                val skewPixels = with(androidx.compose.ui.platform.LocalDensity.current) { skew.toPx() }
                bands.forEachIndexed { index, band ->
                    val shape = remember(index, bands.size, skewPixels) { GenericShape { size, _ ->
                        moveTo(if (index == 0) 0f else skewPixels, 0f)
                        lineTo(size.width, 0f)
                        lineTo(if (index == bands.lastIndex) size.width else size.width - skewPixels, size.height)
                        lineTo(0f, size.height); close()
                    } }
                    Box(Modifier.offset(x = segmentWidth * index).width(segmentWidth + if (index == bands.lastIndex) 0.dp else skew).fillMaxHeight().clip(shape).background(bandColors[index % bandColors.size]).drawWithContent {
                        drawContent()
                        if (index > 0) drawLine(Color.White, Offset(skewPixels, 0f), Offset(0f, size.height), strokeWidth = 1.5.dp.toPx())
                    }) {
                        val photo = if (event.bandImageDisplay == "PHOTO") band.titleImageUrl else null
                        AsyncImage(photo ?: event.location.titleImageUrl, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop, alpha = if (photo != null) 1f else .25f)
                        if (photo == null && band.logoUrl != null) AsyncImage(band.logoUrl, null, Modifier.align(Alignment.Center).fillMaxWidth().padding(10.dp), contentScale = ContentScale.Fit)
                        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .8f)))))
                        if (labels && bands.size <= 3) Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                            Text(band.name.uppercase(Locale.GERMAN), color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 2)
                            band.genres.firstOrNull()?.let { Text(it, color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                        }
                    }
                }
            }
        }
    }
}
@Composable fun ConcertCard(event: EventResponse, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Box {
            ConcertArtwork(event, Modifier.fillMaxWidth().height(180.dp))
            Surface(Modifier.padding(12.dp), shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.background.copy(alpha = .95f)) {
                Text(dateLabel(event.date, "EE, d. MMM").uppercase(Locale.GERMAN) + (event.startTime?.take(5)?.let { " · $it" } ?: ""), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (event.status == "CANCELLED") Text("ABGESAGT", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
            Text(event.listTitle, style = MaterialTheme.typography.titleLarge)
            if (!event.title.isNullOrBlank() && event.bands.isNotEmpty()) Text(event.bands.joinToString(" · ") { it.name }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${event.location.name} · ${event.location.city}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
fun LazyListScope.concerts(events: List<EventResponse>, open: (Long) -> Unit, prefix: String = "") {
    events.groupBy { it.date }.toSortedMap().forEach { (date, group) ->
        item("${prefix}day$date") { SectionTitle(dateLabel(date, "EEEE, d. MMMM")) }
        items(group, key = { "$prefix${it.id}" }) { event -> ConcertCard(event) { open(event.id) } }
    }
}
@Composable fun EntityRow(name: String, detail: String?, image: String? = null, onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge)
                if (image != null) AsyncImage(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                if (!detail.isNullOrBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
fun openLink(context: Context, url: String?) {
    if (url.isNullOrBlank()) return
    val uri = Uri.parse(url)
    if (uri.scheme !in listOf("https", "http", "geo")) return
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }.onFailure { Toast.makeText(context, "Kein passendes Programm gefunden.", Toast.LENGTH_SHORT).show() }
}
