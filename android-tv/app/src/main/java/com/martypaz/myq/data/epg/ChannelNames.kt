package com.martypaz.myq.data.epg

/**
 * One way of reading a channel name, shared by every EPG source.
 *
 * Sources disagree about the same channel: TVmaze calls Channel 5 "5", the
 * XMLTV feed calls it "5 HD", and either may name a regional opt-out or a
 * timeshift. Anything that matches a channel — the Freeview allowlist, the
 * logo table, the streaming hand-off — must fold those apart the same way, or
 * a channel silently vanishes from the guide on one source and not the other.
 */
internal fun normaliseChannelName(name: String): String {
    var key = name.trim().lowercase()
    if (key.startsWith("u&")) key = key.removePrefix("u&")
    key = key.removeSuffix(" hd").removeSuffix(" +1").removeSuffix("+1").trim()
    return REGIONS.fold(key) { acc, region -> acc.removeSuffix(" $region") }.trim()
}

/** Regional opt-outs that are the same channel as far as a guide is concerned. */
private val REGIONS = listOf(
    "london", "anglia", "midlands", "central", "north", "north east", "north west",
    "south", "south east", "south west", "west", "east", "yorkshire", "wales",
    "scotland", "northern ireland", "granada", "meridian", "tyne tees", "border",
    "channel islands", "ci",
)

/**
 * Matches a channel name against the UK Freeview line-up, so pay-TV-only
 * channels cannot leak into a Freeview guide.
 */
fun isFreeviewChannel(name: String): Boolean =
    normaliseChannelName(name) in NORMALISED_FREEVIEW_CHANNELS

/**
 * True for a timeshift channel — "E4 +1", "ITV3+1".
 *
 * They carry the same programmes an hour later, which a full listings feed
 * reports in full. Left in, every card on those channels appears twice, an
 * hour apart, and the rails fill up with the repeat rather than with something
 * else to watch.
 */
fun isTimeshiftChannel(name: String): Boolean =
    name.trim().lowercase().let { it.endsWith("+1") || it.endsWith(" +1") }

/**
 * The channel name as a viewer should see it. Sources tack on the broadcast
 * standard, which tells nobody anything: "BBC One London HD" is BBC One
 * London, and the region is worth keeping.
 */
fun displayChannelName(name: String): String =
    name.trim().removeSuffix(" HD").removeSuffix(" hd").trim().ifEmpty { name }

/**
 * The main UK Freeview line-up. Entries are matched after normalisation, so
 * one name per channel covers its HD, +1 and regional variants.
 */
val FREEVIEW_CHANNELS: Set<String> = setOf(
    "BBC One", "BBC Two", "BBC Three", "BBC Four", "CBBC", "CBeebies",
    "BBC News", "ITV", "ITV1", "ITV2", "ITV3", "ITV4", "ITVBe",
    "Channel 4", "E4", "More4", "Film4", "4seven",
    // TVmaze reports the main Channel 5 service as a bare "5".
    "5", "Channel 5", "5USA", "5STAR", "5SELECT", "5ACTION",
    "Dave", "Drama", "Yesterday", "W", "Quest", "Quest Red",
    "Really", "DMAX", "Food Network", "HGTV", "Blaze",
    "Sky Arts", "Sky Mix", "Challenge", "Pick", "GREAT! TV",
    "Legend", "That's TV", "Together TV", "PBS America", "Talking Pictures TV",
    "London Live", "GB News", "TalkTV", "S4C", "STV",
)

private val NORMALISED_FREEVIEW_CHANNELS: Set<String> =
    FREEVIEW_CHANNELS.map(::normaliseChannelName).toSet()
