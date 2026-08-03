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
    val TopGlow = Color(0xFF0073E6)       // Sky Royal Blue top glow
    val UpperBlue = Color(0xFF0C4180)     // Upper royal blue
    val MidBlue = Color(0xFF0B2545)       // Deep mid blue
    val DeepNavy = Color(0xFF041029)      // Base deep blue field

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xDDF0F6FF)
    val TextTertiary = Color(0x99D0E2FF)

    /**
     * Legacy flat surfaces. New UI should prefer `Modifier.glass()`; these
     * remain for the few places that need an opaque backing.
     */
    val CardBackground = Color(0x2EFFFFFF)
    val CardFocused = Color(0x52FFFFFF)
    val FocusRing = Color(0xFFFFFFFF)

    /** Panel behind modal content — translucent royal blue glass. */
    val ScrimDeep = Color(0xEB0A1D3A)

    val AccentBadge = Color(0xFFFFB300)   // Sky highlight gold
    val ReminderBadge = Color(0xFF00E5FF) // Sky bright cyan
    val RecordBadge = Color(0xFFFF3D00)   // PVR record red
    val HateBadge = Color(0x9994A3B8)     // Muted state badge
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
