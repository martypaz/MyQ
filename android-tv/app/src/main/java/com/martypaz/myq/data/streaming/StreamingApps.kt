package com.martypaz.myq.data.streaming

import java.net.URLEncoder

/**
 * A streaming or catch-up service MyQ can hand a programme off to.
 *
 * [packages] are Android TV package-name candidates, most likely first. They
 * are only used once the package manager confirms one is installed, so a
 * candidate that is wrong for a given device is inert rather than harmful.
 */
data class StreamingApp(
    val id: String,
    val displayName: String,
    val packages: List<String>,
    /**
     * Title-search URL for the service. Each service claims its own domain as
     * an Android App Link, so viewing one of these opens the installed app and
     * falls back to a browser when it is absent.
     *
     * The EPG gives us no per-programme identifier for these services, so a
     * title search is the deepest link we can honestly build.
     */
    val searchUrlTemplate: String,
    /**
     * Launcher labels that mean this service. Package names vary by device —
     * BBC iPlayer alone ships under several — but the name a viewer sees on
     * the home screen does not, so it is the more reliable way to find an
     * installed app.
     */
    val labels: List<String> = emptyList(),
) {
    /** Where to send a viewer looking for [title]. */
    fun searchUrl(title: String): String =
        searchUrlTemplate + URLEncoder.encode(title.trim(), "UTF-8")

    /** The package to offer in the store when none of [packages] is installed. */
    val storePackage: String get() = packages.first()

    /** Matches a launcher label loosely enough to survive "BBC iPlayer (Beta)". */
    fun matchesLabel(label: String): Boolean {
        val candidate = label.trim().lowercase()
        if (candidate.isEmpty()) return false
        return (labels + displayName).any { known ->
            val expected = known.trim().lowercase()
            candidate == expected || candidate.startsWith("$expected ")
        }
    }
}

/**
 * The service carrying [channelName], or null when MyQ has no app for it —
 * callers should hide the watch action rather than offer a dead end.
 */
fun streamingAppFor(channelName: String): StreamingApp? {
    val name = normalise(channelName)
    if (name.isEmpty()) return null
    return SERVICES.firstOrNull { name in it.channels }?.app
        ?: SERVICES.firstOrNull { service -> service.prefixes.any { name.startsWith(it) } }?.app
}

/**
 * Channel names arrive from TVmaze in whatever form the broadcaster last
 * rebranded to, so fold away the noise: case, UKTV's "U&" prefix, and the
 * "HD"/"+1" variants that are the same service.
 */
private fun normalise(name: String): String =
    name.trim()
        .lowercase()
        .removePrefix("u&")
        .removeSuffix(" hd")
        .removeSuffix(" +1")
        .removeSuffix("+1")
        .trim()

private class Service(
    val app: StreamingApp,
    val channels: Set<String>,
    val prefixes: List<String> = emptyList(),
)

private val SERVICES: List<Service> = listOf(
    Service(
        app = StreamingApp(
            id = "iplayer",
            displayName = "BBC iPlayer",
            packages = listOf("bbc.iplayer.android", "uk.co.bbc.iplayer"),
            searchUrlTemplate = "https://www.bbc.co.uk/iplayer/search?q=",
            labels = listOf("BBC iPlayer", "iPlayer"),
        ),
        channels = setOf(
            "bbc one", "bbc two", "bbc three", "bbc four", "cbbc", "cbeebies",
            "bbc news", "bbc scotland", "bbc alba", "bbc iplayer", "iplayer",
        ),
        prefixes = listOf("bbc"),
    ),
    Service(
        app = StreamingApp(
            id = "itvx",
            displayName = "ITVX",
            packages = listOf("air.ITVMobilePlayer", "com.itv.itvx"),
            searchUrlTemplate = "https://www.itv.com/search?query=",
            labels = listOf("ITVX", "ITV Hub", "ITV Player"),
        ),
        channels = setOf("itv", "itv1", "itv2", "itv3", "itv4", "itvbe", "itvx", "itv hub"),
        prefixes = listOf("itv"),
    ),
    Service(
        app = StreamingApp(
            id = "channel4",
            displayName = "Channel 4",
            packages = listOf("com.channel4.ondemand"),
            searchUrlTemplate = "https://www.channel4.com/search?q=",
            labels = listOf("Channel 4", "All 4", "4"),
        ),
        channels = setOf("channel 4", "channel4", "e4", "more4", "film4", "4seven", "all 4", "walter presents"),
    ),
    Service(
        app = StreamingApp(
            id = "my5",
            displayName = "My5",
            packages = listOf("com.channel5.my5", "com.my5.tv", "com.channel5.my5tv"),
            searchUrlTemplate = "https://www.channel5.com/search?q=",
            labels = listOf("My5", "Channel 5"),
        ),
        channels = setOf("channel 5", "channel5", "5usa", "5star", "5select", "5action", "my5"),
    ),
    Service(
        app = StreamingApp(
            id = "uktv",
            displayName = "U",
            packages = listOf("uk.co.uktv.play", "com.uktv.play"),
            searchUrlTemplate = "https://u.co.uk/search?q=",
            labels = listOf("U", "UKTV Play", "UKTV"),
        ),
        channels = setOf("dave", "drama", "yesterday", "w", "really", "alibi", "gold", "eden", "uktv play"),
    ),
    Service(
        app = StreamingApp(
            id = "stv",
            displayName = "STV Player",
            packages = listOf("uk.co.stv.player", "tv.stv.player"),
            searchUrlTemplate = "https://player.stv.tv/search?q=",
            labels = listOf("STV Player", "STV"),
        ),
        channels = setOf("stv", "stv player"),
    ),
    Service(
        app = StreamingApp(
            id = "netflix",
            displayName = "Netflix",
            packages = listOf("com.netflix.ninja", "com.netflix.mediaclient"),
            searchUrlTemplate = "https://www.netflix.com/search?q=",
            labels = listOf("Netflix"),
        ),
        channels = setOf("netflix"),
    ),
    Service(
        app = StreamingApp(
            id = "primevideo",
            displayName = "Prime Video",
            packages = listOf("com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient"),
            searchUrlTemplate = "https://www.primevideo.com/search?phrase=",
            labels = listOf("Prime Video", "Amazon Prime Video"),
        ),
        channels = setOf("prime video", "amazon prime video", "amazon", "amazon prime"),
    ),
    Service(
        app = StreamingApp(
            id = "disneyplus",
            displayName = "Disney+",
            packages = listOf("com.disney.disneyplus"),
            searchUrlTemplate = "https://www.disneyplus.com/search?q=",
            labels = listOf("Disney+", "Disney Plus"),
        ),
        channels = setOf("disney+", "disney plus", "disneyplus"),
    ),
    Service(
        app = StreamingApp(
            id = "appletv",
            displayName = "Apple TV",
            packages = listOf("com.apple.atve.androidtv.appletv", "com.apple.atve.sony.appletv"),
            searchUrlTemplate = "https://tv.apple.com/search?term=",
            labels = listOf("Apple TV", "Apple TV+"),
        ),
        channels = setOf("apple tv+", "apple tv plus", "apple tv"),
    ),
    Service(
        app = StreamingApp(
            id = "paramountplus",
            displayName = "Paramount+",
            packages = listOf("com.cbs.ott"),
            searchUrlTemplate = "https://www.paramountplus.com/search/?q=",
            labels = listOf("Paramount+", "Paramount Plus"),
        ),
        channels = setOf("paramount+", "paramount plus", "paramount network"),
    ),
    Service(
        app = StreamingApp(
            id = "discoveryplus",
            displayName = "discovery+",
            packages = listOf("com.discovery.discoveryplus.androidtv", "com.discovery.dplay"),
            searchUrlTemplate = "https://www.discoveryplus.com/gb/search?q=",
            labels = listOf("discovery+", "Discovery Plus"),
        ),
        channels = setOf("discovery+", "discovery plus", "quest", "quest red", "dmax", "food network", "hgtv"),
    ),
    Service(
        app = StreamingApp(
            id = "youtube",
            displayName = "YouTube",
            packages = listOf("com.google.android.youtube.tv", "com.google.android.youtube"),
            searchUrlTemplate = "https://www.youtube.com/results?search_query=",
            labels = listOf("YouTube", "YouTube TV"),
        ),
        channels = setOf("youtube"),
    ),
)
