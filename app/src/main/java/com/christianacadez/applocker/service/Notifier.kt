package com.christianacadez.applocker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.christianacadez.applocker.R

object Notifier {

    private const val CHANNEL_ID = "limit_warnings"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Avisos de límite",
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)
    }

    fun warn(context: Context, packageName: String, appName: String, minutesLeft: Int) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val text = if (minutesLeft <= 1) {
            "Queda 1 minuto de $appName"
        } else {
            "Quedan $minutesLeft minutos de $appName"
        }
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_lock)
            .setContentTitle("Límite por agotarse")
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        manager.notify(packageName.hashCode(), notification)
    }
}