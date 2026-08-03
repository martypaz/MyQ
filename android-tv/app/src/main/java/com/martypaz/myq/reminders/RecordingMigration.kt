package com.martypaz.myq.reminders

import com.martypaz.myq.data.epg.Remap
import com.martypaz.myq.data.epg.broadcastKey
import com.martypaz.myq.data.epg.remapToCurrentListings
import com.martypaz.myq.data.model.Programme
import com.martypaz.myq.data.model.RecordEntry
import com.martypaz.myq.data.prefs.RecordingStore

/**
 * Reattaches recordings whose programme id no longer exists.
 *
 * Recordings had the same flaw reminders did, and it was half-hidden: the
 * record list still displayed the entry, and series recording still matched,
 * because that matches on title. Only the per-programme check went wrong — so
 * a card the user had marked to record appeared unmarked, and marking it again
 * created a second entry for the same broadcast.
 */
fun remapRecordings(
    recordings: List<RecordEntry>,
    programmes: List<Programme>,
): List<Remap<RecordEntry>> = remapToCurrentListings(
    saved = recordings,
    programmes = programmes,
    idOf = { it.programmeId },
    keyOf = { broadcastKey(it.channelName, it.title, it.startMillis) },
    repoint = { entry, match ->
        entry.copy(
            programmeId = match.id,
            // The matched listing is now the authority on when it starts.
            channelName = match.channelName,
            startMillis = match.startMillis,
        )
    },
)

/**
 * Applies [remapRecordings] to the store.
 *
 * Unlike a reminder, a recording has no alarm of its own to cancel — the
 * reminder it was saved alongside owns that, and is migrated separately.
 *
 * @return how many recordings were moved.
 */
suspend fun migrateRecordingIds(store: RecordingStore, programmes: List<Programme>): Int {
    val remaps = remapRecordings(store.current(), programmes)
    remaps.forEach { (from, to) ->
        store.remove(from.programmeId)
        store.upsert(to)
    }
    return remaps.size
}
