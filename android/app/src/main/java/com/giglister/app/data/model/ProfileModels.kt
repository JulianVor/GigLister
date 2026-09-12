package com.giglister.app.data.model

import kotlinx.serialization.Serializable

// Mirrors dto/MeResponse.java and dto/ProfileUpdateRequest.java.

@Serializable
data class MeResponse(
    val id: Long,
    val email: String,
    val username: String,
    val homeCity: String? = null,
    val homeLatitude: Double? = null,
    val homeLongitude: Double? = null,
    val radiusKm: Int? = null,
    val platformAdmin: Boolean = false
)

/** Every field is optional and only the ones set here get changed - the backend leaves
 * anything sent as null untouched rather than clearing it (see UserService.updateProfile),
 * so a call that's only updating the radius, say, can't accidentally wipe out a saved
 * home city/position. */
@Serializable
data class ProfileUpdateRequest(
    val homeCity: String? = null,
    val homeLatitude: Double? = null,
    val homeLongitude: Double? = null,
    val radiusKm: Int? = null
)

@Serializable
data class DeviceTokenRequest(val token: String)
