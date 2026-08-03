package com.martypaz.myq.data.epg

import com.martypaz.myq.data.model.Programme

/**
 * One way of finding out what is on.
 *
 * MyQ takes listings from a published XMLTV feed, from a web API, and from the
 * television's own tuner. They arrive as XML, as JSON and as a content-provider
 * cursor, over very different timescales, and each knows things the others do
 * not. Behind this interface they are all the same thing: a list of
 * [Programme]s covering a window.
 *
 * Implementations must not throw. A source that cannot answer returns nothing,
 * because the guide is built from whichever sources did.
 */
interface EpgSource {

    /** Identifies this source to the merge order and to the user in Settings. */
    val source: EpgRepository.Source

    /** Listings overlapping [fromMillis]..[toMillis], or empty if unavailable. */
    suspend fun listings(fromMillis: Long, toMillis: Long): List<Programme>
}

/**
 * Folds every source's listings into one guide, in priority order.
 *
 * The first list owns identity and wins any disagreement; each later one fills
 * blanks the earlier ones left. That ordering is the whole design: the feed
 * with the broadest schedule decides what exists, and the sources that know
 * genres, or what this particular box receives, enrich it without being able
 * to remove anything.
 */
fun mergeAll(byPriority: List<List<Programme>>): List<Programme> =
    byPriority.filter { it.isNotEmpty() }
        .reduceOrNull { merged, next -> mergeProgrammes(merged, next) }
        ?: emptyList()
