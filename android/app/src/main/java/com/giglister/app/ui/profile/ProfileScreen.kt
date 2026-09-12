package com.giglister.app.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giglister.app.data.AuthRepository
import com.giglister.app.location.LocationRepository
import com.giglister.app.ui.SimpleViewModelFactory

private val RADII = listOf(10, 25, 50)

@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    locationRepository: LocationRepository,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ProfileViewModel = viewModel(
        factory = SimpleViewModelFactory { ProfileViewModel(authRepository, locationRepository) }
    )
    val state = viewModel.uiState

    LaunchedEffect(Unit) { viewModel.load() }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        hasLocationPermission = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            viewModel.saveWithCurrentLocation()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Mein GigLister", style = MaterialTheme.typography.headlineSmall)
        state.me?.let {
            Text(
                "${it.username} · ${it.email}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Text(
            "Standort",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            if (state.me?.homeLatitude != null) {
                "Standort gespeichert · ${state.radiusKm} km Umkreis"
            } else {
                "Noch kein Standort gespeichert - wird bei neuen Konzerten in der Nähe für Push-Benachrichtigungen genutzt."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Row(modifier = Modifier.padding(top = 12.dp)) {
            RADII.forEach { radius ->
                FilterChip(
                    selected = state.radiusKm == radius,
                    onClick = { viewModel.setRadius(radius) },
                    label = { Text("$radius km") },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Button(
            onClick = {
                if (hasLocationPermission) {
                    viewModel.saveWithCurrentLocation()
                } else {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                }
            },
            enabled = !state.saving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text(if (state.saving) "Wird gespeichert …" else "Aktuellen Standort speichern")
        }

        if (state.error != null) {
            Text(
                state.error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (state.message != null) {
            Text(
                state.message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Row(modifier = Modifier.weight(1f)) {}

        OutlinedButton(
            onClick = { viewModel.logout(onLoggedOut) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Abmelden")
        }
    }
}
