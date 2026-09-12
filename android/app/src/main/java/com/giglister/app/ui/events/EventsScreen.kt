package com.giglister.app.ui.events

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giglister.app.data.model.EventResponse
import com.giglister.app.location.LocationRepository
import com.giglister.app.ui.SimpleViewModelFactory

@Composable
fun EventsScreen(locationRepository: LocationRepository) {
    val context = LocalContext.current
    val viewModel: EventsViewModel = viewModel(factory = SimpleViewModelFactory { EventsViewModel(locationRepository) })
    val state = viewModel.uiState

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        hasLocationPermission = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Re-fetches whenever permission just became available (first grant, or coming back
    // to this screen already granted) - not on every recomposition, which is what keying
    // this LaunchedEffect on hasLocationPermission itself guarantees.
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            viewModel.loadNearbyEvents()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Konzerte in deiner Nähe", style = MaterialTheme.typography.headlineSmall)

        when {
            !hasLocationPermission -> {
                Text(
                    "Für Konzerte in deiner Nähe braucht GigLister deinen Standort.",
                    modifier = Modifier.padding(top = 16.dp)
                )
                Button(
                    onClick = {
                        val permissions = mutableListOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissions += Manifest.permission.POST_NOTIFICATIONS
                        }
                        permissionLauncher.launch(permissions.toTypedArray())
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Standort erlauben")
                }
            }

            state.loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Text(
                    state.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Button(onClick = viewModel::loadNearbyEvents, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Erneut versuchen")
                }
            }

            state.events.isEmpty() -> {
                Text("Keine Konzerte in der Nähe gefunden.", modifier = Modifier.padding(top = 16.dp))
            }

            else -> {
                LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
                    items(state.events, key = { it.id }) { event -> EventCard(event) }
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: EventResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(event.displayTitle, style = MaterialTheme.typography.titleMedium)
            val time = event.startTime?.take(5)
            Text(
                if (time != null) "${event.date} · $time" else event.date,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "${event.location.name} · ${event.location.city}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
