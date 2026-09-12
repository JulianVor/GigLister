package com.giglister.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

/** A single real GPS fix for "Konzerte in deiner Nähe" - not a continuous location
 * subscription, since the list screen only ever needs "where am I right now" once per
 * visit/refresh, not a live-updating position. */
class LocationRepository(context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    /** Caller must have already been granted ACCESS_FINE_LOCATION or
     * ACCESS_COARSE_LOCATION (see MainActivity's permission request) - returns null if
     * the fix times out, Play Services is unavailable, or the request otherwise fails,
     * rather than throwing, since "no location yet" is an entirely normal, recoverable
     * state for the events screen to show a retry prompt for. */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .build()
        return try {
            client.getCurrentLocation(request, CancellationTokenSource().token).await()
        } catch (e: Exception) {
            null
        }
    }
}
