package com.giglister.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.giglister.app.data.api.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArrayList

/** Runs against an in-process fake server. No accounts or events on the live service are changed. */
@RunWith(AndroidJUnit4::class)
class MainFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var server: MockWebServer
    private lateinit var original: GigListerApi
    private var scenario: ActivityScenario<MainActivity>? = null
    private val requests = CopyOnWriteArrayList<String>()
    private var saved = false
    private var permission = "EDIT"
    private var eventBody: String? = null
    private var reviewOnly = false
    private var festivalMode = false
    private var festivalGrid = false
    private var passwordRequired = false
    private var actSaved = false
    private var profileBody: String? = null
    private var festivalBody: String? = null
    private var eventGenre: String? = null
    private val today = LocalDate.now().toString()
    private val event get() = """{"id":38,"title":"Rock Jam 2026","date":"$today","startTime":"19:00:00","description":"Vier Bands. Ein unvergesslicher Abend.","ticketUrl":"https://example.org/tickets","location":{"id":9,"name":"Stellwerk Hamburg","city":"Hamburg","status":"PUBLISHED","linkable":true,"latitude":53.46,"longitude":9.99},"bands":[{"id":62,"name":"Hey Nille","status":"PUBLISHED","linkable":true,"genres":["Pop-Punk"]},{"id":64,"name":"Thrashkat","status":"PUBLISHED","linkable":true,"genres":["Emo Punk"]},{"id":63,"name":"Granny’s Milk","status":"PUBLISHED","linkable":true,"genres":["Punkrock"]},{"id":65,"name":"Out of Lights","status":"PUBLISHED","linkable":true,"genres":["Metalcore"]}],"bandImageDisplay":"PHOTO","status":"PUBLISHED","createdBy":88}"""
    private val me get() = """{"id":4,"email":"tester@example.org","username":"tester","homeCity":"Hamburg","radiusKm":50,"preferredGenres":["Rock"],"mustChangePassword":$passwordRequired,"savedActs":[${if(actSaved) """{"eventId":38,"bandId":62}""" else ""}],"savedEvents":[${if (saved) festivalEvent else ""}],"followedBands":[],"managedEntities":[{"entityType":"BAND","entityId":62,"name":"Hey Nille","permission":"$permission"},{"entityType":"EVENT_SERIES","entityId":5,"name":"MusicNight","permission":"EDIT"}]}"""
    private val festivalEvent get() = if (festivalMode) event.dropLast(1) + """, "eventSeries":{"id":5,"name":"MusicNight","ticketUrl":"https://example.org/festival"}}""" else event
    private val band get() = """{"id":62,"name":"Hey Nille","city":"Hamburg","status":"PUBLISHED","genres":["Pop-Punk"],"upcomingEvents":[$event],"unclaimed":false}"""

    @Before fun setup() {
        val app = ApplicationProvider.getApplicationContext<GigListerApp>()
        runBlocking { app.authRepository.logout() }
        app.getSharedPreferences("discovery", 0).edit().clear().commit()
        original = ApiClient.api
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl!!.encodedPath
                requests.add("${request.method} $path")
                if (path == "/api/bands/62" && request.method == "DELETE") return MockResponse().setResponseCode(409).setHeader("Content-Type", "application/json").setBody("""{"message":"Diese Band hat noch Konzerte und kann daher nicht gelöscht werden."}""")
                if (path.startsWith("/api/admin")) return MockResponse().setResponseCode(500)
                val body = when (path) {
                    "/api/events" -> if (request.method == "POST") { eventBody = request.body.readUtf8(); if (reviewOnly) """{"published":false,"event":null,"submission":{"id":74,"status":"PENDING"}}""" else """{"published":true,"event":$event,"submission":null}""" } else {
                        val from = request.requestUrl!!.queryParameter("from")
                        eventGenre = request.requestUrl!!.queryParameter("genre")
                        val list = if (from != null && from > today) "" else event
                        """{"content":[$list],"totalPages":1,"totalElements":1}"""
                    }
                    "/api/events/38" -> festivalEvent
                    "/api/events/38/bands/62/save" -> { actSaved = request.method == "POST"; saved = actSaved; return MockResponse().setResponseCode(204) }
                    "/api/events/38/save" -> { saved = request.method == "POST"; return MockResponse().setResponseCode(204) }
                    "/api/me" -> { if (request.method == "PUT") profileBody = request.body.readUtf8(); me }
                    "/api/me/password" -> { passwordRequired = false; return MockResponse().setResponseCode(204) }
                    "/api/me/events", "/api/me/bands" -> "[]"
                    "/api/genres" -> """["Rock","Nu-Metal","Progressive Metal"]"""
                    "/api/events/genres" -> """[{"genre":"Rock","eventCount":1},{"genre":"Nu-Metal","eventCount":1}]"""
                    "/api/event-series" -> if (request.method == "POST") { festivalBody = request.body.readUtf8(); """{"id":5,"name":"MusicNight","timetableStyle":"GRID","events":[]}""" } else """[{"id":5,"name":"MusicNight"}]"""
                    "/api/event-series/5" -> """{"id":5,"name":"MusicNight","timetableStyle":"GRID","events":[$festivalEvent${if (festivalGrid) "," + festivalEvent.replace("\"id\":38", "\"id\":39").replace("\"id\":9", "\"id\":10").replace("Stellwerk Hamburg", "Zweite Bühne").replace("Rock Jam 2026", "Andere Show") else ""}]}"""
                    "/api/me/submissions" -> if (reviewOnly) """[{"id":74,"status":"PENDING"}]""" else "[]"
                    "/api/auth/login" -> """{"token":"test-token","userId":4,"email":"tester@example.org","username":"tester","platformAdmin":false}"""
                    "/api/me/device-token" -> return MockResponse().setResponseCode(204)
                    "/api/bands/62/stories" -> "[]"
                    "/api/bands/62" -> band
                    "/api/bands/62/permissions" -> "[]"
                    "/api/locations/9" -> """{"id":9,"name":"Stellwerk Hamburg","city":"Hamburg","address":"Hannoversche Straße 85","status":"PUBLISHED","upcomingEvents":[$event]}"""
                    "/api/locations" -> """{"content":[{"id":9,"name":"Stellwerk Hamburg","city":"Hamburg","upcomingEventCount":1}],"totalPages":1}"""
                    "/api/discover" -> """{"todayNearby":[$event],"thisWeekend":[],"newEvents":[],"locationsWithUpcomingShows":[],"bandsPlayingSoon":[]}"""
                    "/api/search" -> """{"events":[$event],"bands":[$band],"locations":[]}"""
                    "/api/bands/duplicates" -> """[{"id":62,"name":"Hey Nille","city":"Hamburg"}]"""
                    "/api/locations/duplicates" -> """[{"id":9,"name":"Stellwerk Hamburg","city":"Hamburg"}]"""
                    else -> return MockResponse().setResponseCode(404).setBody("""{"message":"Not in test: $path"}""")
                }
                return MockResponse().setHeader("Content-Type", "application/json").setBody(body)
            }
        }
        server.start()
        ApiClient.api = ApiClient.createApi(server.url("/").toString())
    }
    @After fun cleanup() {
        compose.waitForIdle()
        scenario?.close(); ApiClient.api = original
        runBlocking { ApplicationProvider.getApplicationContext<GigListerApp>().authRepository.logout() }
        server.shutdown()
        assertFalse(requests.any { it.contains("/api/admin") })
    }
    private fun launch(loggedIn: Boolean = false) {
        if (loggedIn) runBlocking { ApplicationProvider.getApplicationContext<GigListerApp>().authRepository.saveToken("test-token") }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        tap("Konzerte"); awaitText("Rock Jam 2026")
    }
    private fun awaitText(text: String) {
        try { compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
        catch (e: Throwable) { println("Waiting for $text. Requests: $requests\n${compose.onRoot().printToString()}"); throw e }
    }
    private fun tap(text: String) { awaitText(text); compose.onNodeWithText(text).performClick() }
    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val folder = File(ApplicationProvider.getApplicationContext<GigListerApp>().getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun guestCanBrowseAndChooseManualLocationWithoutPermission() {
        launch(); capture("home")
        compose.onNodeWithText("Admin").assertDoesNotExist()
        tap("Alle Orte · Standort wählen"); awaitText("Wo soll es hingehen?")
        compose.onNodeWithText("Stadt").assertExists()
        tap("Alle Orte")
        tap("Rock Jam 2026"); awaitText("Tickets"); capture("event")
        compose.onNodeWithText("Bearbeiten").assertDoesNotExist()
        compose.onNodeWithText("Merken").performScrollTo().performClick()
        awaitText("Anmelden")
        assertFalse(requests.any { it == "POST /api/events/38/save" })
    }
    @Test fun saveAfterLoginAppearsInPersonalList() {
        launch(); tap("Rock Jam 2026")
        compose.onNodeWithText("Merken").performScrollTo().performClick()
        awaitText("Nutzername")
        compose.onNodeWithText("Nutzername").performTextInput("tester")
        compose.onNodeWithText("Passwort", substring = false).performTextInput("password123")
        tap("Anmelden"); awaitText("Tickets")
        compose.onNodeWithText("Merken").performScrollTo().performClick(); awaitText("Gemerkt ✓")
        assertTrue(saved)
        compose.onNodeWithContentDescription("Zurück").performClick()
        tap("Mein GigLister"); awaitText("tester · tester@example.org")
        compose.onNodeWithText("GEMERKT · 1").performScrollTo().assertExists()
    }
    @Test fun dateFilterCalendarAndSearchNavigate() {
        launch(); tap("Morgen"); awaitText("Hier ist es noch ruhig. Probiere einen anderen Zeitraum oder Standort.")
        tap("Kalender"); awaitText("Alle Orte · Standort wählen")
        compose.onNodeWithContentDescription("Zurück").performClick()
        compose.onNodeWithContentDescription("Suche").performClick()
        compose.onNodeWithText("Band, Konzert oder Ort").performTextInput("Hey Nille")
        tap("Suchen"); awaitText("Rock Jam 2026")
        tap("Rock Jam 2026"); awaitText("Tickets")
    }
    @Test fun editPermissionDoesNotExposeManagementControls() {
        launch(true)
        tap("Rock Jam 2026"); awaitText("Bearbeiten")
        compose.onNodeWithText("Hey Nille").performScrollTo().performClick(); awaitText("Band folgen")
        tap("Bearbeiten"); awaitText("Name *")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Profil speichern"))
        compose.onNodeWithText("Veröffentlichung").assertDoesNotExist()
        compose.onNodeWithText("GEMEINSAM VERWALTEN").assertDoesNotExist()
    }
    @Test fun concertCreationKeepsSelectedEntitiesAcrossRecreation() {
        launch(true)
        compose.onNodeWithContentDescription("Konzert anlegen").performClick()
        awaitText("Ort auswählen oder hinzufügen"); tap("Ort auswählen oder hinzufügen")
        compose.onNodeWithText("Name").performTextInput("Stellwerk")
        tap("Stellwerk Hamburg · Hamburg")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Weitere Band auswählen oder hinzufügen"))
        tap("Weitere Band auswählen oder hinzufügen")
        compose.onNodeWithText("Name").performTextInput("Hey Nille")
        tap("Hey Nille · Hamburg")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Festival (optional)"))
        tap("Festival (optional)"); tap("MusicNight")
        compose.waitForIdle()
        scenario!!.recreate()
        compose.waitForIdle()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Konzert einreichen"))
        tap("Konzert einreichen"); awaitText("Tickets")
        compose.waitForIdle()
        val body = Json.parseToJsonElement(eventBody!!).jsonObject
        assertEquals(9, body["location"]!!.jsonObject["id"]!!.jsonPrimitive.int)
        assertEquals(62, body["bands"]!!.jsonArray.single().jsonObject["id"]!!.jsonPrimitive.int)
        assertEquals("PHOTO", body["bandImageDisplay"]!!.jsonPrimitive.content)
        assertEquals(5, body["eventSeriesId"]!!.jsonPrimitive.int)
        assertEquals(1, requests.count { it == "POST /api/events" })
    }
    @Test fun managerCanReachObjectPermissionsWithoutAdminRoutes() {
        permission = "MANAGE"
        launch(true); tap("Rock Jam 2026"); awaitText("Tickets")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Hey Nille", substring = false))
        tap("Hey Nille"); awaitText("Band folgen"); tap("Bearbeiten"); awaitText("Name *")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("VERÖFFENTLICHUNG"))
        compose.onNodeWithText("Veröffentlicht").assertExists()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Nutzer-ID"))
        compose.onNodeWithText("Nutzer-ID").performTextInput("7")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Nutzer hinzufügen"))
        tap("Nutzer hinzufügen")
        try { compose.waitUntil(10000) { requests.any { it == "POST /api/bands/62/permissions" } } }
        catch (e: Throwable) { throw AssertionError("Requests: $requests\n${compose.onRoot().printToString()}", e) }
    }

    @Test fun pendingConcertShowsConfirmationAndOwnSubmissionStatus() {
        reviewOnly = true
        launch(true)
        compose.onNodeWithContentDescription("Konzert anlegen").performClick()
        tap("Ort auswählen oder hinzufügen")
        compose.onNodeWithText("Name").performTextInput("Stellwerk"); tap("Stellwerk Hamburg · Hamburg")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Weitere Band auswählen oder hinzufügen"))
        tap("Weitere Band auswählen oder hinzufügen")
        compose.onNodeWithText("Name").performTextInput("Hey Nille"); tap("Hey Nille · Hamburg")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Konzert einreichen")); tap("Konzert einreichen")
        awaitText("Vorschlag eingereicht")
        tap("Meine Vorschläge"); awaitText("Wird geprüft")
        assertEquals(1, requests.count { it == "POST /api/events" })
    }

    @Test fun temporaryPasswordBlocksContentUntilChanged() {
        passwordRequired = true
        runBlocking { ApplicationProvider.getApplicationContext<GigListerApp>().authRepository.saveToken("test-token") }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        awaitText("Aktuelles Passwort")
        compose.onNodeWithText("Konzerte").assertDoesNotExist()
        compose.onNodeWithText("Aktuelles Passwort").performTextInput("temporary123")
        compose.onNodeWithText("Neues Passwort").performTextInput("newPassword123")
        compose.onNodeWithText("Passwort wiederholen").performTextInput("newPassword123")
        compose.onNodeWithText("Passwort speichern").performScrollTo().performClick()
        awaitText("Start")
        assertTrue(requests.contains("PUT /api/me/password"))
    }

    @Test fun favoriteGenresCanBeSavedFromAccount() {
        launch(true); tap("Mein GigLister"); tap("Lieblingsgenres")
        awaitText("Nu-Metal"); tap("Nu-Metal")
        compose.onNodeWithText("Genres speichern").performScrollTo().performClick()
        compose.waitUntil(5000) { profileBody != null }
        val selected = Json.parseToJsonElement(profileBody!!).jsonObject["preferredGenres"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertEquals(listOf("Rock", "Nu-Metal"), selected)
    }

    @Test fun festivalCanSaveSingleActAndFilterToSavedLineup() {
        festivalMode = true
        launch(true); tap("Festivals"); tap("MusicNight"); awaitText("Rock Jam 2026")
        tap("Rock Jam 2026"); awaitText("Das Line-up".uppercase())
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Hey Nille: Auftritt merken"))
        tap("Hey Nille: Auftritt merken"); awaitText("Hey Nille: Auftritt gemerkt ✓")
        compose.onNodeWithContentDescription("Zurück").performClick()
        tap("Gemerkte Konzerte")
        awaitText("Rock Jam 2026")
        assertTrue(actSaved)
        compose.onNodeWithText("Alle Konzerte").assertExists()
        tap("Alle Konzerte")
        compose.onNodeWithText("Rock Jam 2026").assertExists()
    }

    @Test fun multipleGenresAreCombinedAsOneOrQuery() {
        launch(); tap("Genres"); tap("Rock (1)"); tap("Nu-Metal (1)"); tap("Fertig")
        compose.waitUntil(5000) { eventGenre == "Rock,Nu-Metal" }
        compose.onNodeWithText("Genres · 2").assertExists()
    }

    @Test fun festivalCreationSendsSelectedTimetableStyle() {
        launch(true); tap("Festivals"); tap("+ Festival")
        compose.onNodeWithText("Name *").performTextInput("MusicNight")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Raster")); tap("Raster")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Festival speichern")); tap("Festival speichern")
        awaitText("KONZERTE DIESES FESTIVALS")
        assertEquals("GRID", Json.parseToJsonElement(festivalBody!!).jsonObject["timetableStyle"]!!.jsonPrimitive.content)
    }

    @Test fun deletingBandWithConcertsShowsServerConflictAndKeepsProfile() {
        permission = "MANAGE"
        launch(true); tap("Rock Jam 2026"); awaitText("Tickets")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Hey Nille", substring = false)); tap("Hey Nille")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Band löschen")); tap("Band löschen"); tap("Endgültig löschen")
        awaitText("Diese Band hat noch Konzerte und kann daher nicht gelöscht werden.")
        tap("Abbrechen")
        compose.onNodeWithText("Band löschen").assertExists()
        assertTrue(requests.contains("DELETE /api/bands/62"))
    }

    @Test fun parallelFestivalShowsHaveVenueColumnsAndCanBeFiltered() {
        festivalMode = true; festivalGrid = true
        launch(); tap("Festivals"); tap("MusicNight")
        awaitText("Zeit")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Zeit"))
        capture("update-grid")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Alle Orte"))
        compose.onAllNodesWithText("Zweite Bühne").onFirst().performClick()
        awaitText("Andere Show")
        compose.onNodeWithText("Rock Jam 2026").assertDoesNotExist()
        compose.onNodeWithText("Zeit").assertDoesNotExist()
    }
}
