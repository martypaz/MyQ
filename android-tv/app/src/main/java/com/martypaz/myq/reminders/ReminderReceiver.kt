package com.martypaz.myq.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.martypaz.myq.MainActivity
import com.martypaz.myq.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Fires when a reminder alarm goes off and raises the notification. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SHOW) return

        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val channelName = intent.getStringExtra(EXTRA_CHANNEL) ?: ""
        val startMillis = intent.getLongExtra(EXTRA_START_MILLIS, 0L)
        val leadHours = intent.getIntExtra(EXTRA_LEAD_HOURS, 1)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Programme reminders",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Alerts before a programme you flagged starts" },
        )

        val startTime = DateTimeFormatter.ofPattern("EEE HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(startMillis))

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val hoursLabel = if (leadHours == 1) "in 1 hour" else "in $leadHours hours"
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$title starts $hoursLabel")
            .setContentText("$channelName · $startTime")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        manager.notify(title.hashCode(), notification)
    }

    companion object {
        const val ACTION_SHOW = "com.martypaz.myq.SHOW_REMINDER"
        const val EXTRA_TITLE = "title"
        const val EXTRA_CHANNEL = "channel"
        const val EXTRA_START_MILLIS = "startMillis"
        const val EXTRA_LEAD_HOURS = "leadHours"
        const val NOTIFICATION_CHANNEL_ID = "programme_reminders"
    }
}
