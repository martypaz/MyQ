package com.martypaz.myq

import android.app.Application
import com.martypaz.myq.data.epg.EpgRepository
import com.martypaz.myq.data.prefs.ProfileStore
import com.martypaz.myq.data.prefs.RecordingStore
import com.martypaz.myq.data.prefs.ReminderStore
import com.martypaz.myq.data.prefs.TasteStore
import com.martypaz.myq.recs.Recommender
import com.martypaz.myq.reminders.ReminderScheduler
import com.martypaz.myq.reminders.rearmReminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Application-scoped service container (deliberately framework-free DI). */
class MyQApp : Application() {

    val epgRepository by lazy { EpgRepository() }
    val reminderStore by lazy { ReminderStore(this) }
    val tasteStore by lazy { TasteStore(this) }
    val recordingStore by lazy { RecordingStore(this) }
    val profileStore by lazy { ProfileStore(this) }
    val recommender by lazy { Recommender(tasteStore) }
    val reminderScheduler by lazy { ReminderScheduler(this) }

    override fun onCreate() {
        super.onCreate()
        // A force-stop clears every alarm and suppresses BOOT_COMPLETED until
        // the app is opened again, so the receiver alone cannot guarantee a
        // reminder survives. Re-arming on each start closes that gap; it is
        // idempotent, so the cost is a few PendingIntent updates.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            rearmReminders(this@MyQApp)
        }
    }
}
