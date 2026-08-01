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
import com.martypaz.myq.data.epg.EpgRepository
import com.martypaz.myq.data.prefs.Profile
import com.martypaz.myq.ui.theme.SkyPalette
import com.martypaz.myq.ui.components.LeadTimeDropdown
import com.martypaz.myq.ui.components.glass

/**
 * MyQ merges two listings sources, and either can fail on its own. Saying
 * which answered turns "why is this channel missing?" into something the
 * viewer can actually see.
 */
private fun describeSources(isLiveData: Boolean, sources: Set<EpgRepository.Source>): String {
    if (!isLiveData) {
        return "Offline — showing sample listings. Check the TV's network connection."
    }
    val names = sources.sortedBy { it.ordinal }.map {
        when (it) {
            EpgRepository.Source.FREEVIEW_EPG -> "Freeview EPG (full listings)"
            EpgRepository.Source.DEVICE_TUNER -> "this TV's tuner"
            EpgRepository.Source.TVMAZE -> "TVmaze (genres and artwork)"
        }
    }
    return when {
        names.isEmpty() -> "Live listings loaded."
        names.size == 1 -> "Live listings from ${names[0]}. The other sources did not answer."
        else -> "Live listings from " + names.dropLast(1).joinToString(", ") + " and ${names.last()}."
    }
}

@Composable
fun SettingsScreen(
    profile: Profile,
    isLiveData: Boolean,
    sources: Set<EpgRepository.Source>,
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
                            .glass(focused = isFocused, shape = RoundedCornerShape(12.dp))
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

        SettingBlock(
            "Default reminder",
            "Used when a recording sets its own reminder, and pre-selected for a new one.",
        ) {
            LeadTimeDropdown(
                selectedMinutes = profile.defaultLeadMinutes,
                onSelect = onDefaultLeadChange,
            )
        }

        SettingBlock("Listings", describeSources(isLiveData, sources)) {
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
