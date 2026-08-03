package com.martypaz.myq.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.martypaz.myq.data.epg.channelLogoUrl

/**
 * The channel ident on a programme card: the broadcaster's logo where we have
 * one, the channel name otherwise.
 *
 * Logos are flattened to white at 80% and sit directly on the artwork with no
 * chip behind them. Broadcaster idents come in clashing brand colours, and a
 * wall of them turns a rail into a pick-and-mix; as white silhouettes they
 * read as one system and stay subordinate to the programme image.
 *
 * The name is not merely a placeholder — it is what shows while the logo
 * loads, when the device is offline, and for every channel the logo repos do
 * not carry. A card must always say which channel it is.
 */
@Composable
fun ChannelChip(channelName: String, modifier: Modifier = Modifier) {
    val logoUrl = channelLogoUrl(channelName)

    Box(modifier = modifier.alpha(IDENT_OPACITY)) {
        if (logoUrl == null) {
            ChannelName(channelName)
        } else {
            SubcomposeAsyncImage(
                model = logoUrl,
                contentDescription = channelName,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(Color.White),
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
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Present but never competing with the programme's own title. */
private const val IDENT_OPACITY = 0.8f

/** Sized to sit on the card without competing with the title. */
private val LOGO_HEIGHT = 16.dp
private val LOGO_MAX_WIDTH = 56.dp
