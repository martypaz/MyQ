package com.martypaz.myq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martypaz.myq.data.account.readDeviceGoogleAccount
import com.martypaz.myq.reminders.ReminderReadiness
import com.martypaz.myq.reminders.ReminderTrace
import com.martypaz.myq.reminders.openExactAlarmSettings
import com.martypaz.myq.reminders.openFullScreenAlertSettings
import com.martypaz.myq.reminders.openNotificationSettings
import com.martypaz.myq.ui.components.glass
import com.martypaz.myq.ui.theme.SkyPalette

/**
 * Tools for working out why a reminder did not appear.
 *
 * Three things have to happen in order — the alarm fires, the notification
 * posts, the full-screen alert opens — and each fails silently on its own.
 * This screen separates them: [onShowAlertNow] opens the alert directly,
 * proving the last link works, while [onTestReminder] goes the whole way round
 * through a real alarm. If the direct one appears and the timed one does not,
 * the alert is fine and the notification path is not.
 */
@Composable
fun DeveloperScreen(
    readiness: ReminderReadiness?,
    message: String?,
    onTestReminder: () -> Unit,
    onShowAlertNow: () -> Unit,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    val trace = ReminderTrace.read(context)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        ScreenHeader(title = "Developer")

        message?.let { Note(it) }

        DeveloperBlock(
            "Test the alert on its own",
            "Opens the reminder alert directly, without an alarm or a notification. " +
                "If this works, the alert itself is fine.",
        ) {
            DeveloperChip("Show the alert now") { onShowAlertNow() }
        }

        DeveloperBlock(
            "Test the whole chain",
            "Arms a real alarm 15 seconds out. Leave MyQ once it is armed — a " +
                "full-screen alert is usually suppressed while the app that posted " +
                "it is already in front of you, so staying here can look like a failure.",
        ) {
            DeveloperChip("Test reminder in 15s") { onTestReminder() }
        }

        DeveloperBlock(
            "Device account",
            readDeviceGoogleAccount(context).summary,
        ) {
            DeveloperChip("Re-check") { onRefresh() }
        }

        DeveloperBlock("What happened last time", trace.describe()) {
            DeveloperChip("Refresh") { onRefresh() }
        }

        readiness?.let { state ->
            DeveloperBlock(
                "Permissions",
                state.blockers.firstOrNull() ?: "Everything a reminder needs is allowed.",
            ) {
                if (!state.notificationsEnabled) {
                    DeveloperChip("Turn on notifications") { context.openNotificationSettings() }
                }
                if (!state.fullScreenAlertsAllowed) {
                    DeveloperChip("Allow full-screen alerts") { context.openFullScreenAlertSettings() }
                }
                if (!state.exactAlarmsAllowed) {
                    DeveloperChip("Allow exact alarms") { context.openExactAlarmSettings() }
                }
            }
            PermissionList(state)
        }
    }
}

@Composable
private fun PermissionList(state: ReminderReadiness) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PermissionRow("Notifications", state.notificationsEnabled)
        PermissionRow("Full-screen alerts", state.fullScreenAlertsAllowed)
        PermissionRow("Exact alarms", state.exactAlarmsAllowed)
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    BasicText(
        text = if (granted) "✓  $label" else "✕  $label",
        style = TextStyle(
            color = if (granted) SkyPalette.ReminderBadge else SkyPalette.RecordBadge,
            fontSize = 14.sp,
        ),
    )
}

@Composable
private fun Note(text: String) {
    BasicText(
        text = text,
        style = TextStyle(color = SkyPalette.AccentBadge, fontSize = 15.sp, lineHeight = 21.sp),
    )
}

@Composable
private fun DeveloperBlock(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(
            text = title,
            style = TextStyle(
                color = SkyPalette.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        BasicText(
            text = subtitle,
            style = TextStyle(color = SkyPalette.TextTertiary, fontSize = 13.sp, lineHeight = 19.sp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}

@Composable
private fun DeveloperChip(label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(24.dp)

    BasicText(
        text = label,
        style = TextStyle(
            color = if (isFocused) Color(0xFF060B1D) else SkyPalette.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        ),
        modifier = Modifier
            .then(
                if (isFocused) {
                    Modifier.background(SkyPalette.TextPrimary, shape)
                } else {
                    Modifier.glass(shape = shape)
                },
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    )
}
