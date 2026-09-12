package com.giglister.app.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giglister.app.data.AuthRepository
import com.giglister.app.data.api.ApiClient
import com.giglister.app.data.model.DeviceTokenRequest
import com.giglister.app.util.errorMessage
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    fun onUsernameChange(value: String) {
        uiState = uiState.copy(username = value)
    }

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(password = value)
    }

    fun login(onSuccess: () -> Unit) {
        if (uiState.username.isBlank() || uiState.password.isBlank()) {
            uiState = uiState.copy(error = "Bitte Nutzername und Passwort angeben.")
            return
        }
        uiState = uiState.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                authRepository.login(uiState.username, uiState.password)
                registerCurrentFcmToken()
                uiState = uiState.copy(loading = false)
                onSuccess()
            } catch (e: Exception) {
                uiState = uiState.copy(loading = false, error = errorMessage(e))
            }
        }
    }

    /** Covers the common case where FCM already handed this device a token before
     * anyone logged in (GigListerMessagingService.onNewToken has nobody to register it
     * to yet in that case, since it only fires once per token, not on every app start). */
    private suspend fun registerCurrentFcmToken() {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            ApiClient.api.registerDeviceToken(DeviceTokenRequest(token))
        }
    }
}
