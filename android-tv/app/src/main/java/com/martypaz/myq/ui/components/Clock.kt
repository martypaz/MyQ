package com.martypaz.myq.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.martypaz.myq.ui.theme.SkyPalette
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The time and date, as a television is expected to show them.
 *
 * Ticks to the top of the next minute rather than every second, so it changes
 * exactly when the displayed minute does and costs one recomposition an hour's
 * worth of sixty.
 */
@Composable
fun Clock(modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(MILLIS_PER_MINUTE - now % MILLIS_PER_MINUTE)
            now = System.currentTimeMillis()
        }
    }

    val moment = Instant.ofEpochMilli(now)
    BasicText(
        text = "${TIME.format(moment)}  ·  ${DATE.format(moment)}",
        style = TextStyle(color = SkyPalette.TextTertiary, fontSize = 14.sp),
        modifier = modifier,
    )
}

private val TIME: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

private val DATE: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE d MMM").withZone(ZoneId.systemDefault())

private const val MILLIS_PER_MINUTE = 60_000L
