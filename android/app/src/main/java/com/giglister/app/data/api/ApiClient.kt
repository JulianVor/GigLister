package com.giglister.app.data.api

import com.giglister.app.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Builds the single Retrofit instance the whole app shares - no DI framework, just a
 * plain object, since there's only ever one backend to talk to. */
object ApiClient {

    private val json = Json {
        // The backend's DTOs (and Spring Data's Page<T> wrapper especially) carry plenty
        // of fields no screen here needs - without this, an unmodeled field would fail
        // the whole response instead of just being ignored.
        ignoreUnknownKeys = true
        // In particular PHOTO must be sent on updates; omission has a different server default.
        encodeDefaults = true
    }

    private val authInterceptor = Interceptor { chain: Interceptor.Chain ->
        val request = chain.request().newBuilder().apply {
            TokenStore.token?.let { header("Authorization", "Bearer $it") }
        }.build()
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            // Credentials, reset tokens and private profile data must never enter Logcat.
            redactHeader("Authorization")
            level = HttpLoggingInterceptor.Level.NONE
        })
        .build()

    var api: GigListerApi = createApi(BuildConfig.API_BASE_URL)
        internal set

    internal fun createApi(baseUrl: String, readOnly: Boolean = false): GigListerApi = Retrofit.Builder()
        .baseUrl(baseUrl.let { if (it.endsWith("/")) it else "$it/" })
        .client(if (!readOnly) okHttpClient else okHttpClient.newBuilder().addInterceptor { chain ->
            check(chain.request().method == "GET") { "Read-only verification rejects writes" }
            chain.proceed(chain.request())
        }.build())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(GigListerApi::class.java)
}
