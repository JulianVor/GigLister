package com.giglister.app

import android.content.*
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.giglister.app.data.api.*
import com.giglister.app.data.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.*
import okio.Buffer
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(AndroidJUnit4::class)
class StoriesFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var server: MockWebServer
    private lateinit var original: GigListerApi
    private var scenario: ActivityScenario<MainActivity>? = null
    private val app get() = ApplicationProvider.getApplicationContext<GigListerApp>()
    private val requests = CopyOnWriteArrayList<String>()
    private var permission = "EDIT"
    private var active = true
    private var deleteFails = false
    private var postBody: String? = null
    private var uploadUri: Uri? = null
    private val png by lazy {
        val bitmap = Bitmap.createBitmap(360, 640, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { shader = LinearGradient(0f,0f,360f,640f,intArrayOf(Color.rgb(35,56,65),Color.rgb(140,62,80),Color.rgb(225,158,60)),null,Shader.TileMode.CLAMP) }
        canvas.drawRect(0f,0f,360f,640f,paint)
        paint.shader = null; paint.color = Color.WHITE; paint.textSize = 34f; paint.isAntiAlias = true
        canvas.drawText("GIGLISTER", 55f, 190f, paint)
        ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }.toByteArray()
    }
    private val band get() = """{"id":62,"name":"Hey Nille","profileImageUrl":"${server.url("photo.png")}","status":"PUBLISHED","upcomingEvents":[]}"""
    private val story get() = BandStory(1, server.url("photo.png").toString(), imgWidthPct = 100f, imgHeightPct = 100f, imgCenterXPct = 50f, imgCenterYPct = 50f, imgRotationDeg = -8f, imgBackgroundColor = "#34404a", textLayersJson = storyJson.encodeToString(listOf(StoryLayer(id="text", text="Heute live!", centerYPct=58f, hasBox=true, colorPos=20f))), bandTagsJson = storyJson.encodeToString(listOf(StoryLayer(id="tag", bandId=63,bandName="Thrashkat",centerYPct=75f,hasBox=true))), createdAt=Instant.now().minusSeconds(30).toString(), expiresAt=Instant.now().plusSeconds(86000).toString())
    @Before fun setup() {
        runBlocking { app.authRepository.logout() }; app.getSharedPreferences("discovery",0).edit().clear().commit()
        original = ApiClient.api
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl!!.encodedPath
                requests.add("${request.method} $path")
                if (path == "/photo.png") return MockResponse().setHeader("Content-Type","image/png").setBody(Buffer().write(png))
                if (deleteFails && request.method == "DELETE") return MockResponse().setResponseCode(409).setHeader("Content-Type","application/json").setBody("""{"message":"Story konnte nicht gelöscht werden."}""")
                val body = when(path) {
                    "/api/me" -> """{"id":4,"email":"test@example.org","username":"tester","preferredGenres":["Rock"],"followedBands":[{"id":62,"name":"Hey Nille","profileImageUrl":"${server.url("photo.png")}","hasActiveStory":$active}],"managedEntities":[{"entityType":"BAND","entityId":62,"name":"Hey Nille","permission":"$permission"}]}"""
                    "/api/discover" -> "{}"
                    "/api/bands/62" -> band
                    "/api/bands/63" -> """{"id":63,"name":"Thrashkat","status":"PUBLISHED"}"""
                    "/api/bands/63/stories" -> "[]"
                    "/api/bands/62/stories" -> if (request.method == "POST") { postBody = request.body.readUtf8(); active = true; storyJson.encodeToString(story) } else if (active) storyJson.encodeToString(listOf(story, story.copy(id=2,textLayersJson=storyJson.encodeToString(listOf(StoryLayer(text="Zweiter Moment")))))) else "[]"
                    "/api/bands/62/stories/1", "/api/bands/62/stories/2" -> { active = false; return MockResponse().setResponseCode(204) }
                    "/api/bands/search" -> """[{"id":63,"name":"Thrashkat","profileImageUrl":"${server.url("photo.png")}"}]"""
                    "/api/uploads" -> """{"url":"${server.url("photo.png")}"}"""
                    else -> return MockResponse().setResponseCode(404)
                }
                return MockResponse().setHeader("Content-Type","application/json").setBody(body)
            }
        }
        server.start(); ApiClient.api = ApiClient.createApi(server.url("/").toString())
    }
    @After fun cleanup() {
        scenario?.close(); uploadUri?.let { app.contentResolver.delete(it,null,null) }
        ApiClient.api = original; runBlocking { app.authRepository.logout() }; server.shutdown()
        assertFalse(requests.any { it.contains("/api/admin") })
    }
    private fun launch(login: Boolean = false, home: Boolean = false) {
        if(login) runBlocking { app.authRepository.saveToken("test-token") }
        val intent = Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if(!home) { intent.action = Intent.ACTION_VIEW; intent.data = Uri.parse("https://sandbox.fotosvorju.de/bands/62") }
        scenario = ActivityScenario.launch(intent)
        waitText(if(home) "GEFOLGTE BANDS" else "Story ansehen")
    }
    private fun waitText(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    private fun tap(text: String) { waitText(text); compose.onNodeWithText(text).performClick() }
    private fun scroll(text: String) { compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text)); tap(text) }
    private fun screenshot(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val file = File(app.getExternalFilesDir(null), "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    @Test fun guestViewsWebOverlaysPausesNavigatesAndOpensBandTag() {
        launch(); compose.onNodeWithText("Story erstellen").assertDoesNotExist(); tap("Story ansehen"); tap("Pause")
        compose.onNodeWithText("Heute live!").assertExists(); compose.onNodeWithText("Löschen").assertDoesNotExist()
        screenshot("stories-viewer")
        tap("Nächste"); waitText("Zweiter Moment"); tap("Vorherige"); waitText("Heute live!")
        tap("Thrashkat"); waitText("Keine aktuelle Story"); compose.onNodeWithText("Thrashkat").assertExists()
    }
    @Test fun editorCanDeleteAfterConfirmationAndHomeRingRefreshes() {
        launch(true); tap("Story ansehen"); tap("Pause"); tap("Löschen"); tap("Abbrechen")
        assertFalse(requests.any { it.startsWith("DELETE") }); tap("Löschen"); tap("Endgültig löschen")
        waitText("Zweiter Moment"); tap("Schließen"); waitText("Keine aktuelle Story")
        assertTrue(requests.contains("DELETE /api/bands/62/stories/1"))
    }
    @Test fun failedDeletionKeepsStoryAndCanBeRetried() {
        deleteFails = true; launch(true); tap("Story ansehen"); tap("Pause"); tap("Löschen"); tap("Endgültig löschen")
        waitText("Story konnte nicht gelöscht werden.")
        assertTrue(active)
        deleteFails = false; tap("Endgültig löschen"); waitText("Zweiter Moment")
        assertEquals(2, requests.count { it == "DELETE /api/bands/62/stories/1" })
    }
    @Test fun followedBandRingOpensStories() {
        launch(true, true)
        compose.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToNode(hasContentDescription("Story von Hey Nille öffnen"))
        compose.onNodeWithContentDescription("Story von Hey Nille öffnen").performClick(); tap("Pause")
        compose.onNodeWithText("Heute live!").assertExists()
    }
    @Test fun unrelatedLoggedInUserCannotCreateOrDelete() {
        permission = "VIEW"; launch(true)
        compose.onNodeWithText("Story erstellen").assertDoesNotExist(); tap("Story ansehen"); tap("Pause")
        compose.onNodeWithText("Löschen").assertDoesNotExist()
    }
    @Test fun composerUploadsPhotoPreservesDraftAndPostsTextAndBandSnapshot() {
        launch(true); tap("Story erstellen"); waitText("Story-Foto")
        val values = ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME,"giglister-story-test.png"); put(MediaStore.Images.Media.MIME_TYPE,"image/png") }
        uploadUri = app.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)!!
        app.contentResolver.openOutputStream(uploadUri!!)!!.use { it.write(png) }
        scenario!!.onActivity { activity -> activity.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newUri(activity.contentResolver,"Story-Foto",uploadUri)) }
        scroll("Bild aus Zwischenablage")
        compose.waitUntil(15000) { requests.contains("POST /api/uploads") }
        scroll("+ Text"); scroll("Story-Text")
        compose.onNodeWithText("Story-Text").performTextInput("Wir sehen uns heute!"); Espresso.closeSoftKeyboard()
        scenario!!.recreate(); waitText("Story-Text")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Story-Text"))
        compose.onNode(hasSetTextAction() and hasText("Wir sehen uns heute!")).assertExists()
        scroll("+ Band"); compose.onNodeWithText("Band suchen").performTextInput("Thrash")
        tap("Thrashkat"); Espresso.closeSoftKeyboard()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Kasten hinter der Ebene"))
        compose.onNode(isToggleable()).performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("+ Text"))
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("story-editor-preview"))
        compose.onNodeWithTag("story-editor-preview").performTouchInput { swipe(center, center + androidx.compose.ui.geometry.Offset(25f, -30f), 400) }
        screenshot("stories-editor")
        scroll("Story veröffentlichen")
        compose.waitUntil(15000) { postBody != null }
        val input = storyJson.decodeFromString<StoryInput>(postBody!!)
        assertEquals("Wir sehen uns heute!", input.text)
        assertEquals(63L, parseStoryLayers(input.bandTagsJson,true).single().bandId)
        assertTrue(parseStoryLayers(input.bandTagsJson,true).single().hasBox)
        assertTrue(parseStoryLayers(input.bandTagsJson,true).single().centerXPct > 50f)
        assertEquals(100f,input.imgWidthPct,.1f); assertEquals(100f,input.imgHeightPct,.1f)
        assertNotEquals("#111111",input.imgBackgroundColor)
        waitText("Story ansehen")
    }
}
