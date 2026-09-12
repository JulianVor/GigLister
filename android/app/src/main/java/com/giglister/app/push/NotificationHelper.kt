package com.giglister.app.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.giglister.app.MainActivity
import com.giglister.app.R
import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager

object NotificationHelper {

    // Read from the same string resource AndroidManifest.xml's default_notification_channel_id
    // meta-data points at (a plain `const val` couldn't - it needs a Context to resolve),
    // so the channel this creates and the one FCM's default-channel setting expects can
    // never drift apart into two different ids.
    private fun channelId(context: Context) = context.getString(R.string.notification_channel_id)

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            channelId(context),
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** No-ops if the user never granted POST_NOTIFICATIONS (Android 13+) - a missed push
     * is a far smaller problem than crashing on a SecurityException. */
    fun show(context: Context, title: String, body: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId(context))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }
}
