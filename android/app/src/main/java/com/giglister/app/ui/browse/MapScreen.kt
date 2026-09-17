package com.giglister.app.ui.browse

import android.annotation.SuppressLint
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.giglister.app.data.api.ApiClient
import com.giglister.app.data.model.*
import com.giglister.app.ui.*
import com.giglister.app.ui.components.*
import kotlinx.serialization.json.*

@Composable fun PlacesScreen(app: GigState, navigate: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageTitle("Vor deiner Tür.", "Entdecke die Bühnen in deiner Umgebung.")
        AreaButton(app)
        if (!app.area.hasCoordinates) {
            LoadContent(app.area, {
                val venues = mutableListOf<LocationListItem>()
                var page = 0
                do {
                    val response = ApiClient.api.locations(app.area.query() + mapOf("page" to page.toString(), "size" to "100"))
                    venues.addAll(response.content); page++
                } while (page < response.totalPages)
                venues.toList()
            }) { venues ->
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                    item { EmptyMessage("Wähle eine Stadt oder deinen Standort, um die Konzertkarte zu öffnen.") }
                    if (venues.isEmpty()) item { EmptyMessage("Keine Orte gefunden.") }
                    items(venues) { EntityRow(it.name, "${it.city} · ${it.upcomingEventCount} Konzerte") { navigate("locations/${it.id}") } }
                }
            }
        } else {
            DateFilters(app)
            val range = dateRange(app.filter)
            val query = app.area.query() + mapOf("from" to range.first.toString(), "to" to range.second.toString())
            var selected by remember(query) { mutableStateOf<Long?>(null) }
            LoadContent(query to app.revision, { allEvents(query) }) { events ->
                val shown = selected?.let { id -> events.filter { it.location.id == id } } ?: events
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                    item { ConcertMap(app.area, events) { selected = it } }
                    if (selected != null) item { TextButton(onClick = { selected = null }) { Text("Alle Orte anzeigen") } }
                    if (events.any { it.location.latitude == null || it.location.longitude == null }) item { Text("Einige Orte haben noch keine Kartenposition. Ihre Konzerte findest du in der Liste.", style = MaterialTheme.typography.bodySmall) }
                    if (shown.isEmpty()) item { EmptyMessage("Hier sind noch keine Konzerte für diesen Zeitraum gelistet.") }
                    concerts(shown, { navigate("event/$it") })
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable fun ConcertMap(area: SearchArea, events: List<EventResponse>, onVenue: (Long) -> Unit) {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onVenue)
    val payload = buildJsonObject {
        put("lat", area.lat); put("lon", area.lon); put("radius", area.radius)
        putJsonArray("venues") {
            events.groupBy { it.location.id }.values.forEach { concerts ->
                val venue = concerts.first().location
                if (venue.latitude != null && venue.longitude != null) add(buildJsonObject {
                    put("id", venue.id); put("name", venue.name); put("lat", venue.latitude); put("lon", venue.longitude); put("count", concerts.size)
                })
            }
        }
    }.toString()
    val currentPayload by rememberUpdatedState(payload)
    var map by remember { mutableStateOf<WebView?>(null) }
    DisposableEffect(Unit) { onDispose { map?.stopLoading(); map?.destroy(); map = null } }
    AndroidView(modifier = Modifier.fillMaxWidth().height(330.dp), factory = { ctx ->
        val assets = WebViewAssetLoader.Builder().addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx)).build()
        WebView(ctx).apply {
            map = this
            // Keep the map within the Compose scroll container on GPU drivers that
            // incorrectly composite WebView's separate hardware layer.
            setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
            settings.javaScriptEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.domStorageEnabled = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.userAgentString = "GigListerAndroid/1.1 (+https://sandbox.fotosvorju.de)"
            setOnTouchListener { view, event ->
                view.parent.requestDisallowInterceptTouchEvent(event.actionMasked != android.view.MotionEvent.ACTION_UP && event.actionMasked != android.view.MotionEvent.ACTION_CANCEL)
                if (event.actionMasked == android.view.MotionEvent.ACTION_UP) view.performClick()
                false
            }
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    assets.shouldInterceptRequest(request.url)?.let { return it }
                    val host = request.url.host.orEmpty()
                    if (request.url.scheme == "https" && (host == "tile.openstreetmap.org" || host.endsWith(".tile.openstreetmap.org"))) return null
                    return WebResourceResponse("text/plain", "UTF-8", java.io.ByteArrayInputStream(ByteArray(0)))
                }
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    if (uri.scheme == "giglister" && uri.host == "venue") uri.lastPathSegment?.toLongOrNull()?.let(callback)
                    else if (uri.scheme == "https" && uri.host == "www.openstreetmap.org") openLink(context, uri.toString())
                    return true
                }
                override fun onPageFinished(view: WebView, url: String) { view.evaluateJavascript("renderMap($currentPayload)", null); view.tag = currentPayload }
            }
            loadUrl("https://appassets.androidplatform.net/assets/map/index.html")
        }
    }, update = { view -> if (view.tag != null && view.tag != payload) { view.evaluateJavascript("renderMap($payload)", null); view.tag = payload } })
}
