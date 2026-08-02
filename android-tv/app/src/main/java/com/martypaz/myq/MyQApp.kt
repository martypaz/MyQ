package com.martypaz.myq

import android.app.Application
import com.martypaz.myq.data.epg.EpgRepository
import com.martypaz.myq.data.epg.FreeviewEpgApi
import com.martypaz.myq.data.epg.FreeviewEpgSource
import com.martypaz.myq.data.epg.TvMazeApi
import com.martypaz.myq.data.epg.TvMazeSource
import com.martypaz.myq.data.freeview.FreeviewApi
import com.martypaz.myq.data.freeview.FreeviewSource
import com.martypaz.myq.data.tv.DeviceTvSource
import com.martypaz.myq.data.tv.TvLineup
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

    val freeviewApi by lazy { FreeviewApi() }
    val tvLineup by lazy { TvLineup(this) }

    /**
     * The transmitter region, mirrored out of the profile so the listings
     * source can read it without suspending on every request.
     */
    
    var networkId: Int? = null
        private set

    val epgRepository by lazy {
        EpgRepository(
            listOf(
                FreeviewSource(freeviewApi) { networkId },
                FreeviewEpgSource(FreeviewEpgApi(cacheDir = cacheDir)),
                DeviceTvSource(this, tvLineup),
                TvMazeSource(TvMazeApi()),
            ),
        )
    }

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
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch { rearmReminders(this@MyQApp) }
        scope.launch { profileStore.profile.collect { networkId = it.networkId } }
    }
}
