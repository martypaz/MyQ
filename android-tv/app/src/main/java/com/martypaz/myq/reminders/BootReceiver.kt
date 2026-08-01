package com.martypaz.myq.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.martypaz.myq.data.prefs.ReminderStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Re-registers all pending reminder alarms after a device reboot. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val store = ReminderStore(context.applicationContext)
                store.prune()
                ReminderScheduler(context.applicationContext).scheduleAll(store.current())
            } finally {
                pendingResult.finish()
            }
        }
    }
}
