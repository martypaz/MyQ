package com.martypaz.myq.reminders

import android.content.Context
import android.content.Intent
import com.martypaz.myq.data.model.Reminder

/**
 * The payload a fired reminder carries from the alarm through to whatever
 * shows it. Kept in one place because it crosses three process entry points —
 * the alarm's PendingIntent, the broadcast receiver, and the full-screen
 * activity — and a key that disagrees between any two of them fails silently.
 */
data class ReminderAlert(
    val programmeId: String,
    val title: String,
    val channelName: String,
    val startMillis: Long,
    val leadHours: Int,
) {
    /** Stable per programme, so a re-fire replaces rather than stacks. */
    val notificationId: Int get() = programmeId.hashCode()

    fun writeTo(intent: Intent): Intent = intent.apply {
        putExtra(EXTRA_PROGRAMME_ID, programmeId)
        putExtra(EXTRA_TITLE, title)
        putExtra(EXTRA_CHANNEL, channelName)
        putExtra(EXTRA_START_MILLIS, startMillis)
        putExtra(EXTRA_LEAD_HOURS, leadHours)
    }

    companion object {
        const val EXTRA_PROGRAMME_ID = "programmeId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_CHANNEL = "channel"
        const val EXTRA_START_MILLIS = "startMillis"
        const val EXTRA_LEAD_HOURS = "leadHours"

        fun from(reminder: Reminder) = ReminderAlert(
            programmeId = reminder.programmeId,
            title = reminder.title,
            channelName = reminder.channelName,
            startMillis = reminder.startMillis,
            leadHours = reminder.leadHours,
        )

        /** Null when the intent is missing the one field we cannot invent. */
        fun from(intent: Intent): ReminderAlert? {
            val title = intent.getStringExtra(EXTRA_TITLE) ?: return null
            return ReminderAlert(
                programmeId = intent.getStringExtra(EXTRA_PROGRAMME_ID) ?: title,
                title = title,
                channelName = intent.getStringExtra(EXTRA_CHANNEL).orEmpty(),
                startMillis = intent.getLongExtra(EXTRA_START_MILLIS, 0L),
                leadHours = intent.getIntExtra(EXTRA_LEAD_HOURS, 1),
            )
        }
    }
}

/** "in 1 hour" / "in 4 hours" — how far ahead of the programme we are. */
fun ReminderAlert.leadLabel(): String =
    if (leadHours == 1) "in 1 hour" else "in $leadHours hours"

internal fun Context.reminderAlertIntent(alert: ReminderAlert, target: Class<*>): Intent =
    alert.writeTo(Intent(this, target))
