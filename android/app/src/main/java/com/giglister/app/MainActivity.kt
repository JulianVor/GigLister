package com.giglister.app

import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as GigListerApp
        val locationRepository = LocationRepository(this)

        setContent {
            GigListerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GigListerNavHost(authRepository = app.authRepository, locationRepository = locationRepository)
                }
            }
        }
    }
}
