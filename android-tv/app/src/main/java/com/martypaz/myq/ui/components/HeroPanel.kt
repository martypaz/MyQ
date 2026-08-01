package com.martypaz.myq.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.TextStyle
import com.martypaz.myq.data.model.Newness
import com.martypaz.myq.data.model.Programme
import com.martypaz.myq.ui.formatSeasonEpisode
import com.martypaz.myq.ui.formatStart
import com.martypaz.myq.ui.theme.SkyPalette

/**
 * The Sky Q-style information panel pinned above the rails: channel, title,
 * time and synopsis for whichever card currently has focus.
 */
@Composable
fun HeroPanel(
    programme: Programme?,
    hasReminder: Boolean,
    isRecording: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(118.dp)
            .glass()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (programme == null) return@Column

        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                text = programme.channelName.uppercase(),
                style = TextStyle(
                    color = SkyPalette.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                ),
            )
            Spacer(Modifier.width(14.dp))
            BasicText(
                text = formatStart(programme.startMillis),
                style = TextStyle(color = SkyPalette.TextTertiary, fontSize = 14.sp),
            )
            programme.runtimeMinutes?.let { minutes ->
                Spacer(Modifier.width(14.dp))
                BasicText(
                    text = "${minutes}m",
                    style = TextStyle(color = SkyPalette.TextTertiary, fontSize = 14.sp),
                )
            }
            if (programme.newness == Newness.NEW_SERIES || programme.newness == Newness.NEW_SEASON) {
                Spacer(Modifier.width(14.dp))
                BasicText(
                    text = if (programme.newness == Newness.NEW_SERIES) "NEW SERIES" else "NEW SEASON",
                    style = TextStyle(
                        color = SkyPalette.AccentBadge,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    ),
                )
            }
            if (hasReminder) {
                Spacer(Modifier.width(14.dp))
                BasicText(
                    text = "⏰ REMINDER SET",
                    style = TextStyle(
                        color = SkyPalette.ReminderBadge,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    ),
                )
            }
            if (isRecording) {
                Spacer(Modifier.width(14.dp))
                BasicText(
                    text = "● RECORDING",
                    style = TextStyle(
                        color = SkyPalette.RecordBadge,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    ),
                )
            }
        }

        BasicText(
            text = programme.title,
            style = TextStyle(
                color = SkyPalette.TextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        val meta = listOfNotNull(programme.episodeTitle, formatSeasonEpisode(programme))
            .joinToString(" ")
        val synopsis = listOf(programme.synopsis, meta)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        BasicText(
            text = synopsis.ifBlank { programme.genres.joinToString(" · ") },
            style = TextStyle(color = SkyPalette.TextSecondary, fontSize = 16.sp, lineHeight = 22.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
