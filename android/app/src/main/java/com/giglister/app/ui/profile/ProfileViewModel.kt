package com.giglister.app.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giglister.app.data.AuthRepository
import com.giglister.app.data.api.ApiClient
import com.giglister.app.data.model.DeviceTokenRequest
import com.giglister.app.data.model.MeResponse
import com.giglister.app.data.model.ProfileUpdateRequest
import com.giglister.app.location.LocationRepository
import com.giglister.app.util.errorMessage
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ProfileUiState(
    val me: MeResponse? = null,
    val radiusKm: Int = 25,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    var uiState by mutableStateOf(ProfileUiState())
        private set

    fun load() {
        uiState = uiState.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val me = ApiClient.api.me()
                uiState = uiState.copy(loading = false, me = me, radiusKm = me.radiusKm ?: 25)
            } catch (e: Exception) {
                uiState = uiState.copy(loading = false, error = errorMessage(e))
            }
        }
    }

    fun setRadius(radiusKm: Int) {
        uiState = uiState.copy(radiusKm = radiusKm)
    }

    /** Same "type a city vs. use my position" choice the web profile form offers - the
     * app only needs the GPS half of that, since it always has a real position on hand. */
    fun saveWithCurrentLocation() {
        uiState = uiState.copy(saving = true, error = null, message = null)
        viewModelScope.launch {
            val location = locationRepository.getCurrentLocation()
            if (location == null) {
                uiState = uiState.copy(saving = false, error = "Standort konnte nicht ermittelt werden.")
                return@launch
            }
            try {
                val me = ApiClient.api.updateProfile(
                    ProfileUpdateRequest(
                        homeLatitude = location.latitude,
                        homeLongitude = location.longitude,
                        radiusKm = uiState.radiusKm
                    )
                )
                uiState = uiState.copy(saving = false, me = me, message = "Gespeichert.")
            } catch (e: Exception) {
                uiState = uiState.copy(saving = false, error = errorMessage(e))
            }
        }
    }

    /** Unregisters this device's push token first (best-effort - a failure here shouldn't
     * block logging out) so a signed-out phone stops receiving another account's pushes,
     * matching the web's own logout behavior (see MeController.unregisterDeviceToken). */
    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                ApiClient.api.unregisterDeviceToken(DeviceTokenRequest(token))
            }
            authRepository.logout()
            onLoggedOut()
        }
    }
}
