package com.martypaz.myq.data.epg

import com.martypaz.myq.data.model.Newness
import com.martypaz.myq.data.model.Platform
import com.martypaz.myq.data.model.Programme
import java.util.concurrent.TimeUnit

/**
 * Offline fallback used when the network is unavailable, so the UI (and an
 * emulator without connectivity) always has something believable to render.
 */
object SampleData {

    fun programmes(now: Long = System.currentTimeMillis()): List<Programme> {
        fun at(hours: Long, minutes: Long = 0) =
            now + TimeUnit.HOURS.toMillis(hours) + TimeUnit.MINUTES.toMillis(minutes)

        return listOf(
            Programme(
                id = "sample-1", title = "The Detectorists Return",
                synopsis = "Comedy. Andy and Lance chase rumours of a Saxon hoard beneath a caravan park.",
                channelName = "BBC One", platform = Platform.FREEVIEW,
                genres = listOf("Comedy"), startMillis = at(2), runtimeMinutes = 30,
                season = 1, episode = 1, newness = Newness.NEW_SERIES,
            ),
            Programme(
                id = "sample-2", title = "Midnight Signals",
                synopsis = "Crime drama. A coastal radio operator intercepts a distress call nobody else heard.",
                channelName = "Channel 4", platform = Platform.FREEVIEW,
                genres = listOf("Drama", "Crime"), startMillis = at(3), runtimeMinutes = 60,
                season = 1, episode = 1, newness = Newness.NEW_SERIES,
            ),
            Programme(
                id = "sample-3", title = "Great British Railway Walks",
                synopsis = "Documentary. Following the ghost of the Somerset & Dorset line on foot.",
                channelName = "Channel 5", platform = Platform.FREEVIEW,
                genres = listOf("Documentary"), startMillis = at(4), runtimeMinutes = 60,
                season = 4, episode = 1, newness = Newness.NEW_SEASON,
            ),
            Programme(
                id = "sample-4", title = "Orbital",
                synopsis = "Sci-fi thriller. A satellite engineer discovers a signal that predicts disasters.",
                channelName = "ITVX", platform = Platform.STREAMING,
                genres = listOf("Science-Fiction", "Thriller"), startMillis = at(5), runtimeMinutes = 45,
                season = 1, episode = 1, newness = Newness.NEW_SERIES,
            ),
            Programme(
                id = "sample-5", title = "Kitchen Frontline",
                synopsis = "Eight chefs battle through service in the country's busiest kitchens.",
                channelName = "BBC iPlayer", platform = Platform.STREAMING,
                genres = listOf("Food", "Reality"), startMillis = at(6), runtimeMinutes = 50,
                season = 2, episode = 1, newness = Newness.NEW_SEASON,
            ),
            Programme(
                id = "sample-6", title = "The Quiet Coast",
                synopsis = "Drama. A retired detective's beach find reopens a thirty-year-old case.",
                channelName = "ITV1", platform = Platform.FREEVIEW,
                genres = listOf("Drama", "Crime"), startMillis = at(7), runtimeMinutes = 60,
                season = 1, episode = 2, newness = Newness.NEW_EPISODE,
            ),
            Programme(
                id = "sample-7", title = "Comedy Vaults",
                synopsis = "Stand-up and sketches from the archive, remastered.",
                channelName = "Dave", platform = Platform.FREEVIEW,
                genres = listOf("Comedy"), startMillis = at(8), runtimeMinutes = 40,
                newness = Newness.NEW_EPISODE,
            ),
            Programme(
                id = "sample-8", title = "Deep Time",
                synopsis = "Nature documentary. What Britain looked like a million years ago.",
                channelName = "BBC Two", platform = Platform.FREEVIEW,
                genres = listOf("Documentary", "Nature"), startMillis = at(24), runtimeMinutes = 60,
                season = 1, episode = 1, newness = Newness.NEW_SERIES,
            ),
            Programme(
                id = "sample-9", title = "Signal Lost",
                synopsis = "Tech thriller. A missing streamer's final broadcast holds the only clue.",
                channelName = "Netflix", platform = Platform.STREAMING,
                genres = listOf("Thriller"), startMillis = at(26), runtimeMinutes = 55,
                season = 1, episode = 1, newness = Newness.NEW_SERIES,
            ),
            Programme(
                id = "sample-10", title = "The Allotment Year",
                synopsis = "Gentle horticulture through the seasons on a Yorkshire hillside.",
                channelName = "More4", platform = Platform.FREEVIEW,
                genres = listOf("Documentary"), startMillis = at(28), runtimeMinutes = 45,
                season = 3, episode = 1, newness = Newness.NEW_SEASON,
            ),
        )
    }
}
