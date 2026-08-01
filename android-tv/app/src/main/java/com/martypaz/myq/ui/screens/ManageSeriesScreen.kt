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
import com.martypaz.myq.data.model.Verdict
import com.martypaz.myq.ui.theme.SkyPalette

/** One series the user has an opinion about. */
data class SeriesOpinion(val title: String, val verdict: Verdict)

/**
 * Manage series: every title the user has loved or hated, with OK cycling
 * Love -> Hate -> cleared. This is where "I hate Love Island" gets undone.
 */
@Composable
fun ManageSeriesScreen(
    opinions: List<SeriesOpinion>,
    onCycle: (SeriesOpinion) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Manage series",
            subtitle = "Press OK to cycle Love → Hate → no opinion. Hated series never " +
                "appear in For You.",
        )

        if (opinions.isEmpty()) {
            EmptyState("No opinions yet. Press OK on a programme and choose Love or Hate.")
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(opinions.size, key = { opinions[it].title }) { index ->
                val opinion = opinions[index]
                ListRow(onClick = { onCycle(opinion) }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BasicText(
                            text = opinion.title,
                            style = TextStyle(
                                color = SkyPalette.TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val (label, colour) = when (opinion.verdict) {
                            Verdict.LOVE -> "♥ LOVED" to SkyPalette.ReminderBadge
                            Verdict.HATE -> "✕ HIDDEN" to SkyPalette.HateBadge
                            Verdict.NONE -> "—" to SkyPalette.CardFocused
                        }
                        BasicText(
                            text = label,
                            style = TextStyle(
                                color = Color(0xFF060B1D),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                            ),
                            modifier = Modifier
                                .background(colour, RoundedCornerShape(3.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}
