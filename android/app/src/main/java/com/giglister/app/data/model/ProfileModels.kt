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
    val platformAdmin: Boolean = false,
    val mustChangePassword: Boolean = false,
    val preferredGenres: List<String> = emptyList(),
    val savedActs: List<SavedAct> = emptyList(),
    val savedEvents: List<EventResponse> = emptyList(),
    val followedBands: List<FollowedBand> = emptyList(),
    val managedEntities: List<ManagedEntity> = emptyList()
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
    val radiusKm: Int? = null,
    val preferredGenres: List<String>? = null
)

@Serializable
data class DeviceTokenRequest(val token: String)

@Serializable
data class FollowedBand(val id: Long, val name: String, val nextEventDate: String? = null, val logoUrl: String? = null)

@Serializable
data class ManagedEntity(val entityType: String, val entityId: Long, val name: String, val permission: String)

fun MeResponse?.canEdit(kind: String, id: Long): Boolean =
    this?.managedEntities?.any { it.entityType == kind && it.entityId == id && it.permission in listOf("EDIT", "MANAGE") } == true

fun MeResponse?.canManage(kind: String, id: Long): Boolean =
    this?.managedEntities?.any { it.entityType == kind && it.entityId == id && it.permission == "MANAGE" } == true

fun MeResponse?.canEdit(event: EventResponse): Boolean = this != null &&
    (id == event.createdBy || canEdit("LOCATION", event.location.id) || event.bands.any { canEdit("BAND", it.id) })
