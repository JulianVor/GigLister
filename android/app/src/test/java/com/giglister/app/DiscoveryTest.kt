package com.giglister.app

import com.giglister.app.ui.*
import com.giglister.app.data.model.*
import com.giglister.app.util.incomingRoute
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DiscoveryTest {
    @Test fun coordinateSearchDoesNotExcludeNeighbouringCities() {
        val query = SearchArea("Hamburg", 53.5, 9.9, 50).query()
        assertFalse(query.containsKey("city")); assertEquals("50", query["radiusKm"])
        assertEquals(mapOf("city" to "Hamburg"), SearchArea("Hamburg").query())
        assertTrue(SearchArea().query().isEmpty())
    }
    @Test fun weekAndWeekendMatchWebAcrossMonthBoundaries() {
        val friday = LocalDate.parse("2026-01-30")
        assertEquals(LocalDate.parse("2026-01-31") to LocalDate.parse("2026-02-01"), dateRange(DateFilter.WEEKEND, friday))
        assertEquals(LocalDate.parse("2026-02-07") to LocalDate.parse("2026-02-08"), dateRange(DateFilter.WEEKEND, LocalDate.parse("2026-02-01")))
        assertEquals(friday to LocalDate.parse("2026-02-05"), dateRange(DateFilter.WEEK, friday))
        assertEquals(LocalDate.parse("2026-01-31") to LocalDate.parse("2026-01-31"), dateRange(DateFilter.TOMORROW, friday))
    }
    @Test fun editRightsNeverGrantManagementOrPlatformAdminUI() {
        val user = MeResponse(4, "test@example.org", "tester", managedEntities = listOf(ManagedEntity("BAND", 9, "Band", "EDIT")))
        val event = EventResponse(1, date = "2026-09-12", location = LocationSummary(2, "Club", "Hamburg", "PUBLISHED"), bands = listOf(BandSummary(9, "Band", status = "PUBLISHED")), status = "PUBLISHED", createdBy = 8)
        assertTrue(user.canEdit(event)); assertTrue(user.canEdit("BAND", 9)); assertFalse(user.canManage("BAND", 9))
        assertFalse(user.canEdit("LOCATION", 2)); assertFalse((null as MeResponse?).canEdit(event))
        assertFalse(user.copy(platformAdmin = true, managedEntities = emptyList()).canEdit(event))
        assertTrue(user.copy(id = 8, managedEntities = emptyList()).canEdit(event))
    }
    @Test fun linksRejectForeignHostsAndUnknownRoutes() {
        assertEquals("event/38", incomingRoute("https://sandbox.fotosvorju.de/konzerte/38"))
        assertEquals("locations/9", incomingRoute("https://sandbox.fotosvorju.de/orte/9"))
        assertEquals("event/38", incomingRoute(null, "38"))
        assertEquals("reset?token=a%2Fb", incomingRoute("https://sandbox.fotosvorju.de/passwort-zuruecksetzen?token=a%2Fb"))
        assertNull(incomingRoute("https://evil.example/konzerte/38"))
        assertNull(incomingRoute("https://sandbox.fotosvorju.de/admin/users"))
        assertNull(incomingRoute("https://sandbox.fotosvorju.de/konzerte/-1"))
        assertNull(incomingRoute("https://sandbox.fotosvorju.de/email-bestaetigen"))
    }
}
