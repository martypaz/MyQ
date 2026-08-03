package com.martypaz.myq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.martypaz.myq.data.epg.normaliseChannelName
import com.martypaz.myq.data.model.Programme
import com.martypaz.myq.ui.theme.SkyPalette

/**
 * Image renderer for programme cards and modals.
 *
 * When an image URL is present and loads successfully, renders [AsyncImage].
 * If missing or failed to load, renders a Sky Q-styled branded fallback poster
 * so cards and modals never present a blank box.
 */
@Composable
fun ProgrammeImage(
    programme: Programme,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    var isError by remember(programme.id) { mutableStateOf(programme.imageUrl == null) }

    if (!isError && programme.imageUrl != null) {
        AsyncImage(
            model = programme.imageUrl,
            contentDescription = programme.title,
            contentScale = contentScale,
            onError = { isError = true },
            modifier = modifier,
        )
    } else {
        FallbackProgrammePoster(programme = programme, modifier = modifier)
    }
}

@Composable
fun FallbackProgrammePoster(
    programme: Programme,
    modifier: Modifier = Modifier,
) {
    val (primaryColor, secondaryColor) = gradientColorsForChannel(programme.channelName)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(primaryColor, secondaryColor),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Subtle ambient inner scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color(0xD9060B1D),
                    ),
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp),
        ) {
            // Channel Watermark Chip
            BasicText(
                text = programme.channelName.uppercase(),
                style = TextStyle(
                    color = SkyPalette.TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Title Watermark
            BasicText(
                text = programme.title,
                style = TextStyle(
                    color = SkyPalette.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private fun gradientColorsForChannel(channelName: String): Pair<Color, Color> {
    val norm = normaliseChannelName(channelName)
    return when {
        norm.contains("bbc") -> Color(0xFF1E3A8A) to Color(0xFF0F172A)
        norm.contains("itv") -> Color(0xFF0F766E) to Color(0xFF0F172A)
        norm.contains("channel 4") || norm.contains("e4") || norm.contains("more4") ->
            Color(0xFF6B21A8) to Color(0xFF0F172A)
        norm.contains("film") -> Color(0xFF991B1B) to Color(0xFF0F172A)
        norm.contains("5") -> Color(0xFF0284C7) to Color(0xFF0F172A)
        norm.contains("dave") || norm.contains("comedy") -> Color(0xFFB45309) to Color(0xFF0F172A)
        else -> Color(0xFF1D4ED8) to Color(0xFF0B132B)
    }
}
