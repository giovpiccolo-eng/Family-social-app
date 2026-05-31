package com.familynest.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.familynest.app.MainActivity
import com.familynest.app.R

object Notifications {
    private const val CHANNEL_ID = "family_updates"

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Family updates",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "New ideas, plans and photos shared by your family" }
        context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    fun show(context: Context, title: String, body: String, postId: String?) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!postId.isNullOrBlank()) putExtra("postId", postId)
        }
        val pending = PendingIntent.getActivity(
            context,
            postId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        // POST_NOTIFICATIONS is requested at runtime; guard the post regardless.
        val manager = NotificationManagerCompat.from(context)
        if (manager.areNotificationsEnabled()) {
            runCatching { manager.notify(postId?.hashCode() ?: System.currentTimeMillis().toInt(), notification) }
        }
    }
}
