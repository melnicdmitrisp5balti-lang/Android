package com.parentalcontrol.app.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.parentalcontrol.app.R
import com.parentalcontrol.app.ui.main.MainActivity

object NotificationHelper {

    const val MONITORING_CHANNEL_ID = "monitoring_channel"
    const val MONITORING_NOTIFICATION_ID = 1001

    fun createNotificationChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            MONITORING_CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    fun buildMonitoringNotification(context: Context, isCamera: Boolean, isAudio: Boolean): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val activeItems = buildList {
            if (isCamera) add(context.getString(R.string.camera))
            if (isAudio) add(context.getString(R.string.microphone))
        }
        val contentText = context.getString(
            R.string.notification_monitoring_active,
            activeItems.joinToString(", ")
        )

        return NotificationCompat.Builder(context, MONITORING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_monitoring)
            .setContentTitle(context.getString(R.string.notification_monitoring_title))
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
