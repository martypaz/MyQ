package com.martypaz.myq.reminders

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.martypaz.myq.ui.components.glass
import com.martypaz.myq.ui.theme.MyQTheme
import com.martypaz.myq.ui.theme.SkyPalette
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The reminder as a full-screen card over whatever is currently on.
 *
 * Android TV has no heads-up notifications — a posted notification only lands
 * in the launcher's notification row, where nobody watching television will
 * see it. A full-screen intent is the one sanctioned way for a background app
 * to put something in front of the viewer, so this activity is what the
 * notification points at.
 */
class ReminderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val alert = ReminderAlert.from(intent)
        if (alert == null) {
            finish()
            return
        }

        ReminderTrace.recordAlertShown(this)

        // The viewer is looking at it; the notification copy is now noise.
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(alert.notificationId)

        setContent {
            MyQTheme {
                ReminderAlertScreen(alert = alert, onDismiss = ::finish)
            }
        }
    }
}

@Composable
private fun ReminderAlertScreen(alert: ReminderAlert, onDismiss: () -> Unit) {
    val dismiss = remember { FocusRequester() }
    LaunchedEffect(Unit) { dismiss.requestFocus() }

    // Never hold the screen hostage: an unattended alert clears itself.
    LaunchedEffect(alert.programmeId) {
        delay(AUTO_DISMISS_MILLIS)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyPalette.ScrimDeep),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .glass(shape = RoundedCornerShape(20.dp))
                .padding(horizontal = 44.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(
                text = "STARTING ${alert.leadLabel().uppercase()}",
                style = TextStyle(
                    color = SkyPalette.ReminderBadge,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                ),
            )
            BasicText(
                text = alert.title,
                style = TextStyle(
                    color = SkyPalette.TextPrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = listOf(alert.channelName, formatStartTime(alert.startMillis))
                    .filter { it.isNotBlank() }
                    .joinToString("  ·  "),
                style = TextStyle(color = SkyPalette.TextSecondary, fontSize = 17.sp),
            )

            DismissButton(modifier = Modifier.focusRequester(dismiss), onClick = onDismiss)
        }
    }
}

@Composable
private fun DismissButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(24.dp)

    BasicText(
        text = "Dismiss",
        style = TextStyle(
            color = if (isFocused) Color(0xFF060B1D) else SkyPalette.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        modifier = modifier
            .padding(top = 8.dp)
            .then(
                if (isFocused) {
                    Modifier.background(SkyPalette.TextPrimary, shape)
                } else {
                    Modifier.glass(shape = shape)
                },
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 30.dp, vertical = 12.dp),
    )
}

private fun formatStartTime(startMillis: Long): String =
    if (startMillis <= 0L) {
        ""
    } else {
        DateTimeFormatter.ofPattern("EEE HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(startMillis))
    }

private const val AUTO_DISMISS_MILLIS = 60_000L
