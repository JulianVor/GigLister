package com.giglister.app.data.model

import kotlinx.serialization.Serializable

// Mirrors the backend's dto/auth/* records exactly (see LoginRequest.java,
// AuthResponse.java, RegisterRequest.java) - field names/casing must match the JSON the
// Spring Boot API actually sends/expects.

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class RegisterRequest(val email: String, val password: String, val username: String)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: Long,
    val email: String,
    val username: String,
    val platformAdmin: Boolean
)

@Serializable
data class RegisterResponse(val userId: Long, val email: String, val message: String)
