package com.giglister.app.data.model

import kotlinx.serialization.Serializable

// Mirrors dto/common/LocationSummary.java, BandSummary.java and dto/event/EventResponse.java.
// date/startTime stay plain Strings (ISO "yyyy-MM-dd" / "HH:mm:ss", exactly as Jackson
// serializes java.time types on the backend) rather than java.time.* types - simplest
// thing that works for just displaying them, no date-time library needed for v1.

@Serializable
data class LocationSummary(
    val id: Long,
    val name: String,
    val city: String,
    val status: String,
    val titleImageUrl: String? = null,
    val linkable: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class BandSummary(
    val id: Long,
    val name: String,
    val city: String? = null,
    val status: String,
    val logoUrl: String? = null,
    val titleImageUrl: String? = null,
    val linkable: Boolean = false,
    val genres: List<String> = emptyList()
)

@Serializable
data class EventResponse(
    val id: Long,
    val title: String? = null,
    val date: String,
    val startTime: String? = null,
    val location: LocationSummary,
    val bands: List<BandSummary> = emptyList(),
    val description: String? = null,
    val ticketUrl: String? = null,
    val titleImageUrl: String? = null,
    val bandImageDisplay: String,
    val status: String,
    val createdBy: Long? = null
) {
    /** e.g. "Arsen + Fahrtenbuch" - same fallback title Konzerte uses on the web when an
     * event has no explicit title of its own (event.title stays null for almost every
     * event in practice, see eventLineupLabel on the web side). */
    val displayTitle: String
        get() = title ?: bands.joinToString(" + ") { it.name }.ifBlank { location.name }
}

/** Just enough of Spring Data's Page<T> JSON shape to read a result list - the response
 * actually carries several more fields (pageable, sort, first/last, ...) that nothing
 * here needs, which is why the Json parser building GigListerApi must have
 * ignoreUnknownKeys = true (see ApiClient). */
@Serializable
data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val totalElements: Long = 0,
    val totalPages: Int = 0
)
