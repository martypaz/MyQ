package com.martypaz.myq.data.epg

import com.martypaz.myq.data.model.Newness
import com.martypaz.myq.data.model.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XmlTvParserTest {

    /** Shaped exactly like the real Freeview-EPG output. */
    private val feed = """
        <tv generator-info-name="freeview-epg">
          <channel id="BBCOne.uk">
            <display-name lang="en">BBC One CI HD</display-name>
            <icon src="https://example.test/bbc-one.png"></icon>
          </channel>
          <channel id="5.uk">
            <display-name lang="en">5 HD</display-name>
          </channel>
          <channel id="SkyAtlantic.uk">
            <display-name lang="en">Sky Atlantic</display-name>
          </channel>
          <programme channel="BBCOne.uk" start="20260804210000 +0100" stop="20260804220000 +0100">
            <title lang="en">Gogglebox</title>
            <desc lang="en">A compilation of highlights.</desc>
            <icon src="https://example.test/gogglebox.jpg"/>
            <episode-num system="xmltv_ns">26.19.0</episode-num>
            <episode-num system="onscreen">S27E20</episode-num>
          </programme>
          <programme channel="5.uk" start="20260804190000 +0100" stop="20260804200000 +0100">
            <title lang="en">24 Hours in A&amp;E</title>
            <desc lang="en">A woman is rushed in.</desc>
          </programme>
          <programme channel="SkyAtlantic.uk" start="20260804200000 +0100" stop="20260804210000 +0100">
            <title lang="en">Pay TV Only</title>
          </programme>
        </tv>
    """.trimIndent()

    private fun parse(
        keepChannel: (String) -> Boolean = ::isFreeviewChannel,
        notBefore: Long = Long.MIN_VALUE,
        notAfter: Long = Long.MAX_VALUE,
    ) = XmlTvParser.parse(feed.byteInputStream(), keepChannel, notBefore, notAfter)

    @Test
    fun `resolves the channel display name from the channel block, without the HD suffix`() {
        val bbc = parse().first { it.title == "Gogglebox" }
        assertEquals("BBC One CI", bbc.channelName)
    }

    @Test
    fun `timeshift channels can be filtered out, since they duplicate the base channel`() {
        val keep: (String) -> Boolean = { isFreeviewChannel(it) && !isTimeshiftChannel(it) }
        assertTrue(isTimeshiftChannel("E4 +1"))
        assertTrue(isTimeshiftChannel("ITV3+1"))
        assertFalse(isTimeshiftChannel("E4"))
        assertFalse(isTimeshiftChannel("5 HD"))
        assertEquals(2, parse(keepChannel = keep).size)
    }

    @Test
    fun `keeps freeview channels and drops pay TV`() {
        assertEquals(
            listOf("24 Hours in A&E", "Gogglebox"),
            parse().map { it.title },
        )
    }

    @Test
    fun `channel 5 survives, whatever the feed calls it`() {
        assertTrue(parse().any { it.title == "24 Hours in A&E" })
    }

    @Test
    fun `unescapes entities in titles and descriptions`() {
        val programme = parse().first { it.title.startsWith("24 Hours") }
        assertEquals("24 Hours in A&E", programme.title)
        assertEquals("A woman is rushed in.", programme.synopsis)
    }

    @Test
    fun `reads start time, runtime and artwork`() {
        val gogglebox = parse().first { it.title == "Gogglebox" }
        // 20:00 UTC == 21:00 +0100
        assertEquals(1785873600_000L, gogglebox.startMillis)
        assertEquals(60, gogglebox.runtimeMinutes)
        assertEquals("https://example.test/gogglebox.jpg", gogglebox.imageUrl)
        assertEquals(Platform.FREEVIEW, gogglebox.platform)
    }

    @Test
    fun `converts zero-based xmltv episode numbering to human numbering`() {
        val gogglebox = parse().first { it.title == "Gogglebox" }
        assertEquals(27, gogglebox.season)
        assertEquals(20, gogglebox.episode)
    }

    @Test
    fun `leaves newness unknown rather than badging reruns as new`() {
        assertTrue(parse().all { it.newness == Newness.NONE })
    }

    @Test
    fun `identifies a programme by its slot, which is unique per channel`() {
        val gogglebox = parse().first { it.title == "Gogglebox" }
        assertEquals("freeview-BBCOne.uk-1785873600000", gogglebox.id)
    }

    @Test
    fun `honours the requested window`() {
        val onlyLate = parse(notBefore = 1785873600_000L)
        assertEquals(listOf("Gogglebox"), onlyLate.map { it.title })

        val onlyEarly = parse(notAfter = 1785873599_000L)
        assertEquals(listOf("24 Hours in A&E"), onlyEarly.map { it.title })
    }

    @Test
    fun `returns programmes in start order`() {
        val starts = parse(keepChannel = { true }).map { it.startMillis }
        assertEquals(starts.sorted(), starts)
    }

    @Test
    fun `skips programmes with no title rather than inventing one`() {
        val untitled = """
            <tv>
              <channel id="BBCOne.uk"><display-name>BBC One</display-name></channel>
              <programme channel="BBCOne.uk" start="20260804210000 +0100"><desc>No title</desc></programme>
            </tv>
        """.trimIndent()
        assertTrue(XmlTvParser.parse(untitled.byteInputStream()).isEmpty())
    }

    @Test
    fun `skips programmes on channels the feed never declared`() {
        val orphan = """
            <tv>
              <programme channel="Unknown.uk" start="20260804210000 +0100"><title>Orphan</title></programme>
            </tv>
        """.trimIndent()
        assertTrue(XmlTvParser.parse(orphan.byteInputStream()).isEmpty())
    }

    @Test
    fun `parses xmltv timestamps, defaulting to UTC when the offset is absent`() {
        assertEquals(1785873600_000L, parseXmlTvTime("20260804210000 +0100"))
        assertEquals(1785873600_000L, parseXmlTvTime("20260804200000 +0000"))
        assertEquals(1785873600_000L, parseXmlTvTime("20260804200000"))
        assertNull(parseXmlTvTime(null))
        assertNull(parseXmlTvTime(""))
        assertNull(parseXmlTvTime("not a time"))
    }

    @Test
    fun `parses xmltv episode numbering, tolerating missing components`() {
        assertEquals(27 to 20, parseXmlTvEpisodeNum("26.19.0"))
        assertEquals(1 to 1, parseXmlTvEpisodeNum("0.0.0"))
        assertEquals(null to 6, parseXmlTvEpisodeNum(".5."))
        assertEquals(3 to 2, parseXmlTvEpisodeNum("2/8.1/10.0"))
        assertEquals(null to null, parseXmlTvEpisodeNum(""))
    }
}
