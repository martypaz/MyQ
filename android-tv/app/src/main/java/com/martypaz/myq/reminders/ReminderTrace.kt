package com.martypaz.myq.reminders

import android.content.Context
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Breadcrumbs from the last reminder, so a failure can be located rather than
 * guessed at.
 *
 * The chain has three links — the alarm fires, the notification posts, the
 * full-screen alert appears — and every one of them fails silently. Knowing
 * that the alarm fired but the alert never showed points at the full-screen
 * permission; knowing the alarm never fired points somewhere else entirely.
 *
 * Deliberately SharedPreferences rather than DataStore: a broadcast receiver
 * has no coroutine scope to wait on, and a diagnostic that needs its own
 * async plumbing is one more thing that can fail.
 */
object ReminderTrace {

    fun recordAlarmFired(context: Context, at: Long = System.currentTimeMillis()) {
        prefs(context).edit().putLong(KEY_ALARM, at).apply()
    }

    fun recordAlertShown(context: Context, at: Long = System.currentTimeMillis()) {
        prefs(context).edit().putLong(KEY_ALERT, at).apply()
    }

    fun read(context: Context): Trace = prefs(context).let {
        Trace(
            alarmFiredAt = it.getLong(KEY_ALARM, 0L).takeIf { at -> at > 0L },
            alertShownAt = it.getLong(KEY_ALERT, 0L).takeIf { at -> at > 0L },
        )
    }

    data class Trace(val alarmFiredAt: Long?, val alertShownAt: Long?) {
        /** Reads as a sentence on the developer screen. */
        fun describe(): String = when {
            alarmFiredAt == null -> "No alarm has fired yet."
            alertShownAt == null ->
                "Alarm fired at ${format(alarmFiredAt)}, but the alert never appeared — " +
                    "that points at the full-screen permission, not at the alarm."
            alertShownAt < alarmFiredAt ->
                "Alarm fired at ${format(alarmFiredAt)}; the last alert was earlier, at " +
                    "${format(alertShownAt)}. The alarm is arriving but the alert is not."
            else -> "Alarm fired at ${format(alarmFiredAt)} and the alert appeared at ${format(alertShownAt)}."
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences("myq_reminder_trace", Context.MODE_PRIVATE)

    private fun format(millis: Long): String = TIME.format(Instant.ofEpochMilli(millis))

    private val TIME: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

    private const val KEY_ALARM = "alarm_fired_at"
    private const val KEY_ALERT = "alert_shown_at"
}
