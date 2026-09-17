package com.giglister.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable data class FestivalSummary(val id: Long, val name: String, val titleImageUrl: String? = null, val ticketUrl: String? = null)
@Serializable data class FestivalResponse(val id: Long, val name: String, val description: String? = null, val titleImageUrl: String? = null, val ticketUrl: String? = null, val timetableStyle: String = "LIST", val events: List<EventResponse> = emptyList())
@Serializable data class FestivalInput(val name: String = "", val description: String? = null, val titleImageUrl: String? = null, val ticketUrl: String? = null, val timetableStyle: String = "LIST")
@Serializable data class EventCreateResult(val published: Boolean, val event: EventResponse? = null, val submission: SubmissionResponse? = null)
@Serializable data class SubmissionResponse(val id: Long, val status: String, val type: String = "EVENT", val payload: JsonObject = JsonObject(emptyMap()), val rejectionReason: String? = null, val resultEntityId: Long? = null, val submittedAt: String? = null)
@Serializable data class SavedAct(val eventId: Long, val bandId: Long)
@Serializable data class GenreFilterOption(val genre: String, val eventCount: Int)
@Serializable data class ChangePasswordInput(val currentPassword: String, val newPassword: String)

/** Treat an act before the concert's start as taking place after midnight. */
fun runningMinutes(time: String?, reference: String?): Int {
    if (time == null) return -1
    val minutes = time.take(2).toInt() * 60 + time.substring(3, 5).toInt()
    return minutes + if (reference != null && time.take(5) < reference.take(5)) 1440 else 0
}
fun EventResponse.orderedBands() = bands.sortedBy { runningMinutes(it.startTime, startTime) }
fun EventResponse.onlySavedActs(acts: List<SavedAct>): EventResponse {
    val ids = acts.filter { it.eventId == id }.map { it.bandId }.toSet()
    return if (ids.isEmpty()) this else copy(bands = bands.filter { it.id in ids })
}
fun upcomingSaved(me: MeResponse, today: String): List<EventResponse> {
    val festivals = mutableSetOf<Long>()
    return me.savedEvents.filter { it.date >= today }.sortedBy { it.date }
        .filter { it.eventSeries?.let { festival -> festivals.add(festival.id) } ?: true }
}
