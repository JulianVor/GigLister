package com.giglister.app.data.api

/** The current login token, held in memory for the OkHttp interceptor below to read
 * synchronously (interceptors run on a background thread but can't suspend, so they
 * can't read DataStore directly) - AuthRepository is what keeps this in sync with the
 * persisted copy on disk, including restoring it once at app startup. */
object TokenStore {
    @Volatile
    var token: String? = null
}
