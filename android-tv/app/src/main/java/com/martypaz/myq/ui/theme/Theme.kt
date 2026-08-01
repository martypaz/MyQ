package com.martypaz.myq.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Sky Q-inspired palette rendered as glassmorphism: a deep navy field with
 * soft pools of colour, over which every surface is a frosted translucent
 * pane. Solid fills are reserved for badges, which must stay legible.
 */
object SkyPalette {
    val TopGlow = Color(0xFF2C5AB8)
    val UpperBlue = Color(0xFF1B3B7A)
    val MidBlue = Color(0xFF122457)
    val DeepNavy = Color(0xFF080F26)

    val TextPrimary = Color(0xFFF4F7FF)
    val TextSecondary = Color(0xCCE6EDFF)
    val TextTertiary = Color(0x99D7E1FF)

    /**
     * Legacy flat surfaces. New UI should prefer `Modifier.glass()`; these
     * remain for the few places that need an opaque backing.
     */
    val CardBackground = Color(0x1FFFFFFF)
    val CardFocused = Color(0x3DFFFFFF)
    val FocusRing = Color(0xFFFFFFFF)

    /** Panel behind modal content — darker so the glass above it reads. */
    val ScrimDeep = Color(0xE60A1024)

    val AccentBadge = Color(0xFFF2A93B)   // "New series" amber, like Sky's highlight chips
    val ReminderBadge = Color(0xFF41C7C7) // teal for reminder-set state
    val RecordBadge = Color(0xFFE2574C)   // record red, the universal PVR colour
    val HateBadge = Color(0xFF8E9BC4)     // muted: hiding something is not an alarm
}

@Composable
fun MyQTheme(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        SkyPalette.TopGlow,
                        SkyPalette.UpperBlue,
                        SkyPalette.MidBlue,
                        SkyPalette.DeepNavy,
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(0f, Float.POSITIVE_INFINITY),
                ),
            ),
    ) {
        content()
    }
}
