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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martypaz.myq.data.model.Programme
import com.martypaz.myq.ui.theme.SkyPalette

private val LEAD_HOUR_OPTIONS = listOf(1, 2, 4, 24)

/**
 * Full-screen overlay asking how many hours before the start the reminder
 * should fire. D-pad drives the chips; Back dismisses.
 */
@Composable
fun ReminderDialog(
    programme: Programme,
    existingLeadHours: Int?,
    onConfirm: (Programme, Int) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val firstChipFocus = remember { FocusRequester() }
    LaunchedEffect(programme.id) { firstChipFocus.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC050A1A)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .background(SkyPalette.MidBlue, RoundedCornerShape(10.dp))
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(
                text = "Remind me about",
                style = TextStyle(color = SkyPalette.TextSecondary, fontSize = 15.sp),
            )
            BasicText(
                text = programme.title,
                style = TextStyle(
                    color = SkyPalette.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            BasicText(
                text = "${programme.channelName} · ${formatStart(programme.startMillis)}",
                style = TextStyle(color = SkyPalette.TextTertiary, fontSize = 14.sp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LEAD_HOUR_OPTIONS.forEachIndexed { index, hours ->
                    DialogChip(
                        label = if (hours == 1) "1 hour before" else "$hours hours before",
                        emphasised = hours == existingLeadHours,
                        modifier = if (index == 0) Modifier.focusRequester(firstChipFocus) else Modifier,
                    ) { onConfirm(programme, hours) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (existingLeadHours != null) {
                    DialogChip(label = "Remove reminder") { onRemove(programme.id) }
                }
                DialogChip(label = "Cancel") { onDismiss() }
            }
        }
    }
}

@Composable
private fun DialogChip(
    label: String,
    modifier: Modifier = Modifier,
    emphasised: Boolean = false,
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
                color = if (emphasised) SkyPalette.ReminderBadge else Color.Transparent,
                shape = RoundedCornerShape(24.dp),
            )
            .background(
                color = if (isFocused) SkyPalette.TextPrimary else SkyPalette.CardBackground,
                shape = RoundedCornerShape(24.dp),
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    )
}
