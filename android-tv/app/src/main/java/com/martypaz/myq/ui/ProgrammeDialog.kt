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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martypaz.myq.data.model.Programme
import com.martypaz.myq.data.model.Verdict
import com.martypaz.myq.ui.theme.SkyPalette
import com.martypaz.myq.ui.components.glass

private val LEAD_HOUR_OPTIONS = listOf(1, 2, 4, 24)

/** Everything the user can do to a programme, gathered in one D-pad overlay. */
data class ProgrammeDialogState(
    val programme: Programme,
    val existingLeadHours: Int? = null,
    val verdict: Verdict = Verdict.NONE,
    val isRecording: Boolean = false,
    val isSeriesRecording: Boolean = false,
)

@Composable
fun ProgrammeDialog(
    state: ProgrammeDialogState,
    onSetReminder: (Programme, Int) -> Unit,
    onRemoveReminder: (String) -> Unit,
    onSetVerdict: (Programme, Verdict) -> Unit,
    onRecord: (Programme, Boolean) -> Unit,
    onCancelRecord: (Programme, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val programme = state.programme
    val firstChipFocus = remember { FocusRequester() }
    LaunchedEffect(programme.id) { firstChipFocus.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyPalette.ScrimDeep),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 780.dp)
                .glass(shape = RoundedCornerShape(20.dp))
                .padding(horizontal = 36.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(
                text = programme.title,
                style = TextStyle(
                    color = SkyPalette.TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = "${programme.channelName} · ${formatStart(programme.startMillis)}",
                style = TextStyle(color = SkyPalette.TextTertiary, fontSize = 14.sp),
            )

            SectionLabel("Remind me")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LEAD_HOUR_OPTIONS.forEachIndexed { index, hours ->
                    Chip(
                        label = if (hours == 1) "1 hour" else "$hours hours",
                        emphasised = hours == state.existingLeadHours,
                        modifier = if (index == 0) Modifier.focusRequester(firstChipFocus) else Modifier,
                    ) { onSetReminder(programme, hours) }
                }
                if (state.existingLeadHours != null) {
                    Chip(label = "Clear") { onRemoveReminder(programme.id) }
                }
            }

            SectionLabel("Record")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip(
                    label = if (state.isRecording) "✓ Recording" else "Record",
                    emphasised = state.isRecording,
                ) {
                    if (state.isRecording) onCancelRecord(programme, false) else onRecord(programme, false)
                }
                Chip(
                    label = if (state.isSeriesRecording) "✓ Series" else "Record series",
                    emphasised = state.isSeriesRecording,
                ) {
                    if (state.isSeriesRecording) onCancelRecord(programme, true) else onRecord(programme, true)
                }
            }

            SectionLabel("Show me more or less of this")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                BasicText(
                    text = "Hidden from For You. Change it any time under Manage series.",
                    style = TextStyle(color = SkyPalette.TextTertiary, fontSize = 12.sp),
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    BasicText(
        text = text.uppercase(),
        style = TextStyle(
            color = SkyPalette.TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp,
        ),
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

    BasicText(
        text = label,
        style = TextStyle(
            color = if (isFocused) Color(0xFF060B1D) else SkyPalette.TextPrimary,
            fontSize = 15.sp,
            fontWeight = if (emphasised || isFocused) FontWeight.Bold else FontWeight.Medium,
        ),
        modifier = modifier
            .border(
                width = 2.dp,
                color = if (emphasised) accent else Color.Transparent,
                shape = RoundedCornerShape(24.dp),
            )
            .then(
                if (isFocused) {
                    Modifier.background(SkyPalette.TextPrimary, RoundedCornerShape(24.dp))
                } else {
                    Modifier.glass(shape = RoundedCornerShape(24.dp))
                },
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    )
}
