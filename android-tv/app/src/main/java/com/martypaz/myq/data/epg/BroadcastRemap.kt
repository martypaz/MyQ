package com.martypaz.myq.data.epg

import com.martypaz.myq.data.model.Programme

/** Something the user saved, and the same thing repointed at current listings. */
data class Remap<T>(val from: T, val to: T)

/**
 * Reattaches anything saved against a programme id that no longer exists.
 *
 * Reminders and recordings both store the id the listings used when they were
 * saved, and those ids change whenever the listings source does. What they
 * also both store is the title, channel and start time — which is what
 * identifies a broadcast — so the same key that merges the sources can find
 * the programme again under its new id.
 *
 * Shared rather than written twice: the matching is subtle enough that two
 * copies would drift, and a drift here means a reminder that quietly stops
 * being recognised.
 *
 * Deliberately conservative. It will not move an entry onto a different
 * broadcast of the same series, and where two stale entries match one new id
 * only the first moves, rather than one silently replacing the other.
 */
internal fun <T> remapToCurrentListings(
    saved: List<T>,
    programmes: List<Programme>,
    idOf: (T) -> String,
    keyOf: (T) -> String,
    repoint: (T, Programme) -> T,
): List<Remap<T>> {
    if (saved.isEmpty() || programmes.isEmpty()) return emptyList()

    val knownIds = programmes.mapTo(HashSet(programmes.size)) { it.id }
    val byBroadcast = programmes.associateBy(::broadcastKey)
    val claimed = HashSet<String>()

    return saved.mapNotNull { entry ->
        // Still recognised: leave it alone.
        if (idOf(entry) in knownIds) return@mapNotNull null

        val match = byBroadcast[keyOf(entry)] ?: return@mapNotNull null
        if (match.id == idOf(entry)) return@mapNotNull null
        if (!claimed.add(match.id)) return@mapNotNull null

        Remap(from = entry, to = repoint(entry, match))
    }
}
