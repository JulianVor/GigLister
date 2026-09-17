package com.giglister.app.data.api

import com.giglister.app.data.model.AuthResponse
import com.giglister.app.data.model.DeviceTokenRequest
import com.giglister.app.data.model.EventResponse
import com.giglister.app.data.model.LoginRequest
import com.giglister.app.data.model.MeResponse
import com.giglister.app.data.model.PageResponse
import com.giglister.app.data.model.ProfileUpdateRequest
import com.giglister.app.data.model.RegisterRequest
import com.giglister.app.data.model.RegisterResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
import retrofit2.http.*
import com.giglister.app.data.model.*
import okhttp3.MultipartBody

/** One Retrofit interface for the same REST API the web frontend already talks to
 * (see frontend/src/lib/api.ts for the equivalent web-side calls) - both clients hit the
 * one Spring Boot backend, nothing Android-specific on the server side. */
interface GigListerApi {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @GET("api/me")
    suspend fun me(): MeResponse

    @PUT("api/me")
    suspend fun updateProfile(@Body body: ProfileUpdateRequest): MeResponse

    @POST("api/me/device-token")
    suspend fun registerDeviceToken(@Body body: DeviceTokenRequest)

    // Retrofit's @DELETE has no `hasBody` option of its own - @HTTP is the documented way
    // to send a body on a DELETE (the endpoint needs the token to know which registration
    // to remove, see MeController.unregisterDeviceToken).
    @HTTP(method = "DELETE", path = "api/me/device-token", hasBody = true)
    suspend fun unregisterDeviceToken(@Body body: DeviceTokenRequest)

    /** "Konzerte in deiner Nähe" - same query shape as the web's own radius search
     * (EventController.list), just always lat/lon/radiusKm from the phone's real GPS
     * position rather than a cookie. */
    @GET("api/events")
    suspend fun getEvents(
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Query("radiusKm") radiusKm: Int? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("size") size: Int = 30,
        @Query("city") city: String? = null,
        @Query("page") page: Int = 0
    ): PageResponse<EventResponse>

    @GET("api/events/{id}") suspend fun event(@Path("id") id: Long): EventResponse
    @GET("api/events/calendar") suspend fun calendar(@QueryMap query: Map<String, String>): List<CalendarDayCount>
    @GET("api/events") suspend fun events(@QueryMap query: Map<String, String>): PageResponse<EventResponse>
    @POST("api/events") suspend fun createEvent(@Body input: EventInput): EventCreateResult
    @GET("api/genres") suspend fun genres(): List<String>
    @GET("api/events/genres") suspend fun genreFilters(@QueryMap query: Map<String, String>): List<GenreFilterOption>
    @GET("api/event-series") suspend fun festivals(): List<FestivalSummary>
    @GET("api/event-series/{id}") suspend fun festival(@Path("id") id: Long): FestivalResponse
    @POST("api/event-series") suspend fun createFestival(@Body input: FestivalInput): FestivalResponse
    @PUT("api/event-series/{id}") suspend fun updateFestival(@Path("id") id: Long, @Body input: FestivalInput): FestivalResponse
    @GET("api/me/submissions") suspend fun submissions(): List<SubmissionResponse>
    @PUT("api/me/password") suspend fun changePassword(@Body input: ChangePasswordInput)
    @POST("api/events/{id}/bands/{bandId}/save") suspend fun saveAct(@Path("id") id: Long, @Path("bandId") bandId: Long)
    @DELETE("api/events/{id}/bands/{bandId}/save") suspend fun unsaveAct(@Path("id") id: Long, @Path("bandId") bandId: Long)
    @DELETE("api/{kind}/{id}") suspend fun deleteEntity(@Path("kind") kind: String, @Path("id") id: Long)
    @PUT("api/events/{id}") suspend fun updateEvent(@Path("id") id: Long, @Body input: EventInput): EventResponse
    @PATCH("api/events/{id}/status") suspend fun eventStatus(@Path("id") id: Long, @Body input: StatusInput): EventResponse
    @POST("api/events/{id}/save") suspend fun save(@Path("id") id: Long)
    @DELETE("api/events/{id}/save") suspend fun unsave(@Path("id") id: Long)
    @GET("api/{kind}/{id}") suspend fun entity(@Path("kind") kind: String, @Path("id") id: Long): EntityDetails
    @GET("api/locations") suspend fun locations(@QueryMap query: Map<String, String>): PageResponse<LocationListItem>
    @GET("api/search") suspend fun search(@Query("q") query: String): SearchResults
    @GET("api/discover") suspend fun discover(@QueryMap query: Map<String, String>): DiscoverResponse
    @POST("api/bands/{id}/follow") suspend fun follow(@Path("id") id: Long)
    @DELETE("api/bands/{id}/follow") suspend fun unfollow(@Path("id") id: Long)
    @GET("api/me/bands") suspend fun myBands(): List<EntityDetails>
    @GET("api/me/events") suspend fun myEvents(): List<EventResponse>
    @POST("api/bands") suspend fun createBand(@Body input: BandInput): EntityDetails
    @PUT("api/bands/{id}") suspend fun updateBand(@Path("id") id: Long, @Body input: BandInput): EntityDetails
    @POST("api/locations") suspend fun createLocation(@Body input: LocationInput): EntityDetails
    @PUT("api/locations/{id}") suspend fun updateLocation(@Path("id") id: Long, @Body input: LocationInput): EntityDetails
    @PATCH("api/{kind}/{id}/status") suspend fun entityStatus(@Path("kind") kind: String, @Path("id") id: Long, @Body input: StatusInput): EntityDetails
    @GET("api/{kind}/duplicates") suspend fun duplicates(@Path("kind") kind: String, @Query("name") name: String, @Query("city") city: String?): List<DuplicateCandidate>
    @POST("api/{kind}/{id}/claim") suspend fun claim(@Path("kind") kind: String, @Path("id") id: Long, @Body input: ClaimInput): kotlinx.serialization.json.JsonObject
    @GET("api/{kind}/{id}/permissions") suspend fun permissions(@Path("kind") kind: String, @Path("id") id: Long): List<PermissionResponse>
    @POST("api/{kind}/{id}/permissions") suspend fun grant(@Path("kind") kind: String, @Path("id") id: Long, @Body input: PermissionInput)
    @DELETE("api/{kind}/{id}/permissions/{userId}") suspend fun revoke(@Path("kind") kind: String, @Path("id") id: Long, @Path("userId") userId: Long)
    @Multipart @POST("api/uploads") suspend fun upload(@Part file: MultipartBody.Part): UploadResponse
    @POST("api/auth/forgot-password") suspend fun forgot(@Body input: EmailInput): MessageResponse
    @POST("api/auth/reset-password") suspend fun reset(@Body input: ResetInput): AuthResponse
    @POST("api/auth/verify-email") suspend fun verify(@Body input: TokenInput): AuthResponse
    @GET("api/auth/username-available") suspend fun usernameAvailable(@Query("username") username: String): UsernameAvailability
}
