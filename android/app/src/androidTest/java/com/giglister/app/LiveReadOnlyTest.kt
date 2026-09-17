package com.giglister.app

import android.app.UiModeManager
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.giglister.app.data.api.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import java.io.File
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

/** Explicit opt-in: -e live true. Uses public GET endpoints only, never a live account. */
class LiveReadOnlyTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var original: GigListerApi
    @Before fun prepare() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("live") == "true")
        val app = ApplicationProvider.getApplicationContext<GigListerApp>()
        runBlocking { app.authRepository.logout() }
        app.getSharedPreferences("discovery", 0).edit().clear().commit()
        original = ApiClient.api
        ApiClient.api = ApiClient.createApi(BuildConfig.API_BASE_URL, readOnly = true)
    }
    @After fun finish() { scenario?.close(); if (::original.isInitialized) ApiClient.api = original }
    private fun awaitText(value: String) = compose.waitUntil(25000) { runCatching { compose.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false) }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val app = ApplicationProvider.getApplicationContext<GigListerApp>()
        val folder = File(app.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        // Native Compose capture waits for its rendered frame. WebView needs the
        // device compositor because its content is not part of that bitmap.
        var hasMap = false
        scenario?.onActivity { hasMap = findMap(it.window.decorView) != null }
        val bitmap = if (hasMap) InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() else compose.onRoot().captureToImage().asAndroidBitmap()
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun publicConcertsAndMapRenderWithLiveData() {
        val app = ApplicationProvider.getApplicationContext<GigListerApp>()
        val first = runBlocking { ApiClient.api.events(mapOf("from" to LocalDate.now().toString(), "size" to "1")).content.first() }
        if (Build.VERSION.SDK_INT >= 31) app.getSystemService(UiModeManager::class.java).setApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        awaitText("Konzerte in\ndeiner Nähe")
        screenshot("update-home-dark")
        awaitText("Konzerte"); compose.onNodeWithText("Konzerte").performClick()
        awaitText(first.displayTitle)
        // Preload the same URLs via Coil so visual verification does not capture in-flight images.
        runBlocking {
            val urls = listOfNotNull(first.titleImageUrl, first.location.titleImageUrl) + first.bands.flatMap { listOfNotNull(it.titleImageUrl, it.logoUrl) }
            urls.forEach { url -> coil.Coil.imageLoader(app).execute(coil.request.ImageRequest.Builder(app).data(url).build()) }
        }
        screenshot("live-home-dark")
        compose.onNodeWithText(first.displayTitle).performClick()
        awaitText(first.displayTitle)
        screenshot("live-event-dark")
        scenario?.close()
        app.getSharedPreferences("discovery", 0).edit().putString("city", "Hamburg").putString("lat", "53.46").putString("lon", "9.99").putInt("radius", 25).commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        awaitText("Orte"); compose.onNodeWithText("Orte").performClick()
        awaitMap()
        screenshot("live-map-dark")
        if (Build.VERSION.SDK_INT >= 31) {
            app.getSystemService(UiModeManager::class.java).setApplicationNightMode(UiModeManager.MODE_NIGHT_NO)
            awaitText("Vor deiner Tür."); awaitMap(); screenshot("live-map-light")
        }
        val festival = runBlocking { ApiClient.api.festivals().firstOrNull() }
        if (festival != null) {
            compose.onNodeWithText("Festivals").performClick()
            awaitText(festival.name); compose.onNodeWithText(festival.name).performClick()
            awaitText("KONZERTE DIESES FESTIVALS"); screenshot("update-festival-light")
            compose.onNode(hasScrollToIndexAction()).performTouchInput { swipeUp() }
            screenshot("update-timetable-light")
        }
    }
    private fun awaitMap() {
        val tiles = AtomicInteger(0)
        compose.waitUntil(30000) {
            scenario?.onActivity { activity -> findMap(activity.window.decorView)?.evaluateJavascript("document.getElementById('map').getBoundingClientRect().height > 100 ? document.querySelectorAll('.leaflet-tile-loaded').length : 0") { value -> tiles.set(value.toIntOrNull() ?: 0) } }
            tiles.get() > 0
        }
        val drawn = java.util.concurrent.atomic.AtomicBoolean(false)
        scenario?.onActivity { activity -> findMap(activity.window.decorView)?.postVisualStateCallback(1L, object : WebView.VisualStateCallback() { override fun onComplete(requestId: Long) { drawn.set(true) } }) }
        compose.waitUntil(5000) { drawn.get() }
    }
    private fun findMap(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) findMap(view.getChildAt(index))?.let { return it }
        return null
    }
}

