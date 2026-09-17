package com.giglister.app.ui

import android.content.Context
import android.location.Geocoder
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giglister.app.data.AuthRepository
import com.giglister.app.data.api.ApiClient
import com.giglister.app.data.model.*
import com.giglister.app.location.LocationRepository
import com.giglister.app.util.errorMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class DateFilter(val label: String) { ALL("Alle"), TODAY("Heute"), TOMORROW("Morgen"), WEEKEND("Wochenende"), WEEK("7 Tage") }

fun dateRange(filter: DateFilter, today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> = when (filter) {
    DateFilter.ALL -> today to today.plusYears(1)
    DateFilter.TODAY -> today to today
    DateFilter.TOMORROW -> today.plusDays(1).let { it to it }
    DateFilter.WEEK -> today to today.plusDays(6)
    DateFilter.WEEKEND -> {
        val from = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        from to from.plusDays(1)
    }
}

data class SearchArea(val city: String = "", val lat: Double? = null, val lon: Double? = null, val radius: Int = 25) {
    val hasCoordinates get() = lat != null && lon != null
    val label get() = if (hasCoordinates) "${city.ifBlank { "Aktueller Standort" }} · $radius km" else city.ifBlank { "Alle Orte · Standort wählen" }
    fun query(): Map<String, String> = buildMap {
        if (hasCoordinates) { put("lat", lat.toString()); put("lon", lon.toString()); put("radiusKm", radius.toString()) }
        else if (city.isNotBlank()) put("city", city)
    }
}

class GigState(val auth: AuthRepository, private val location: LocationRepository, context: Context) : ViewModel() {
    private val application = context.applicationContext as android.app.Application
    private val prefs = context.getSharedPreferences("discovery", Context.MODE_PRIVATE)
    var area by mutableStateOf(SearchArea(prefs.getString("city", "") ?: "", prefs.getString("lat", null)?.toDoubleOrNull(), prefs.getString("lon", null)?.toDoubleOrNull(), prefs.getInt("radius", 25)))
        private set
    var filter by mutableStateOf(DateFilter.ALL)
    var genres by mutableStateOf<List<String>>(emptyList())
    var genrePrompt by mutableStateOf(false)
    fun genreQuery(): Map<String, String> = if (genres.isEmpty()) emptyMap() else mapOf("genre" to genres.joinToString(","))
    var loggedIn by mutableStateOf(auth.isLoggedIn); private set
    var me by mutableStateOf<MeResponse?>(null); private set
    var revision by mutableIntStateOf(0); private set
    var notice by mutableStateOf<String?>(null)
    init { if (loggedIn) viewModelScope.launch { runCatching { reloadMe(!prefs.contains("radius")) }.onFailure { handleFailure(it) } } }

    fun changeArea(value: SearchArea) {
        area = value
        prefs.edit().putString("city", value.city).putString("lat", value.lat?.toString()).putString("lon", value.lon?.toString()).putInt("radius", value.radius).apply()
    }

    @Suppress("DEPRECATION")
    suspend fun findCity(city: String, radius: Int): SearchArea = withContext(Dispatchers.IO) {
        require(city.isNotBlank()) { "Bitte eine Stadt eingeben." }
        val address = try { Geocoder(application, Locale.GERMANY).getFromLocationName(city.trim(), 1)?.firstOrNull() } catch (e: Exception) { null }
        SearchArea(city.trim(), address?.latitude, address?.longitude, radius)
    }

    suspend fun gps(radius: Int): SearchArea {
        val fix = withTimeoutOrNull(15000) { location.getCurrentLocation() }
            ?: throw IllegalStateException("Standort nicht verfügbar. Du kannst stattdessen eine Stadt eingeben.")
        return SearchArea(lat = fix.latitude, lon = fix.longitude, radius = radius)
    }

    suspend fun reloadMe(applyHome: Boolean = false) {
        if (!auth.isLoggedIn) return
        val profile = try { ApiClient.api.me() } catch (e: Exception) { handleFailure(e); throw e }
        me = profile
        loggedIn = true
        if (applyHome) changeArea(SearchArea(profile.homeCity.orEmpty(), profile.homeLatitude, profile.homeLongitude, profile.radiusKm ?: 25))
    }

    suspend fun signedIn() { loggedIn = true; reloadMe(true); revision++ }
    fun acceptLogin(profile: MeResponse) {
        me = profile
        loggedIn = true
        genrePrompt = profile.preferredGenres.isEmpty()
        changeArea(SearchArea(profile.homeCity.orEmpty(), profile.homeLatitude, profile.homeLongitude, profile.radiusKm ?: 25))
        revision++
    }
    suspend fun changed() { revision++; if (loggedIn) reloadMe() }
    suspend fun logout() {
        withTimeoutOrNull(3000) {
            runCatching {
                val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                val value = token.await()
                ApiClient.api.unregisterDeviceToken(DeviceTokenRequest(value))
            }
        }
        auth.logout(); me = null; loggedIn = false; revision++
    }

    suspend fun handleFailure(error: Throwable) {
        if (error is CancellationException) throw error
        if (error is HttpException && error.code() == 401) {
            auth.logout(); me = null; loggedIn = false; revision++
            notice = "Bitte melde dich erneut an."
        } else notice = errorMessage(error)
    }
}

/** Exhaust every page for calendar/map; an incomplete response is never shown as complete. */
suspend fun allEvents(query: Map<String, String>): List<EventResponse> {
    val events = mutableListOf<EventResponse>()
    var page = 0
    do {
        currentCoroutineContext().ensureActive()
        val response = ApiClient.api.events(query + mapOf("page" to page.toString(), "size" to "100"))
        events.addAll(response.content)
        page++
    } while (page < response.totalPages)
    return events.distinctBy { it.id }
}
