package com.martypaz.myq.data.epg

import com.martypaz.myq.data.model.Programme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * UK Freeview listings from the Freeview-EPG project
 * (github.com/dp247/Freeview-EPG), which publishes a week of XMLTV for the
 * whole line-up, rebuilt every 12 hours.
 *
 * This is the broad source: it has every channel and every slot, where TVmaze
 * has only the shows in its own database. What it does not have is genres, so
 * neither source replaces the other — see [mergeProgrammes].
 *
 * The feed is one ~20MB file. OkHttp asks for it gzipped and the parser
 * streams it, so nothing ever holds the document in memory; the response body
 * is filtered down to the requested window and the Freeview allowlist as it
 * arrives.
 */
class FreeviewEpgApi(
    private val client: OkHttpClient = defaultClient(),
    private val feedUrl: String = FEED_URL,
) {

    suspend fun listings(fromMillis: Long, toMillis: Long): List<Programme> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(feedUrl)
                .header("Accept", "application/xml")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Freeview-EPG responded ${response.code}")
                val body = response.body ?: return@withContext emptyList()
                XmlTvParser.parse(
                    input = body.byteStream(),
                    keepChannel = { isFreeviewChannel(it) && !isTimeshiftChannel(it) },
                    notBefore = fromMillis,
                    notAfter = toMillis,
                )
            }
        }

    companion object {
        const val FEED_URL = "https://raw.githubusercontent.com/dp247/Freeview-EPG/master/epg.xml"

        /** A week of XML over a domestic connection deserves a long read timeout. */
        private fun defaultClient() = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
