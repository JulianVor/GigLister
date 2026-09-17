package com.giglister.app.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.giglister.app.data.api.ApiClient
import com.giglister.app.data.model.*
import com.giglister.app.ui.GigState
import com.giglister.app.ui.components.*

@Composable fun RecoveryScreen(app: GigState, mode: String, token: String?, onDone: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    val action = rememberAction(app)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PageTitle(when (mode) { "forgot" -> "Wieder dabei."; "reset" -> "Neues Passwort"; else -> "E-Mail bestätigen" })
        when {
            mode == "forgot" && sent -> { Text("Falls ein Konto zu dieser E-Mail-Adresse existiert, erhältst du einen Link zum Zurücksetzen."); Button(onClick = onDone) { Text("Zurück zur Anmeldung") } }
            mode == "forgot" -> {
                Text("Wir senden dir einen Link, mit dem du dein Passwort zurücksetzen kannst.")
                OutlinedTextField(email, { email = it }, label = { Text("E-Mail") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(enabled = !action.busy && email.contains('@'), onClick = { action.run { ApiClient.api.forgot(EmailInput(email.trim())); sent = true } }) { Text("Link anfordern") }
            }
            token.isNullOrBlank() -> EmptyMessage("Dieser Link ist unvollständig. Öffne den vollständigen Link aus deiner E-Mail.")
            mode == "verify" -> {
                Text("Bestätige deine E-Mail-Adresse, um GigLister mit deinem Konto zu nutzen.")
                Button(enabled = !action.busy, onClick = { action.run { val response = ApiClient.api.verify(TokenInput(token)); app.auth.saveToken(response.token); app.signedIn(); onDone() } }) { Text("E-Mail bestätigen") }
            }
            else -> {
                OutlinedTextField(password, { password = it }, label = { Text("Neues Passwort (mindestens 8 Zeichen)") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(repeat, { repeat = it }, label = { Text("Passwort wiederholen") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(enabled = !action.busy && password.length >= 8 && password == repeat, onClick = { action.run { val response = ApiClient.api.reset(ResetInput(token, password)); app.auth.saveToken(response.token); app.signedIn(); onDone() } }) { Text("Passwort speichern") }
            }
        }
        if (action.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}
