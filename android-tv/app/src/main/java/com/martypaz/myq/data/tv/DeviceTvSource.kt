package com.martypaz.myq.data.tv

import android.content.Context
import android.media.tv.TvContract
import com.martypaz.myq.data.epg.EpgRepository
import com.martypaz.myq.data.epg.EpgSource
import com.martypaz.myq.data.model.Newness
import com.martypaz.myq.data.model.Platform
import com.martypaz.myq.data.model.Programme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Listings from the television's own tuner, via the system TV provider.
 *
 * The narrowest of the three sources and the most authoritative: it is the
 * only one describing what this box will actually show, on the regional
 * variant its aerial is tuned to. How far ahead it reaches is up to the tuner
 * — often only the next few hours — so it enriches the published feeds rather
 * than replacing them.
 *
 * Needs READ_TV_LISTINGS. Without it, or on a box with no tuner, this returns
 * nothing and the guide is built from the other sources.
 */
class DeviceTvSource(
    private val context: Context,
    private val lineup: TvLineup,
) : EpgSource {

    override val source = EpgRepository.Source.DEVICE_TUNER

    override suspend fun listings(fromMillis: Long, toMillis: Long): List<Programme> =
        withContext(Dispatchers.IO) {
            if (!lineup.hasPermission) return@withContext emptyList()
            val channels = lineup.channels().associateBy { it.id }
            if (channels.isEmpty()) return@withContext emptyList()

            channels.values.flatMap { channel ->
                programmesOn(channel, fromMillis, toMillis)
            }.sortedBy { it.startMillis }
        }

    private fun programmesOn(channel: TvChannel, from: Long, to: Long): List<Programme> =
        runCatching {
            context.contentResolver.query(
                TvContract.buildProgramsUriForChannel(channel.id, from, to),
                arrayOf(
                    TvContract.Programs.COLUMN_TITLE,
                    TvContract.Programs.COLUMN_SHORT_DESCRIPTION,
                    TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS,
                    TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS,
                    TvContract.Programs.COLUMN_SEASON_NUMBER,
                    TvContract.Programs.COLUMN_EPISODE_NUMBER,
                    TvContract.Programs.COLUMN_EPISODE_TITLE,
                    TvContract.Programs.COLUMN_CANONICAL_GENRE,
                    TvContract.Programs.COLUMN_POSTER_ART_URI,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                val title = cursor.getColumnIndexOrThrow(TvContract.Programs.COLUMN_TITLE)
                val desc = cursor.getColumnIndexOrThrow(TvContract.Programs.COLUMN_SHORT_DESCRIPTION)
                val start = cursor.getColumnIndexOrThrow(TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS)
                val end = cursor.getColumnIndexOrThrow(TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS)
                val season = cursor.getColumnIndexOrThrow(TvContract.Programs.COLUMN_SEASON_NUMBER)
                val episode = cursor.getColumnIndexOrThrow(TvContract.Programs.COLUMN_EPISODE_NUMBER)
                val episodeTitle = cursor.getColumnIndexOrThrow(TvContract.Programs.COLUMN_EPISODE_TITLE)
                val genre = cursor.getColumnIndexOrThrow(TvContract.Programs.COLUMN_CANONICAL_GENRE)
                val poster = cursor.getColumnIndexOrThrow(TvContract.Programs.COLUMN_POSTER_ART_URI)

                buildList {
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(title)?.trim().orEmpty()
                        val startMillis = cursor.getLong(start)
                        if (name.isEmpty() || startMillis <= 0L) continue

                        val endMillis = cursor.getLong(end)
                        add(
                            Programme(
                                id = "tuner-${channel.id}-$startMillis",
                                title = name,
                                episodeTitle = cursor.getString(episodeTitle)?.trim()?.takeIf { it.isNotEmpty() },
                                synopsis = cursor.getString(desc)?.trim().orEmpty(),
                                channelName = channel.displayName,
                                platform = Platform.FREEVIEW,
                                genres = decodeGenres(cursor.getString(genre)),
                                startMillis = startMillis,
                                runtimeMinutes = ((endMillis - startMillis) / 60_000L)
                                    .toInt()
                                    .takeIf { endMillis > startMillis && it in 1..600 },
                                season = cursor.getIntOrNull(season),
                                episode = cursor.getIntOrNull(episode),
                                // The tuner marks no premieres, and guessing
                                // from S1E1 would badge every rerun.
                                newness = Newness.NONE,
                                imageUrl = cursor.getString(poster)?.takeIf { it.isNotEmpty() },
                            ),
                        )
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())

    private fun android.database.Cursor.getIntOrNull(column: Int): Int? =
        if (isNull(column)) null else getInt(column).takeIf { it > 0 }
}

/**
 * Canonical genres arrive as a single encoded string. Decoding is best-effort:
 * a genre we cannot read is worth less than a crash.
 */
internal fun decodeGenres(encoded: String?): List<String> {
    if (encoded.isNullOrBlank()) return emptyList()
    return runCatching { TvContract.Programs.Genres.decode(encoded).toList() }
        .getOrDefault(emptyList())
        .map { it.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase) }
}
