package com.martypaz.myq.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.martypaz.myq.data.epg.channelLogoUrl
import com.martypaz.myq.ui.theme.SkyPalette

/**
 * The channel ident on a programme card: the broadcaster's logo where we have
 * one, the channel name otherwise.
 *
 * The name is not merely a placeholder — it is what shows while the logo
 * loads, when the device is offline, and for every channel the logo repos do
 * not carry. A card must always say which channel it is.
 */
@Composable
fun ChannelChip(channelName: String, modifier: Modifier = Modifier) {
    val logoUrl = channelLogoUrl(channelName)
    val shape = RoundedCornerShape(5.dp)

    Box(modifier = modifier.glass(shape = shape).padding(horizontal = 7.dp, vertical = 3.dp)) {
        if (logoUrl == null) {
            ChannelName(channelName)
        } else {
            SubcomposeAsyncImage(
                model = logoUrl,
                contentDescription = channelName,
                contentScale = ContentScale.Fit,
                loading = { ChannelName(channelName) },
                error = { ChannelName(channelName) },
                modifier = Modifier.height(LOGO_HEIGHT).widthIn(max = LOGO_MAX_WIDTH),
            )
        }
    }
}

@Composable
private fun ChannelName(channelName: String) {
    BasicText(
        text = channelName,
        style = TextStyle(
            color = SkyPalette.TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Sized to sit on the card without competing with the title. */
private val LOGO_HEIGHT = 16.dp
private val LOGO_MAX_WIDTH = 56.dp
