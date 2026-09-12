package com.giglister.app.push

import com.giglister.app.data.api.ApiClient
import com.giglister.app.data.api.TokenStore
import com.giglister.app.data.model.DeviceTokenRequest
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GigListerMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** Fires whenever FCM (re)issues this device a token - on first install, and
     * occasionally afterwards (token rotation is normal, not an error). Only registers it
     * if someone's actually logged in right now; LoginViewModel also registers the
     * then-current token right after a successful login, covering the case where this
     * fired before anyone was signed in. */
    override fun onNewToken(token: String) {
        val userToken = TokenStore.token ?: return
        scope.launch {
            runCatching { ApiClient.api.registerDeviceToken(DeviceTokenRequest(token)) }
        }
    }

    /** Only reached for a "notification" message while the app is in the foreground -
     * Android shows it automatically from the system tray otherwise, using the default
     * channel declared in AndroidManifest (see PushNotificationService.java on the
     * backend, which sends title+body as a plain FCM Notification payload). */
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: return
        val body = message.notification?.body ?: ""
        NotificationHelper.show(applicationContext, title, body)
    }
}
