package com.giglister.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.giglister.app.data.api.ApiClient
import com.giglister.app.data.api.TokenStore
import com.giglister.app.data.model.LoginRequest
import com.giglister.app.data.model.MeResponse
import com.giglister.app.data.model.RegisterRequest
import kotlinx.coroutines.flow.first

private val Context.authDataStore by preferencesDataStore(name = "auth")
private val TOKEN_KEY = stringPreferencesKey("token")

/** Persists the login token to disk (DataStore) and keeps TokenStore (the in-memory copy
 * the network layer actually reads from) in sync with it. */
class AuthRepository(private val context: Context) {

    /** Call once, at app startup, before any screen might need to know whether someone's
     * already logged in - restores the in-memory token from disk. */
    suspend fun restoreSession() {
        TokenStore.token = context.authDataStore.data.first()[TOKEN_KEY]
    }

    val isLoggedIn: Boolean
        get() = TokenStore.token != null

    suspend fun login(username: String, password: String): MeResponse {
        val response = ApiClient.api.login(LoginRequest(username, password))
        saveToken(response.token)
        return ApiClient.api.me()
    }

    suspend fun register(email: String, password: String, username: String) {
        ApiClient.api.register(RegisterRequest(email, password, username))
        // No auto-login here - the backend requires a confirmed email address before a
        // token will actually work (see AuthController/AuthService), so the app's job is
        // just to tell the user to check their inbox, same as the web's own /registrieren.
    }

    suspend fun logout() {
        context.authDataStore.edit { it.remove(TOKEN_KEY) }
        TokenStore.token = null
    }

    private suspend fun saveToken(token: String) {
        context.authDataStore.edit { it[TOKEN_KEY] = token }
        TokenStore.token = token
    }
}
