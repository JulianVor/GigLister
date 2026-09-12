package com.giglister.app

import android.app.Application
import com.giglister.app.data.AuthRepository
import com.giglister.app.push.NotificationHelper
import kotlinx.coroutines.runBlocking

class GigListerApp : Application() {

    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        authRepository = AuthRepository(this)
        NotificationHelper.createChannel(this)
        // A one-time, local-disk read at startup - simple and fast enough to do
        // synchronously here rather than adding a loading state to the nav graph just
        // for this. Every screen after this point can just check authRepository.isLoggedIn.
        runBlocking { authRepository.restoreSession() }
    }
}
