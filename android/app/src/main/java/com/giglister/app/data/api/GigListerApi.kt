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
        @Query("size") size: Int = 50
    ): PageResponse<EventResponse>
}
