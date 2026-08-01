package com.martypaz.freeviewguide

import android.app.Application
import com.martypaz.freeviewguide.data.epg.EpgRepository
import com.martypaz.freeviewguide.data.prefs.ReminderStore
import com.martypaz.freeviewguide.data.prefs.TasteStore
import com.martypaz.freeviewguide.recs.Recommender
import com.martypaz.freeviewguide.reminders.ReminderScheduler

/** Application-scoped service container (deliberately framework-free DI). */
class FreeviewGuideApp : Application() {

    val epgRepository by lazy { EpgRepository() }
    val reminderStore by lazy { ReminderStore(this) }
    val tasteStore by lazy { TasteStore(this) }
    val recommender by lazy { Recommender(tasteStore) }
    val reminderScheduler by lazy { ReminderScheduler(this) }
}
