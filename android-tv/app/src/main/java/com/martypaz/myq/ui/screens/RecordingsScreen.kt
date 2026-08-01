package com.martypaz.myq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martypaz.myq.data.model.RecordEntry
import com.martypaz.myq.ui.formatStart
import com.martypaz.myq.ui.theme.SkyPalette

/**
 * The record list. MyQ cannot drive the television's tuner, so this is honest
 * about what it is: a saved list that reminds you, not a PVR schedule.
 */
@Composable
fun RecordingsScreen(
    recordings: List<RecordEntry>,
    onRemove: (RecordEntry) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Recordings",
            subtitle = "Saved in MyQ with a reminder before each start — MyQ can't " +
                "control your TV's built-in recorder.",
        )

        if (recordings.isEmpty()) {
            EmptyState("Nothing saved yet. Press OK on a programme and choose Record.")
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(recordings.size, key = { recordings[it].programmeId }) { index ->
                val entry = recordings[index]
                ListRow(onClick = { onRemove(entry) }) { isFocused ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BasicText(
                            text = entry.title,
                            style = TextStyle(
                                color = SkyPalette.TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (entry.isSeries) {
                            BasicText(
                                text = "SERIES",
                                style = TextStyle(
                                    color = Color(0xFF060B1D),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                modifier = Modifier
                                    .background(SkyPalette.RecordBadge, RoundedCornerShape(3.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    BasicText(
                        text = "${entry.channelName} · ${formatStart(entry.startMillis)}",
                        style = TextStyle(color = SkyPalette.TextTertiary, fontSize = 13.sp),
                    )
                    if (isFocused) {
                        BasicText(
                            text = "Press OK to remove",
                            style = TextStyle(color = SkyPalette.TextSecondary, fontSize = 12.sp),
                        )
                    }
                }
            }
        }
    }
}
