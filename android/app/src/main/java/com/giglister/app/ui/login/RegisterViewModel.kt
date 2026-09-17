package com.giglister.app.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giglister.app.data.AuthRepository
import com.giglister.app.util.errorMessage
import kotlinx.coroutines.launch

data class RegisterUiState(
    val email: String = "",
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    var uiState by mutableStateOf(RegisterUiState())
        private set

    fun onEmailChange(value: String) {
        uiState = uiState.copy(email = value)
    }

    fun onUsernameChange(value: String) {
        uiState = uiState.copy(username = value)
    }

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(password = value)
    }

    fun register() {
        if (!Regex("^[A-Za-z0-9_.-]{3,30}$").matches(uiState.username) || uiState.password.length < 8 || !uiState.email.contains('@')) {
            uiState = uiState.copy(error = "Bitte eine gültige E-Mail, einen Nutzernamen mit 3–30 Zeichen und ein Passwort mit mindestens 8 Zeichen angeben.")
            return
        }
        uiState = uiState.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                if (!com.giglister.app.data.api.ApiClient.api.usernameAvailable(uiState.username).available) {
                    uiState = uiState.copy(loading = false, error = "Dieser Nutzername ist bereits vergeben.")
                    return@launch
                }
                authRepository.register(uiState.email, uiState.password, uiState.username)
                uiState = uiState.copy(loading = false, success = true)
            } catch (e: Exception) {
                uiState = uiState.copy(loading = false, error = errorMessage(e))
            }
        }
    }
}
