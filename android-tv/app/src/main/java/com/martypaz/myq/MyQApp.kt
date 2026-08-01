package com.martypaz.myq

import android.app.Application
import com.martypaz.myq.data.epg.EpgRepository
import com.martypaz.myq.data.prefs.ReminderStore
import com.martypaz.myq.data.prefs.TasteStore
import com.martypaz.myq.recs.Recommender
import com.martypaz.myq.reminders.ReminderScheduler

/** Application-scoped service container (deliberately framework-free DI). */
class MyQApp : Application() {

    val epgRepository by lazy { EpgRepository() }
    val reminderStore by lazy { ReminderStore(this) }
    val tasteStore by lazy { TasteStore(this) }
    val recommender by lazy { Recommender(tasteStore) }
    val reminderScheduler by lazy { ReminderScheduler(this) }
}
