package com.martypaz.myq.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.martypaz.myq.data.prefs.ReminderStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-registers pending reminder alarms whenever the system has thrown them
 * away: on reboot, and on app update. AlarmManager keeps nothing across
 * either, and an update sends no BOOT_COMPLETED, so without the second filter
 * every reminder set before an update would quietly never fire.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in REARM_ACTIONS) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                rearmReminders(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val REARM_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            // Some manufacturers send only their own variant.
            "android.intent.action.QUICKBOOT_POWERON",
        )
    }
}

/**
 * Drops reminders whose programme has already started, then re-arms the rest.
 *
 * Safe to call repeatedly: scheduling the same reminder twice updates the one
 * alarm rather than adding another, because the PendingIntent is keyed on the
 * programme id.
 */
suspend fun rearmReminders(context: Context) {
    val store = ReminderStore(context)
    store.prune()
    ReminderScheduler(context).scheduleAll(store.current())
}
