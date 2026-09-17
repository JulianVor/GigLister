package com.giglister.app

import com.giglister.app.data.api.*
import com.giglister.app.data.model.*
import com.giglister.app.ui.browse.festivalSlots
import com.giglister.app.ui.browse.festivalTimeGroups
import com.giglister.app.util.incomingRoute
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*

class FestivalContractTest {
    private lateinit var server: MockWebServer
    private lateinit var api: GigListerApi
    private val venue = LocationSummary(9, "Club", "Hamburg", "PUBLISHED")
    private fun event(id: Long = 38, festival: FestivalSummary? = null) = EventResponse(id, date = "2026-09-18", location = venue, status = "PUBLISHED", eventSeries = festival)
    @Before fun setup() { server = MockWebServer(); server.start(); api = ApiClient.createApi(server.url("/").toString()) }
    @After fun cleanup() { server.shutdown() }
    private fun respond(body: String) { server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(body)) }

    @Test fun createDistinguishesPublishedEventAndPendingReview() = runBlocking {
        val input = EventInput("2026-09-18", EntityRef(id = 9), listOf(EntityRef(id = 62, startTime = "00:30")), eventSeriesId = 5)
        respond("""{"published":false,"event":null,"submission":{"id":74,"status":"PENDING","payload":{}}}""")
        val reviewed = api.createEvent(input)
        assertFalse(reviewed.published); assertNull(reviewed.event); assertEquals(74L, reviewed.submission!!.id)
        val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals(5, body["eventSeriesId"]!!.jsonPrimitive.int)
        assertEquals("00:30", body["bands"]!!.jsonArray[0].jsonObject["startTime"]!!.jsonPrimitive.content)
        respond("""{"published":true,"event":{"id":38,"date":"2026-09-18","location":{"id":9,"name":"Club","city":"Hamburg","status":"PUBLISHED"},"status":"PUBLISHED"},"submission":null}""")
        val direct = api.createEvent(input); assertTrue(direct.published); assertEquals(38L, direct.event!!.id)
    }
    @Test fun lateActsSortAfterEveningAndUntimedActsAreKept() {
        val show = event().copy(startTime = "20:00", bands = listOf(
            BandSummary(1, "Late", status = "PUBLISHED", startTime = "00:30"),
            BandSummary(2, "Opener", status = "PUBLISHED", startTime = "21:00"),
            BandSummary(3, "Shared", status = "PUBLISHED")))
        assertEquals(listOf("Shared", "Opener", "Late"), show.orderedBands().map { it.name })
        assertEquals(listOf("Shared", "Opener", "Late"), festivalSlots(listOf(show)).map { it.label })
        assertEquals(1470, runningMinutes("00:30", "20:00"))
    }
    @Test fun savedFestivalRowsDedupeAndActFilterNeverHidesWholeEventSaves() {
        val festival = FestivalSummary(5, "MusicNight")
        val band = BandSummary(1, "Saved act", status = "PUBLISHED")
        val show = event(festival = festival).copy(bands = listOf(band, band.copy(id = 2, name = "Other")))
        val profile = MeResponse(4, "a@example.org", "user", savedEvents = listOf(show, event(39, festival), event(40).copy(date = "2026-09-01"), event(41)))
        assertEquals(listOf(38L, 41L), upcomingSaved(profile, "2026-09-16").map { it.id })
        assertEquals(listOf(band), show.onlySavedActs(listOf(SavedAct(38, 1))).bands)
        assertEquals(show.bands, show.onlySavedActs(emptyList()).bands)
    }
    @Test fun identicalClockTimesOnDifferentNightsAreNotGroupedAsSimultaneous() {
        val early = event().copy(startTime = "00:30")
        val late = event(39).copy(startTime = "20:00", bands = listOf(BandSummary(1, "After midnight", status = "PUBLISHED", startTime = "00:30")))
        val groups = festivalTimeGroups(listOf(early, late))
        assertEquals(2, groups.size)
        assertTrue(groups.all { it.second.size == 1 })
    }
    @Test fun festivalTicketOverridesConcertAndLinksSupportSavedFilter() {
        val show = event(festival = FestivalSummary(5, "Night", ticketUrl = "https://festival.example/tickets")).copy(ticketUrl = "https://club.example/tickets")
        assertEquals("https://festival.example/tickets", show.effectiveTicketUrl)
        assertEquals("Night – Konzert", show.listTitle)
        assertEquals("festivals/5?saved=true", incomingRoute("https://sandbox.fotosvorju.de/festivals/5?filter=saved"))
    }
    @Test fun genresAndActBookmarksUseCurrentApiContract() = runBlocking {
        respond("""{"content":[],"totalPages":0}"""); api.events(mapOf("genre" to "Nu-Metal,Progressive Metal"))
        assertEquals("Nu-Metal,Progressive Metal", server.takeRequest().requestUrl!!.queryParameter("genre"))
        respond("""{"id":4,"email":"a@example.org","username":"user","mustChangePassword":true,"preferredGenres":[],"savedActs":[{"eventId":38,"bandId":62}]}""")
        val me = api.updateProfile(ProfileUpdateRequest(preferredGenres = emptyList()))
        assertTrue(me.mustChangePassword); assertEquals(SavedAct(38, 62), me.savedActs.single())
        assertEquals(JsonArray(emptyList()), Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject["preferredGenres"])
        server.enqueue(MockResponse().setResponseCode(204)); api.saveAct(38, 62)
        assertEquals("/api/events/38/bands/62/save", server.takeRequest().path)
        server.enqueue(MockResponse().setResponseCode(204)); api.unsaveAct(38, 62)
        val request = server.takeRequest(); assertEquals("DELETE", request.method); assertEquals("/api/events/38/bands/62/save", request.path)
    }
}
