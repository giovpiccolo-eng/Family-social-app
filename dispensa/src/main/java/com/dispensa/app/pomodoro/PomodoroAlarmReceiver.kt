package com.dispensa.app.pomodoro

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dispensa.app.DispensaApp
import com.dispensa.app.MainActivity
import com.dispensa.app.R

class PomodoroAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val phase = intent.getStringExtra(PomodoroController.EXTRA_PHASE) ?: return
        val topic = intent.getStringExtra(PomodoroController.EXTRA_TOPIC_TITLE).orEmpty()

        val (titleResId, defaultText) = when (phase) {
            PomodoroController.Phase.Focus.name -> R.string.pomodoro to context.getString(R.string.pomodoro_focus_done)
            PomodoroController.Phase.Rest.name -> R.string.pomodoro to context.getString(R.string.pomodoro_break_done)
            else -> return
        }

        val text = if (topic.isNotBlank()) "$defaultText\n$topic" else defaultText

        val pi = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val n = NotificationCompat.Builder(context, DispensaApp.POMODORO_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(titleResId))
            .setContentText(defaultText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .build()

        val nm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            context.getSystemService(NotificationManager::class.java)
        else
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID, n)
    }

    companion object {
        private const val NOTIFICATION_ID = 4221
    }
}
