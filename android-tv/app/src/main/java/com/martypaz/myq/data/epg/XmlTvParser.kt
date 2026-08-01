package com.martypaz.myq.data.epg

import com.martypaz.myq.data.model.Newness
import com.martypaz.myq.data.model.Platform
import com.martypaz.myq.data.model.Programme
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.SAXParserFactory

/**
 * Reads XMLTV listings into [Programme]s.
 *
 * SAX rather than a document parser: the feed is ~20MB of XML covering a week
 * of every channel, and MyQ wants a couple of days of about fifty of them.
 * Streaming lets the filters throw most of it away without ever holding the
 * document in memory — which matters on a television.
 *
 * SAX is also the one XML API present both on Android and on a desktop JVM,
 * so this parses identically in unit tests and on device.
 */
object XmlTvParser {

    /**
     * @param keepChannel decides which channels survive, by display name.
     * @param notBefore drops programmes that have already finished.
     * @param notAfter drops programmes beyond the window the caller asked for.
     */
    fun parse(
        input: InputStream,
        keepChannel: (String) -> Boolean = { true },
        notBefore: Long = Long.MIN_VALUE,
        notAfter: Long = Long.MAX_VALUE,
    ): List<Programme> {
        val handler = XmlTvHandler(keepChannel, notBefore, notAfter)
        SAXParserFactory.newInstance()
            .apply { isNamespaceAware = false }
            .newSAXParser()
            .parse(input, handler)
        return handler.programmes.sortedBy { it.startMillis }
    }
}

private class XmlTvHandler(
    private val keepChannel: (String) -> Boolean,
    private val notBefore: Long,
    private val notAfter: Long,
) : DefaultHandler() {

    val programmes = mutableListOf<Programme>()

    /** channel id -> display name, populated by the <channel> block up top. */
    private val channelNames = mutableMapOf<String, String>()

    private var channelId: String? = null
    private var current: Builder? = null
    private var text = StringBuilder()
    private var capturing: String? = null

    override fun startElement(uri: String?, local: String?, name: String, attrs: Attributes) {
        when (name) {
            "channel" -> channelId = attrs.getValue("id")
            "display-name" -> if (channelId != null) capture(name)
            "programme" -> current = startProgramme(attrs)
            "title", "desc" -> if (current != null) capture(name)
            "episode-num" -> if (current != null && attrs.getValue("system") == "xmltv_ns") capture(name)
            "icon" -> current?.let { it.iconUrl = attrs.getValue("src") ?: it.iconUrl }
        }
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        if (capturing != null) text.appendRange(ch, start, start + length)
    }

    override fun endElement(uri: String?, local: String?, name: String) {
        when (name) {
            "display-name" -> {
                val id = channelId
                // Only the first display-name counts; later ones are aliases.
                if (id != null && capturing == name && id !in channelNames) {
                    channelNames[id] = finishCapture()
                }
            }
            "channel" -> channelId = null
            "title" -> current?.let { if (capturing == name) it.title = finishCapture() }
            "desc" -> current?.let { if (capturing == name) it.desc = finishCapture() }
            "episode-num" -> current?.let { if (capturing == name) it.episodeNum = finishCapture() }
            "programme" -> {
                current?.build()?.let(programmes::add)
                current = null
            }
        }
        if (capturing == name) capturing = null
    }

    private fun capture(name: String) {
        capturing = name
        text = StringBuilder()
    }

    private fun finishCapture(): String {
        capturing = null
        return text.toString().trim()
    }

    private fun startProgramme(attrs: Attributes): Builder? {
        val id = attrs.getValue("channel") ?: return null
        val channelName = channelNames[id] ?: return null
        if (!keepChannel(channelName)) return null

        val start = parseXmlTvTime(attrs.getValue("start")) ?: return null
        if (start < notBefore || start > notAfter) return null

        return Builder(
            channelId = id,
            channelName = channelName,
            startMillis = start,
            stopMillis = parseXmlTvTime(attrs.getValue("stop")),
        )
    }

    private class Builder(
        val channelId: String,
        val channelName: String,
        val startMillis: Long,
        val stopMillis: Long?,
        var title: String = "",
        var desc: String = "",
        var iconUrl: String? = null,
        var episodeNum: String = "",
    ) {
        fun build(): Programme? {
            if (title.isBlank()) return null
            val (season, episode) = parseXmlTvEpisodeNum(episodeNum)
            return Programme(
                // No episode ids in XMLTV, so identity is the slot itself:
                // one channel can only show one thing at a given moment.
                id = "freeview-$channelId-$startMillis",
                title = title,
                synopsis = desc,
                channelName = displayChannelName(channelName),
                platform = Platform.FREEVIEW,
                startMillis = startMillis,
                runtimeMinutes = stopMillis?.let { ((it - startMillis) / 60_000L).toInt() }
                    ?.takeIf { it in 1..600 },
                season = season,
                episode = episode,
                // XMLTV carries no premiere marker, and inferring "new series"
                // from S1E1 would badge every rerun of an old show. Leave it
                // unknown and let TVmaze, which knows premiere dates, say so
                // when the two sources are merged.
                newness = Newness.NONE,
                imageUrl = iconUrl,
            )
        }
    }
}

private val XMLTV_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z")

/** XMLTV stamps look like "20260804010500 +0100". */
internal fun parseXmlTvTime(raw: String?): Long? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    // The offset is optional in the spec; assume UTC when it is absent.
    val normalised = if (' ' in value) value else "$value +0000"
    return runCatching {
        OffsetDateTime.parse(normalised, XMLTV_TIME).toInstant().toEpochMilli()
    }.getOrNull()
}

/**
 * xmltv_ns numbering is zero-based and dot-separated ("26.19.0" is season 27,
 * episode 20). Any component may be blank when the broadcaster does not know
 * it.
 */
internal fun parseXmlTvEpisodeNum(raw: String): Pair<Int?, Int?> {
    if (raw.isBlank()) return null to null
    val parts = raw.split('.')
    fun part(index: Int): Int? = parts.getOrNull(index)
        ?.substringBefore('/')
        ?.trim()
        ?.toIntOrNull()
        ?.let { it + 1 }
        ?.takeIf { it > 0 }
    return part(0) to part(1)
}
