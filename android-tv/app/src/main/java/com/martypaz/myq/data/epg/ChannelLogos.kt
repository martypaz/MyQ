package com.martypaz.myq.data.epg

/**
 * Channel idents for the programme cards.
 *
 * The artwork is the set the Freeview-EPG project uses
 * (github.com/dp247/Freeview-EPG): the community tv-logo/tv-logos collection,
 * with dp247's mediaportal-uk-logos mirror covering the channels tv-logos has
 * not picked up. Both are hotlinked and cached by Coil rather than bundled, so
 * a rebrand is a URL change and not a release.
 *
 * Every URL below was checked to resolve. An unmapped channel returns null and
 * the card falls back to the channel name in text, so a gap here is never a
 * blank card.
 */
fun channelLogoUrl(channelName: String): String? = LOGOS[normaliseChannelName(channelName)]

private const val TVL = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries"
private const val UK = "$TVL/united-kingdom"
private const val INT = "$TVL/international"
private const val MP = "https://raw.githubusercontent.com/dp247/mediaportal-uk-logos/master/TV"

private val LOGOS: Map<String, String> = mapOf(
    // BBC
    "bbc one" to "$UK/bbc-one-uk.png",
    "bbc two" to "$UK/bbc-two-uk.png",
    "bbc three" to "$UK/bbc-three-uk.png",
    "bbc four" to "$UK/bbc-four-uk.png",
    "bbc news" to "$UK/bbc-news-uk.png",
    "bbc alba" to "$UK/bbc-alba-uk.png",
    "cbbc" to "$UK/bbc-cbbc-uk.png",
    "cbeebies" to "$UK/bbc-cbeebies-uk.png",

    // ITV
    "itv" to "$UK/itv-1-uk.png",
    "itv1" to "$UK/itv-1-uk.png",
    "itv2" to "$UK/itv-2-uk.png",
    "itv3" to "$UK/itv-3-uk.png",
    "itv4" to "$UK/itv-4-uk.png",
    "itvbe" to "$UK/itv-be-uk.png",
    "stv" to "$UK/stv-uk.png",

    // Channel 4
    "channel 4" to "$UK/channel-4-uk.png",
    "e4" to "$UK/e-4-uk.png",
    "more4" to "$UK/4-more-uk.png",
    "film4" to "$UK/film-4-uk.png",
    "4seven" to "$UK/4-seven-uk.png",

    // Channel 5 — TVmaze reports the main channel as plain "5".
    "5" to "$UK/channel-5-uk.png",
    "channel 5" to "$UK/channel-5-uk.png",
    "5usa" to "$UK/5-usa-uk.png",
    "5 usa" to "$UK/5-usa-uk.png",
    "5star" to "$UK/5-star-uk.png",
    "5select" to "$UK/5-select-uk.png",
    "5action" to "$UK/5-action-uk.png",

    // UKTV — the "U&" prefix is folded away before lookup.
    "dave" to "$MP/U%26Dave.png",
    "drama" to "$MP/U%26DRAMA.png",
    "yesterday" to "$MP/U%26YESTERDAY.png",
    "w" to "$MP/U%26W.png",
    "really" to "$MP/Really.png",

    // Discovery / Warner
    "quest" to "$UK/quest-uk.png",
    "quest red" to "$UK/quest-red-uk.png",
    "dmax" to "$UK/dmax-uk.png",
    "food network" to "$UK/food-network-uk.png",
    "hgtv" to "$UK/hgtv-uk.png",

    // Everything else on the Freeview line-up
    "blaze" to "$UK/blaze-uk.png",
    "challenge" to "$UK/challenge-uk.png",
    "pick" to "$UK/pick-uk.png",
    "great! tv" to "$UK/great-tv-uk.png",
    "legend" to "$MP/Legend.png",
    "that's tv" to "$MP/Thats-TV-plain.png",
    "together tv" to "$UK/together-tv-uk.png",
    "pbs america" to "$UK/pbs-america-uk.png",
    "talking pictures tv" to "$UK/talking-pictures-tv-uk.png",
    "gb news" to "$UK/gb-news-uk.png",
    "talktv" to "$UK/talk-tv-uk.png",
    "london live" to "$UK/london-live-uk.png",
    "s4c" to "$UK/s4c-uk.png",
    "sky arts" to "$MP/Sky-Arts.png",
    "sky mix" to "$MP/Sky-Mix.png",

    // Streaming. The logo repos carry very few of these, so most streaming
    // channels fall back to text by design.
    "netflix" to "$INT/netflix-int.png",
    "disney+" to "$INT/disney-plus-int.png",
)
