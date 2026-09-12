package com.giglister.app.ui.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giglister.app.data.api.ApiClient
import com.giglister.app.data.model.EventResponse
import com.giglister.app.location.LocationRepository
import com.giglister.app.util.errorMessage
import kotlinx.coroutines.launch

data class EventsUiState(
    val loading: Boolean = false,
    val events: List<EventResponse> = emptyList(),
    val error: String? = null,
    val radiusKm: Int = 25
)

class EventsViewModel(private val locationRepository: LocationRepository) : ViewModel() {

    var uiState by mutableStateOf(EventsUiState())
        private set

    /** Called once the events screen has confirmed location permission is granted -
     * fetches a fresh GPS fix every time rather than caching one, since "near me" should
     * reflect wherever the phone actually is right now, not wherever it was on a
     * previous visit to this screen. */
    fun loadNearbyEvents() {
        uiState = uiState.copy(loading = true, error = null)
        viewModelScope.launch {
            val location = locationRepository.getCurrentLocation()
            if (location == null) {
                uiState = uiState.copy(loading = false, error = "Standort konnte nicht ermittelt werden.")
                return@launch
            }
            try {
                val page = ApiClient.api.getEvents(
                    lat = location.latitude,
                    lon = location.longitude,
                    radiusKm = uiState.radiusKm,
                    size = 50
                )
                uiState = uiState.copy(loading = false, events = page.content, error = null)
            } catch (e: Exception) {
                uiState = uiState.copy(loading = false, error = errorMessage(e))
            }
        }
    }
}
