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

/**
 * Fires when a reminder alarm goes off.
 *
 * Posts a notification *and* attaches a full-screen intent. On a television
 * the notification alone is close to invisible — Android TV files it in the
 * launcher's notification row rather than showing it over the picture — so
 * [ReminderActivity] is what actually reaches a viewer mid-programme. The
 * notification remains the fallback for when the system declines the
 * full-screen intent, which Android 14 does unless the user has granted it.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SHOW) return
        val alert = ReminderAlert.from(intent) ?: return

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
            .format(Instant.ofEpochMilli(alert.startMillis))

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${alert.title} starts ${alert.leadLabel()}")
            .setContentText("${alert.channelName} · $startTime")
            .setContentIntent(context.openAppIntent(alert))
            .setFullScreenIntent(context.fullScreenIntent(alert), true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        manager.notify(alert.notificationId, notification)
    }

    /** Selecting the notification itself just opens MyQ. */
    private fun Context.openAppIntent(alert: ReminderAlert): PendingIntent =
        PendingIntent.getActivity(
            this,
            alert.notificationId,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun Context.fullScreenIntent(alert: ReminderAlert): PendingIntent =
        PendingIntent.getActivity(
            this,
            alert.notificationId,
            reminderAlertIntent(alert, ReminderActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val ACTION_SHOW = "com.martypaz.myq.SHOW_REMINDER"
        const val NOTIFICATION_CHANNEL_ID = "programme_reminders"
    }
}
