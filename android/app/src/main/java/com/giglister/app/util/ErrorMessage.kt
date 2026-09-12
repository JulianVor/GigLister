package com.giglister.app.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException

/** Turns whatever went wrong calling the API into one German sentence a screen can just
 * display - the backend's error responses are a plain {"message": "..."} JSON body (see
 * GlobalExceptionHandler on the backend), so a non-2xx response's actual validation/
 * business-rule message (e.g. "Nutzername oder Passwort ist falsch") makes it through
 * instead of a generic "something went wrong". */
fun errorMessage(e: Throwable): String = when (e) {
    is HttpException -> {
        val body = e.response()?.errorBody()?.string()
        val backendMessage = body?.let {
            runCatching { Json.parseToJsonElement(it).jsonObject["message"]?.jsonPrimitive?.content }.getOrNull()
        }
        backendMessage ?: "Serverfehler (${e.code()})"
    }
    is IOException -> "Keine Verbindung zum Server."
    else -> e.message ?: "Unbekannter Fehler"
}
