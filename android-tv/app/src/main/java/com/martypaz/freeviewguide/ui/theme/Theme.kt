package com.martypaz.freeviewguide.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Sky Q-inspired palette: a deep navy vertical gradient with a lighter
 * "glow" band near the top, white text, and translucent white cards.
 */
object SkyPalette {
    val TopGlow = Color(0xFF2C5AB8)
    val UpperBlue = Color(0xFF1B3B7A)
    val MidBlue = Color(0xFF122457)
    val DeepNavy = Color(0xFF080F26)

    val TextPrimary = Color(0xFFF4F7FF)
    val TextSecondary = Color(0xB3E6EDFF)
    val TextTertiary = Color(0x80D7E1FF)

    val CardBackground = Color(0x22FFFFFF)
    val CardFocused = Color(0x33FFFFFF)
    val FocusRing = Color(0xFFFFFFFF)

    val AccentBadge = Color(0xFFF2A93B)   // "New series" amber, like Sky's highlight chips
    val ReminderBadge = Color(0xFF41C7C7) // teal for reminder-set state
}

@Composable
fun FreeviewGuideTheme(content: @Composable () -> Unit) {
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
