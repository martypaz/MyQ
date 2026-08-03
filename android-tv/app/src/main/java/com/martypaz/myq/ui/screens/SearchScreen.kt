package com.martypaz.myq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martypaz.myq.data.epg.deduplicateSoonest
import com.martypaz.myq.data.model.Platform
import com.martypaz.myq.data.model.Programme
import com.martypaz.myq.ui.formatStart
import com.martypaz.myq.ui.theme.SkyPalette
import com.martypaz.myq.ui.components.glass

/** Search across everything loaded — Freeview and streaming alike. */
@Composable
fun SearchScreen(
    query: String,
    results: List<Programme>,
    onQueryChange: (String) -> Unit,
    onSelect: (Programme) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Search",
            subtitle = "Titles, channels and genres across Freeview and streaming",
        )

        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            interactionSource = interactionSource,
            textStyle = TextStyle(color = SkyPalette.TextPrimary, fontSize = 20.sp),
            cursorBrush = SolidColor(SkyPalette.TextPrimary),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glass(focused = isFocused, shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (query.isEmpty()) {
                        BasicText(
                            text = "Search programmes…",
                            style = TextStyle(color = SkyPalette.TextTertiary, fontSize = 20.sp),
                        )
                    }
                    inner()
                }
            },
        )

        when {
            query.isBlank() -> EmptyState("Start typing to search the next few days of listings.")
            results.isEmpty() -> EmptyState("Nothing matching \"$query\".")
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                items(results.size, key = { results[it].id }) { index ->
                    val programme = results[index]
                    ListRow(onClick = { onSelect(programme) }) {
                        BasicText(
                            text = programme.title,
                            style = TextStyle(
                                color = SkyPalette.TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val platform = if (programme.platform == Platform.STREAMING) "Streaming" else "Freeview"
                        BasicText(
                            text = "${programme.channelName} · ${formatStart(programme.startMillis)} · $platform",
                            style = TextStyle(color = SkyPalette.TextTertiary, fontSize = 13.sp),
                        )
                    }
                }
            }
        }
    }
}

/** Case-insensitive match across title, episode, channel and genres. */
fun searchProgrammes(programmes: List<Programme>, query: String): List<Programme> {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return emptyList()
    return programmes.filter { programme ->
        programme.title.lowercase().contains(needle) ||
            programme.episodeTitle?.lowercase()?.contains(needle) == true ||
            programme.channelName.lowercase().contains(needle) ||
            programme.genres.any { it.lowercase().contains(needle) }
    }.deduplicateSoonest().take(60)
}
