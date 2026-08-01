package com.martypaz.myq.ui.screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martypaz.myq.data.prefs.Profile
import com.martypaz.myq.ui.theme.SkyPalette

private val LEAD_HOUR_OPTIONS = listOf(1, 2, 4, 24)

@Composable
fun SettingsScreen(
    profile: Profile,
    isLiveData: Boolean,
    onNameChange: (String) -> Unit,
    onDefaultLeadChange: (Int) -> Unit,
    onRefresh: () -> Unit,
    onClearTaste: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        ScreenHeader(title = "Settings")

        SettingBlock("Your name", "Used on the welcome screen.") {
            var name by remember(profile.firstName) { mutableStateOf(profile.firstName.orEmpty()) }
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()

            BasicTextField(
                value = name,
                onValueChange = {
                    name = it.take(24)
                    onNameChange(name)
                },
                singleLine = true,
                interactionSource = interactionSource,
                textStyle = TextStyle(color = SkyPalette.TextPrimary, fontSize = 18.sp),
                cursorBrush = SolidColor(SkyPalette.TextPrimary),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .widthIn(max = 380.dp)
                            .fillMaxWidth()
                            .background(SkyPalette.CardBackground, RoundedCornerShape(8.dp))
                            .border(
                                width = 2.dp,
                                color = if (isFocused) SkyPalette.FocusRing else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        if (name.isEmpty()) {
                            BasicText(
                                text = "First name",
                                style = TextStyle(color = SkyPalette.TextTertiary, fontSize = 18.sp),
                            )
                        }
                        inner()
                    }
                },
            )
        }

        SettingBlock("Default reminder", "Pre-selected when you set a new reminder.") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LEAD_HOUR_OPTIONS.forEach { hours ->
                    SettingChip(
                        label = if (hours == 1) "1 hour" else "$hours hours",
                        emphasised = hours == profile.defaultLeadHours,
                    ) { onDefaultLeadChange(hours) }
                }
            }
        }

        SettingBlock(
            "Listings",
            if (isLiveData) {
                "Live listings loaded from TVmaze."
            } else {
                "Offline — showing sample listings. Check the TV's network connection."
            },
        ) {
            SettingChip(label = "Refresh listings") { onRefresh() }
        }

        SettingBlock(
            "Recommendations",
            "For You learns from what you browse, open and set reminders for. " +
                "Everything stays on this device.",
        ) {
            SettingChip(label = "Reset what MyQ has learned") { onClearTaste() }
        }
    }
}

@Composable
private fun SettingBlock(title: String, subtitle: String, content: @Composable () -> Unit) {
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
            style = TextStyle(color = SkyPalette.TextTertiary, fontSize = 13.sp),
        )
        content()
    }
}

@Composable
private fun SettingChip(label: String, emphasised: Boolean = false, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    BasicText(
        text = label,
        style = TextStyle(
            color = if (isFocused) Color(0xFF060B1D) else SkyPalette.TextPrimary,
            fontSize = 15.sp,
            fontWeight = if (emphasised || isFocused) FontWeight.Bold else FontWeight.Medium,
        ),
        modifier = Modifier
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
            .padding(horizontal = 18.dp, vertical = 10.dp),
    )
}
