package com.dispensa.app.pomodoro

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.pomodoroStore by preferencesDataStore("pomodoro")

/**
 * Single, app-wide Pomodoro timer.
 *
 * Persistent state lives in DataStore so the timer survives process death
 * and the AlarmManager fires the end-of-phase notification even if the
 * user has switched apps or locked the screen — that's the whole point
 * for an ADHD-style "I want to study, then forget about the app".
 *
 * Phases: focus (study) and rest (break). When focus ends we auto-arm a
 * rest phase. When rest ends we go idle.
 */
class PomodoroController(private val context: Context) {

    enum class Phase { Idle, Focus, Rest }

    data class Snapshot(
        val phase: Phase = Phase.Idle,
        val endAtMillis: Long = 0,
        val durationSec: Int = 0,
        val topicTitle: String = "",
    )

    val snapshot: Flow<Snapshot> = context.pomodoroStore.data.map { p ->
        Snapshot(
            phase = runCatching { Phase.valueOf(p[KEY_PHASE] ?: "Idle") }.getOrDefault(Phase.Idle),
            endAtMillis = p[KEY_END_AT] ?: 0L,
            durationSec = p[KEY_DURATION] ?: 0,
            topicTitle = p[KEY_TOPIC_TITLE].orEmpty(),
        )
    }

    suspend fun startFocus(minutes: Int, topicTitle: String) {
        val durationSec = minutes * 60
        val end = System.currentTimeMillis() + durationSec * 1000L
        persist(Phase.Focus, end, durationSec, topicTitle)
        scheduleAlarmAt(end, Phase.Focus, topicTitle)
    }

    suspend fun startRest(minutes: Int = 5, topicTitle: String) {
        val durationSec = minutes * 60
        val end = System.currentTimeMillis() + durationSec * 1000L
        persist(Phase.Rest, end, durationSec, topicTitle)
        scheduleAlarmAt(end, Phase.Rest, topicTitle)
    }

    suspend fun cancel() {
        cancelAlarm()
        persist(Phase.Idle, 0, 0, "")
    }

    private suspend fun persist(phase: Phase, endAt: Long, durationSec: Int, topicTitle: String) {
        context.pomodoroStore.edit { p ->
            p[KEY_PHASE] = phase.name
            p[KEY_END_AT] = endAt
            p[KEY_DURATION] = durationSec
            p[KEY_TOPIC_TITLE] = topicTitle
        }
    }

    private fun scheduleAlarmAt(triggerAt: Long, phase: Phase, topicTitle: String) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, PomodoroAlarmReceiver::class.java).apply {
            putExtra(EXTRA_PHASE, phase.name)
            putExtra(EXTRA_TOPIC_TITLE, topicTitle)
        }
        val pi = PendingIntent.getBroadcast(
            context, ALARM_REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun cancelAlarm() {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, PomodoroAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, ALARM_REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.cancel(pi)
    }

    companion object {
        private val KEY_PHASE = stringPreferencesKey("phase")
        private val KEY_END_AT = longPreferencesKey("end_at")
        private val KEY_DURATION = intPreferencesKey("duration_sec")
        private val KEY_TOPIC_TITLE = stringPreferencesKey("topic_title")
        const val EXTRA_PHASE = "phase"
        const val EXTRA_TOPIC_TITLE = "topic_title"
        private const val ALARM_REQUEST = 4221
    }
}
