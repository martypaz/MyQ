package com.martypaz.myq.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.martypaz.myq.data.model.Reminder

/**
 * Schedules one exact alarm per reminder via AlarmManager, so notifications
 * fire even when the app isn't running. Alarms don't survive reboot, so
 * [BootReceiver] calls [scheduleAll] to re-register everything.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: Reminder) {
        val fireAt = reminder.fireAtMillis
        if (fireAt <= System.currentTimeMillis()) return

        val pending = pendingIntentFor(reminder.programmeId) {
            ReminderAlert.from(reminder).writeTo(this)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pending)
        } else {
            // Exact alarms denied by the user: an inexact alarm still lands close enough.
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, fireAt, 10L * 60L * 1000L, pending)
        }
    }

    /**
     * Arms a reminder a few seconds out through the ordinary path — real
     * alarm, real receiver, real full-screen intent — so the whole chain can
     * be seen working without waiting for a programme. Not persisted, so it
     * never appears in the guide.
     */
    fun scheduleTest(inMillis: Long = TEST_DELAY_MILLIS): Reminder {
        val reminder = Reminder(
            programmeId = "myq-test-reminder",
            title = "Test reminder",
            channelName = "MyQ",
            startMillis = System.currentTimeMillis() + inMillis + ONE_HOUR_MILLIS,
            leadMinutes = Reminder.MINUTES_PER_HOUR,
        )
        schedule(reminder)
        return reminder
    }

    fun cancel(programmeId: String) {
        alarmManager.cancel(pendingIntentFor(programmeId) {})
    }

    fun scheduleAll(reminders: List<Reminder>) = reminders.forEach(::schedule)

    private fun pendingIntentFor(programmeId: String, configure: Intent.() -> Unit): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SHOW
            data = android.net.Uri.parse("myq://reminder/$programmeId")
            configure()
        }
        return PendingIntent.getBroadcast(
            context,
            programmeId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val TEST_DELAY_MILLIS = 5_000L
        const val ONE_HOUR_MILLIS = 60L * 60L * 1000L
    }
}
