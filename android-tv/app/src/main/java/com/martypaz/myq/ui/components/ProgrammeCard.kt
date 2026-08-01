package com.martypaz.myq.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.martypaz.myq.data.model.Newness
import com.martypaz.myq.data.model.Programme
import com.martypaz.myq.ui.formatStart
import com.martypaz.myq.ui.theme.SkyPalette

/**
 * A 16:9 rail card. Focus grows the card and draws the white Sky Q focus
 * ring; the channel chip sits top-left and badges sit top-right.
 */
@Composable
fun ProgrammeCard(
    programme: Programme,
    hasReminder: Boolean,
    isRecording: Boolean = false,
    onFocused: (Programme) -> Unit,
    onSelected: (Programme) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "cardScale",
    )

    Box(
        modifier = modifier
            .width(224.dp)
            .height(126.dp)
            .scale(scale)
            .glass(focused = isFocused, shape = RoundedCornerShape(10.dp))
            .onFocusChanged { if (it.isFocused) onFocused(programme) }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { onSelected(programme) },
    ) {
        programme.imageUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = programme.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Bottom scrim so the title stays readable over any image.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.48f to Color.Transparent,
                        1f to Color(0xD9060B1D),
                    ),
                ),
        )

        // Channel ident, top-left — a frosted chip, mirroring Sky Q's tiles.
        ChannelChip(
            channelName = programme.channelName,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )

        val badge = when {
            isRecording -> "●" to SkyPalette.RecordBadge
            hasReminder -> "⏰" to SkyPalette.ReminderBadge
            programme.newness == Newness.NEW_SERIES -> "NEW" to SkyPalette.AccentBadge
            programme.newness == Newness.NEW_SEASON -> "NEW SEASON" to SkyPalette.AccentBadge
            else -> null
        }
        badge?.let { (label, color) ->
            BasicText(
                text = label,
                style = TextStyle(
                    color = Color(0xFF060B1D),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(color, RoundedCornerShape(3.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        // Title + start time along the bottom edge.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            BasicText(
                text = programme.title,
                style = TextStyle(
                    color = SkyPalette.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 16.dp),
            )
            BasicText(
                text = formatStart(programme.startMillis),
                style = TextStyle(color = SkyPalette.TextTertiary, fontSize = 11.sp),
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }
}
