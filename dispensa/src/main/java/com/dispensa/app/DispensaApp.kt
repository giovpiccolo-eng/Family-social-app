package com.dispensa.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.dispensa.app.di.Graph

class DispensaApp : Application() {
    lateinit var graph: Graph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = Graph(this)
        ensurePomodoroChannel()
    }

    private fun ensurePomodoroChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(POMODORO_CHANNEL) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                POMODORO_CHANNEL,
                getString(R.string.pomodoro_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.pomodoro_channel_desc)
                enableVibration(true)
            }
        )
    }

    companion object {
        const val POMODORO_CHANNEL = "pomodoro_alerts"
    }
}
