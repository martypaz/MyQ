package com.martypaz.myq.reminders

import com.martypaz.myq.data.epg.broadcastKey
import com.martypaz.myq.data.model.Programme
import com.martypaz.myq.data.model.Reminder
import com.martypaz.myq.data.prefs.ReminderStore

/** A stored reminder and the same reminder repointed at the current listings. */
data class ReminderRemap(val from: Reminder, val to: Reminder)

/**
 * Reattaches reminders whose programme id no longer exists.
 *
 * A reminder records the id the listings used when it was set. Change where
 * the listings come from and those ids change with it — a programme saved as
 * "tvmaze-3686256" now arrives as "freeview-BBCOne.uk-1785873600000". The
 * reminder still fires, because the alarm carries its own copy of the details,
 * but nothing in the guide recognises it: no badge on the card, and the dialog
 * offers to set a reminder that is already set.
 *
 * A reminder knows the title, channel and start time of what it is for, which
 * is exactly what identifies a broadcast, so the same key that merges the two
 * sources can find the programme again under its new id.
 *
 * Pure, so the matching is testable without a store or an alarm manager.
 */
fun remapReminders(reminders: List<Reminder>, programmes: List<Programme>): List<ReminderRemap> {
    if (reminders.isEmpty() || programmes.isEmpty()) return emptyList()

    val knownIds = programmes.mapTo(HashSet(programmes.size)) { it.id }
    val byBroadcast = programmes.associateBy(::broadcastKey)
    val claimed = HashSet<String>()

    return reminders.mapNotNull { reminder ->
        // Still recognised: leave it alone.
        if (reminder.programmeId in knownIds) return@mapNotNull null

        val match = byBroadcast[broadcastKey(reminder.channelName, reminder.title, reminder.startMillis)]
            ?: return@mapNotNull null
        if (match.id == reminder.programmeId) return@mapNotNull null
        // Two stale reminders for one broadcast would collide on the new id.
        if (!claimed.add(match.id)) return@mapNotNull null

        ReminderRemap(
            from = reminder,
            to = reminder.copy(
                programmeId = match.id,
                // The matched listing is now the authority on when it starts,
                // which is what the alarm is set from.
                channelName = match.channelName,
                startMillis = match.startMillis,
            ),
        )
    }
}

/**
 * Applies [remapReminders] to the stored reminders and their alarms.
 *
 * The alarm's PendingIntent is keyed on the programme id, so a re-pointed
 * reminder needs its old alarm cancelled explicitly — otherwise the original
 * would survive alongside the new one and fire twice.
 *
 * @return how many reminders were moved.
 */
suspend fun migrateReminderIds(
    store: ReminderStore,
    scheduler: ReminderScheduler,
    programmes: List<Programme>,
): Int {
    val remaps = remapReminders(store.current(), programmes)
    remaps.forEach { (from, to) ->
        scheduler.cancel(from.programmeId)
        store.remove(from.programmeId)
        store.upsert(to)
        scheduler.schedule(to)
    }
    return remaps.size
}
