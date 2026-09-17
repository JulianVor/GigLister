package com.giglister.app.data.model

import kotlinx.serialization.Serializable

/** Band and venue detail responses share these fields; absent fields keep defaults. */
@Serializable
data class EntityDetails(
    val id: Long, val name: String, val city: String? = null,
    val country: String? = null, val shortDescription: String? = null,
    val website: String? = null, val logoUrl: String? = null, val titleImageUrl: String? = null,
    val profileImageUrl: String? = null,
    val genres: List<String> = emptyList(), val status: String = "PUBLISHED", val unclaimed: Boolean = false,
    val address: String? = null, val postalCode: String? = null,
    val latitude: Double? = null, val longitude: Double? = null,
    val upcomingEvents: List<EventResponse> = emptyList(),
    val pastEventsByYear: Map<String, List<EventResponse>> = emptyMap()
)

@Serializable
data class LocationListItem(val id: Long, val name: String, val city: String, val upcomingEventCount: Int = 0)
@Serializable
data class SearchResults(val events: List<EventResponse> = emptyList(), val bands: List<EntityDetails> = emptyList(), val locations: List<LocationListItem> = emptyList())
@Serializable
data class DiscoverResponse(
    val todayNearby: List<EventResponse> = emptyList(), val thisWeekend: List<EventResponse> = emptyList(),
    val newEvents: List<EventResponse> = emptyList(), val locationsWithUpcomingShows: List<LocationListItem> = emptyList(),
    val bandsPlayingSoon: List<EntityDetails> = emptyList(),
    val recommendedForYou: List<EventResponse> = emptyList()
)
@Serializable
data class CalendarDayCount(val date: String, val count: Int)
@Serializable
data class EntityRef(val id: Long? = null, val name: String? = null, val city: String? = null, val address: String? = null, val postalCode: String? = null, val startTime: String? = null)
@Serializable
data class EventInput(
    val date: String, val location: EntityRef, val bands: List<EntityRef>,
    val title: String? = null, val startTime: String? = null, val description: String? = null,
    val ticketUrl: String? = null, val titleImageUrl: String? = null, val bandImageDisplay: String = "PHOTO",
    val eventSeriesId: Long? = null
)
@Serializable
data class BandInput(val name: String, val city: String?, val country: String?, val shortDescription: String?, val website: String?, val logoUrl: String?, val titleImageUrl: String?, val genres: List<String>, val profileImageUrl: String? = null)
@Serializable
data class LocationInput(val name: String, val city: String, val address: String?, val postalCode: String?, val country: String?, val website: String?, val logoUrl: String?, val titleImageUrl: String?, val latitude: Double?, val longitude: Double?)
@Serializable
data class StatusInput(val status: String)
@Serializable
data class ClaimInput(val message: String)
@Serializable
data class PermissionInput(val userId: Long, val permission: String)
@Serializable
data class PermissionResponse(val userId: Long, val username: String, val email: String, val permission: String)
@Serializable
data class DuplicateCandidate(val id: Long, val name: String, val city: String? = null)
@Serializable
data class UploadResponse(val url: String)
@Serializable
data class MessageResponse(val message: String = "")
@Serializable
data class EmailInput(val email: String)
@Serializable
data class TokenInput(val token: String)
@Serializable
data class ResetInput(val token: String, val newPassword: String)
@Serializable
data class UsernameAvailability(val available: Boolean)
