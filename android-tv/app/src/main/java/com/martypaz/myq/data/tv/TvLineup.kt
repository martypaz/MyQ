package com.martypaz.myq.data.tv

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.tv.TvContract
import androidx.core.content.ContextCompat
import com.martypaz.myq.data.epg.normaliseChannelName
import com.martypaz.myq.data.model.Programme

/** A channel this television is actually tuned to receive. */
data class TvChannel(
    val id: Long,
    val displayName: String,
    /** As shown on the remote — "101", "102". Null when the input omits it. */
    val number: String?,
) {
    val tuneUri: android.net.Uri get() = TvContract.buildChannelUri(id)
}

/**
 * The channel line-up held by the television itself, read through the system
 * TV provider.
 *
 * This is the only source that knows what this particular box receives: which
 * regional variant its aerial is tuned to, which channels are missing from a
 * weak transmitter, and the numbers on the remote. The two listings feeds are
 * national and cannot know any of it.
 *
 * Reading it needs READ_TV_LISTINGS, which is a runtime permission and may be
 * refused. Every method here degrades to "we do not know", never to an error:
 * an empty line-up must leave the guide exactly as it was rather than emptying
 * it.
 */
class TvLineup(private val context: Context) {

    @Volatile
    private var cached: List<TvChannel>? = null

    val hasPermission: Boolean
        get() = ContextCompat.checkSelfPermission(context, "android.permission.READ_TV_LISTINGS") ==
            PackageManager.PERMISSION_GRANTED

    /** Channels this box receives, or empty when unknown. Cached after the first read. */
    fun channels(): List<TvChannel> = cached ?: read().also { cached = it }

    fun invalidate() {
        cached = null
    }

    /** The tuner channel carrying [channelName], if this box has it. */
    fun channelFor(channelName: String): TvChannel? {
        val wanted = normaliseChannelName(channelName)
        if (wanted.isEmpty()) return null
        return channels().firstOrNull { normaliseChannelName(it.displayName) == wanted }
    }

    private fun read(): List<TvChannel> {
        if (!hasPermission) return emptyList()

        // A SecurityException is still possible if the grant is revoked between
        // the check and the query, and some inputs reject the projection.
        return runCatching {
            context.contentResolver.query(
                TvContract.Channels.CONTENT_URI,
                arrayOf(
                    TvContract.Channels._ID,
                    TvContract.Channels.COLUMN_DISPLAY_NAME,
                    TvContract.Channels.COLUMN_DISPLAY_NUMBER,
                    TvContract.Channels.COLUMN_SERVICE_TYPE,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                buildList {
                    val idColumn = cursor.getColumnIndexOrThrow(TvContract.Channels._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(TvContract.Channels.COLUMN_DISPLAY_NAME)
                    val numberColumn = cursor.getColumnIndexOrThrow(TvContract.Channels.COLUMN_DISPLAY_NUMBER)
                    val typeColumn = cursor.getColumnIndexOrThrow(TvContract.Channels.COLUMN_SERVICE_TYPE)

                    while (cursor.moveToNext()) {
                        // Radio services share the line-up and are not watchable here.
                        if (cursor.getString(typeColumn) != TvContract.Channels.SERVICE_TYPE_AUDIO_VIDEO) {
                            continue
                        }
                        val name = cursor.getString(nameColumn)?.trim().orEmpty()
                        if (name.isEmpty()) continue
                        add(
                            TvChannel(
                                id = cursor.getLong(idColumn),
                                displayName = name,
                                number = cursor.getString(numberColumn)?.trim()?.takeIf { it.isNotEmpty() },
                            ),
                        )
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }
}

/**
 * Narrows listings to what this box can actually receive.
 *
 * Returns [programmes] untouched when the line-up is unknown — no permission,
 * no tuner, or nothing scanned. Showing a national guide is a much smaller
 * failure than showing an empty one.
 */
fun restrictToLineup(programmes: List<Programme>, lineup: List<TvChannel>): List<Programme> {
    if (lineup.isEmpty()) return programmes
    val received = lineup.mapTo(HashSet(lineup.size)) { normaliseChannelName(it.displayName) }
    val kept = programmes.filter { normaliseChannelName(it.channelName) in received }
    // A line-up that matches nothing means the names disagree, not that the
    // television receives nothing.
    return kept.ifEmpty { programmes }
}

/**
 * Switches the television to [channel] through whichever app owns live TV.
 *
 * Resolved before it is started: not every device has a live-TV app, and an
 * unhandled intent shows a system error rather than throwing something
 * catchable.
 */
fun Context.tuneTo(channel: TvChannel): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, channel.tuneUri)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (packageManager.resolveActivity(intent, 0) == null) return false
    return runCatching { startActivity(intent) }.isSuccess
}
