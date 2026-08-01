package com.martypaz.myq.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.martypaz.myq.data.model.Verdict
import com.martypaz.myq.data.streaming.StreamingApp
import com.martypaz.myq.data.tv.TvChannel
import com.martypaz.myq.ui.components.LeadTimeDropdown
import com.martypaz.myq.ui.components.glass
import com.martypaz.myq.ui.theme.SkyPalette

/** Everything the user can do to a programme, gathered in one D-pad overlay. */
data class ProgrammeDialogState(
    val programme: Programme,
    val existingLeadMinutes: Int? = null,
    val verdict: Verdict = Verdict.NONE,
    val isRecording: Boolean = false,
    val isSeriesRecording: Boolean = false,
    /** The tuner channel carrying this programme, when the box receives it. */
    val tunableChannel: TvChannel? = null,
    /** True while the programme is actually on, which is when tuning helps. */
    val isOnNow: Boolean = false,
    /** The service carrying this channel, when MyQ knows of one. */
    val streamingApp: StreamingApp? = null,
)

/**
 * The programme overlay: artwork and full details across the top, then the
 * actions grouped by intent — watch it now, be reminded, keep it, tune the
 * recommendations. Watch leads because it is the only one that ends with the
 * viewer actually watching something.
 */
@Composable
fun ProgrammeDialog(
    state: ProgrammeDialogState,
    onOpenInApp: (Programme, StreamingApp) -> Unit,
    onTune: (TvChannel) -> Unit,
    onSetReminder: (Programme, Int) -> Unit,
    onRemoveReminder: (String) -> Unit,
    onSetVerdict: (Programme, Verdict) -> Unit,
    onRecord: (Programme, Boolean) -> Unit,
    onCancelRecord: (Programme, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val programme = state.programme
    val firstAction = remember { FocusRequester() }
    LaunchedEffect(programme.id) { firstAction.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyPalette.ScrimDeep),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .heightIn(max = 620.dp)
                .glass(shape = RoundedCornerShape(20.dp))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 36.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Header(state)

            // Tuning is only offered while the programme is actually on. At
            // any other time the channel would be showing something else, and
            // a reminder is the right answer instead.
            val tunable = state.tunableChannel.takeIf { state.isOnNow }
            if (tunable != null || state.streamingApp != null) {
                Section("Watch") {
                    tunable?.let { channel ->
                        val number = channel.number?.let { " ($it)" }.orEmpty()
                        Chip(
                            label = "▶  Switch to ${channel.displayName}$number",
                            emphasised = true,
                            accent = SkyPalette.AccentBadge,
                            modifier = Modifier.focusRequester(firstAction),
                        ) { onTune(channel) }
                    }
                    state.streamingApp?.let { app ->
                        Chip(
                            label = "▶  Open in ${app.displayName}",
                            emphasised = tunable == null,
                            accent = SkyPalette.AccentBadge,
                            modifier = if (tunable == null) {
                                Modifier.focusRequester(firstAction)
                            } else {
                                Modifier
                            },
                        ) { onOpenInApp(programme, app) }
                    }
                }
            }

            Section("Remind me") {
                LeadTimeDropdown(
                    selectedMinutes = state.existingLeadMinutes,
                    onSelect = { minutes -> onSetReminder(programme, minutes) },
                    onClear = { onRemoveReminder(programme.id) },
                    modifier = if (state.streamingApp == null && state.tunableChannel == null) {
                        Modifier.focusRequester(firstAction)
                    } else {
                        Modifier
                    },
                )
            }

            Section("Record") {
                Chip(
                    label = if (state.isRecording) "✓ Recording" else "Record",
                    emphasised = state.isRecording,
                    accent = SkyPalette.RecordBadge,
                ) {
                    if (state.isRecording) onCancelRecord(programme, false) else onRecord(programme, false)
                }
                Chip(
                    label = if (state.isSeriesRecording) "✓ Series" else "Record series",
                    emphasised = state.isSeriesRecording,
                    accent = SkyPalette.RecordBadge,
                ) {
                    if (state.isSeriesRecording) onCancelRecord(programme, true) else onRecord(programme, true)
                }
            }

            Section("Show me more or less of this") {
                Chip(
                    label = if (state.verdict == Verdict.LOVE) "♥ Loved" else "♥ Love",
                    emphasised = state.verdict == Verdict.LOVE,
                    accent = SkyPalette.ReminderBadge,
                ) {
                    val next = if (state.verdict == Verdict.LOVE) Verdict.NONE else Verdict.LOVE
                    onSetVerdict(programme, next)
                }
                Chip(
                    label = if (state.verdict == Verdict.HATE) "✕ Hidden" else "✕ Hate",
                    emphasised = state.verdict == Verdict.HATE,
                    accent = SkyPalette.HateBadge,
                ) {
                    val next = if (state.verdict == Verdict.HATE) Verdict.NONE else Verdict.HATE
                    onSetVerdict(programme, next)
                }
                Chip(label = "Close") { onDismiss() }
            }

            if (state.verdict == Verdict.HATE) {
                Footnote("Hidden from For You. Change it any time under Manage series.")
            }
        }
    }
}

/** Artwork beside the full billing — what the rail card had no room to say. */
@Composable
private fun Header(state: ProgrammeDialogState) {
    val programme = state.programme

    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Box(
            modifier = Modifier
                .size(width = 232.dp, height = 131.dp)
                .glass(shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (programme.imageUrl != null) {
                AsyncImage(
                    model = programme.imageUrl,
                    contentDescription = programme.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // No artwork: the channel ident still tells the viewer where they are.
                BasicText(
                    text = programme.channelName,
                    style = TextStyle(
                        color = SkyPalette.TextTertiary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MetaLine(state)

            BasicText(
                text = programme.title,
                style = TextStyle(
                    color = SkyPalette.TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            val episode = listOfNotNull(programme.episodeTitle, formatSeasonEpisode(programme))
                .joinToString("  ")
            if (episode.isNotBlank()) {
                BasicText(
                    text = episode,
                    style = TextStyle(color = SkyPalette.TextSecondary, fontSize = 16.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (programme.synopsis.isNotBlank()) {
                BasicText(
                    text = programme.synopsis,
                    style = TextStyle(
                        color = SkyPalette.TextSecondary,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                    ),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (programme.genres.isNotEmpty()) {
                BasicText(
                    text = programme.genres.joinToString(" · "),
                    style = TextStyle(color = SkyPalette.TextTertiary, fontSize = 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Channel, time and runtime, then whatever state badges apply. */
@Composable
private fun MetaLine(state: ProgrammeDialogState) {
    val programme = state.programme
    val facts = listOfNotNull(
        programme.channelName.uppercase(),
        formatStart(programme.startMillis),
        programme.runtimeMinutes?.let { "${it}m" },
    ).joinToString("  ·  ")

    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            text = facts,
            style = TextStyle(
                color = SkyPalette.TextTertiary,
                fontSize = 13.sp,
                letterSpacing = 0.8.sp,
            ),
        )
        when (programme.newness) {
            Newness.NEW_SERIES -> Badge("NEW SERIES", SkyPalette.AccentBadge)
            Newness.NEW_SEASON -> Badge("NEW SEASON", SkyPalette.AccentBadge)
            else -> Unit
        }
        if (state.existingLeadMinutes != null) Badge("⏰ REMINDER SET", SkyPalette.ReminderBadge)
        if (state.isRecording || state.isSeriesRecording) Badge("● RECORDING", SkyPalette.RecordBadge)
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Spacer(Modifier.width(12.dp))
    BasicText(
        text = text,
        style = TextStyle(
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        ),
    )
}

/** A labelled band of chips — the repeating unit the whole overlay is built from. */
@Composable
private fun Section(label: String, chips: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        BasicText(
            text = label.uppercase(),
            style = TextStyle(
                color = SkyPalette.TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.6.sp,
            ),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { chips() }
    }
}

@Composable
private fun Footnote(text: String) {
    BasicText(
        text = text,
        style = TextStyle(color = SkyPalette.TextTertiary, fontSize = 12.sp),
    )
}

@Composable
private fun Chip(
    label: String,
    modifier: Modifier = Modifier,
    emphasised: Boolean = false,
    accent: Color = SkyPalette.ReminderBadge,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(24.dp)

    BasicText(
        text = label,
        style = TextStyle(
            color = if (isFocused) Color(0xFF060B1D) else SkyPalette.TextPrimary,
            fontSize = 15.sp,
            fontWeight = if (emphasised || isFocused) FontWeight.Bold else FontWeight.Medium,
        ),
        maxLines = 1,
        modifier = modifier
            .then(
                if (isFocused) {
                    Modifier.background(SkyPalette.TextPrimary, shape)
                } else {
                    Modifier.glass(shape = shape)
                },
            )
            // Drawn after the glass so the accent reads as a ring on the pane
            // rather than a halo floating outside it.
            .border(
                width = 2.dp,
                color = if (emphasised && !isFocused) accent else Color.Transparent,
                shape = shape,
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    )
}
