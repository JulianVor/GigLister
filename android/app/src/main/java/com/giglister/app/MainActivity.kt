package com.giglister.app

import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import com.giglister.app.util.incomingRoute
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.giglister.app.location.LocationRepository
import com.giglister.app.ui.GigListerNavHost
import com.giglister.app.ui.theme.GigListerTheme

class MainActivity : ComponentActivity() {
    private val pendingRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingRoute.value = incomingRoute(intent.dataString, intent.getStringExtra("eventId"))
        enableEdgeToEdge()

        val app = application as GigListerApp
        val locationRepository = LocationRepository(this)

        setContent {
            GigListerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GigListerNavHost(authRepository = app.authRepository, locationRepository = locationRepository, incomingRoute = pendingRoute.value, consumeRoute = { pendingRoute.value = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute.value = incomingRoute(intent.dataString, intent.getStringExtra("eventId"))
    }
}
