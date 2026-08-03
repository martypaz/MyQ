package com.martypaz.myq.reminders

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Whether this device will actually let a reminder reach the viewer.
 *
 * Three separate permissions sit between an alarm firing and someone seeing
 * it, and each fails silently on its own: a denied notification permission
 * makes the post a no-op, and with it the full-screen alert that hangs off it;
 * Android 14 revokes full-screen intents for anything that is not a dialler or
 * alarm clock, leaving a notification a television files out of sight.
 *
 * Nothing throws. The only way to know is to ask.
 */
data class ReminderReadiness(
    val notificationsEnabled: Boolean,
    val fullScreenAlertsAllowed: Boolean,
    val exactAlarmsAllowed: Boolean,
) {
    val isReady: Boolean get() = notificationsEnabled && fullScreenAlertsAllowed

    /** What to tell the viewer, worst first. */
    val blockers: List<String>
        get() = buildList {
            if (!notificationsEnabled) {
                add("Notifications are turned off, so nothing can be shown at all.")
            }
            if (!fullScreenAlertsAllowed) {
                add("Full-screen alerts are not allowed, so reminders will not appear over what you are watching.")
            }
            if (!exactAlarmsAllowed) {
                add("Exact alarms are not allowed. Reminders still fire, timed as an alarm clock rather than a plain alarm.")
            }
        }
}

fun checkReminderReadiness(context: Context): ReminderReadiness {
    val notifications = NotificationManagerCompat.from(context)
    val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    return ReminderReadiness(
        notificationsEnabled = notifications.areNotificationsEnabled(),
        fullScreenAlertsAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.getSystemService(android.app.NotificationManager::class.java)
                ?.canUseFullScreenIntent() ?: false
        } else {
            true
        },
        exactAlarmsAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarms.canScheduleExactAlarms()
        } else {
            true
        },
    )
}

/** Opens the system page for whichever permission is missing. */
fun Context.openNotificationSettings() = startSettings(
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
)

fun Context.openFullScreenAlertSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
    startSettings(
        Intent(
            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
            Uri.parse("package:$packageName"),
        ),
    )
}

fun Context.openExactAlarmSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    startSettings(
        Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:$packageName"),
        ),
    )
}

/**
 * Televisions do not always carry the settings screens a phone does, so a
 * missing one must not take the app down with it.
 */
private fun Context.startSettings(intent: Intent): Boolean {
    val launchable = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (packageManager.resolveActivity(launchable, 0) == null) return false
    return runCatching { startActivity(launchable) }.isSuccess
}
