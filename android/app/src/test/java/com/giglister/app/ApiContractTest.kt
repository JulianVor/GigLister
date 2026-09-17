package com.giglister.app

import com.giglister.app.data.api.*
import com.giglister.app.data.model.*
import com.giglister.app.ui.allEvents
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*

class ApiContractTest {
    private lateinit var server: MockWebServer
    private lateinit var original: GigListerApi
    @Before fun setup() { server = MockWebServer(); server.start(); original = ApiClient.api; ApiClient.api = ApiClient.createApi(server.url("/").toString()) }
    @After fun teardown() { ApiClient.api = original; TokenStore.token = null; server.shutdown() }
    private fun respond(body: String) { server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(body)) }

    @Test fun mapAndCalendarReadBeyondFirstPage() = runBlocking {
        respond("""{"content":[],"totalPages":3,"totalElements":2}""")
        respond("""{"content":[],"totalPages":3,"totalElements":2}""")
        respond("""{"content":[],"totalPages":3,"totalElements":2}""")
        allEvents(mapOf("lat" to "53.5", "lon" to "10", "radiusKm" to "50"))
        repeat(3) { page -> val url = server.takeRequest().requestUrl!!; assertEquals(page.toString(), url.queryParameter("page")); assertEquals("50", url.queryParameter("radiusKm")) }
    }
    @Test fun accountAcceptsSummaryEventsWithoutDetailFields() = runBlocking {
        respond("""{"id":4,"email":"test@example.org","username":"tester","savedEvents":[{"id":38,"date":"2026-09-12","location":{"id":9,"name":"Club","city":"Hamburg","status":"PUBLISHED"},"bands":[],"status":"PUBLISHED"}],"followedBands":[{"id":9,"name":"Band","nextEventDate":null}],"managedEntities":[]}""")
        val profile = ApiClient.api.me()
        assertEquals("Konzert", profile.savedEvents.single().displayTitle)
        assertNull(profile.savedEvents.single().createdBy)
        assertNull(profile.followedBands.single().nextEventDate)
    }
    @Test fun mutationsUseRealUserEndpointsAndBodies() = runBlocking {
        TokenStore.token = "test-token"
        server.enqueue(MockResponse().setResponseCode(204)); ApiClient.api.unsave(38)
        val remove = server.takeRequest(); assertEquals("DELETE", remove.method); assertEquals("/api/events/38/save", remove.path); assertEquals("Bearer test-token", remove.getHeader("Authorization"))
        server.enqueue(MockResponse().setResponseCode(204)); ApiClient.api.grant("bands", 9, PermissionInput(7, "EDIT"))
        val grant = server.takeRequest(); assertEquals("/api/bands/9/permissions", grant.path)
        val json = Json.parseToJsonElement(grant.body.readUtf8()).jsonObject
        assertEquals("EDIT", json["permission"]!!.jsonPrimitive.content); assertEquals(7, json["userId"]!!.jsonPrimitive.int)
    }
}
