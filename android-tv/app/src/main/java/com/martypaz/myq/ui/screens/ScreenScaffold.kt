package com.martypaz.myq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martypaz.myq.ui.theme.SkyPalette
import com.martypaz.myq.ui.components.glass

/** Shared page header so every secondary screen reads the same way. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 18.dp),
    ) {
        BasicText(
            text = title,
            style = TextStyle(
                color = SkyPalette.TextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        subtitle?.let {
            BasicText(
                text = it,
                style = TextStyle(color = SkyPalette.TextSecondary, fontSize = 15.sp),
            )
        }
    }
}

@Composable
fun EmptyState(message: String) {
    BasicText(
        text = message,
        style = TextStyle(color = SkyPalette.TextTertiary, fontSize = 16.sp),
        modifier = Modifier.padding(vertical = 24.dp),
    )
}

/** A full-width focusable row, the workhorse of the list screens. */
@Composable
fun ListRow(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable (isFocused: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glass(focused = isFocused, shape = RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        content(isFocused)
    }
}
