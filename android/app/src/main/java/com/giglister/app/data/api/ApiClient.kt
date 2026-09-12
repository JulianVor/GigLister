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
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    val api: GigListerApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL.let { if (it.endsWith("/")) it else "$it/" })
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(GigListerApi::class.java)
}
