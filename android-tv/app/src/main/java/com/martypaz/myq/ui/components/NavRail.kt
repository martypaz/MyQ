package com.martypaz.myq.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martypaz.myq.ui.theme.SkyPalette

/** The sections reachable from the left-hand navigation. */
enum class NavDestination(val label: String, val glyph: String) {
    HOME("Home", "⌂"),          // house
    SEARCH("Search", "⌕"),      // magnifier
    RECORDINGS("Recordings", "●"),
    SERIES("Manage series", "☰"),
    SETTINGS("Settings", "⚙"),
}

/**
 * Sky Q-style left rail: a narrow strip of glyphs that expands to show labels
 * while any of its items has focus, then collapses back out of the way.
 */
@Composable
fun NavRail(
    selected: NavDestination,
    onSelect: (NavDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .onFocusChanged { expanded = it.hasFocus }
            .animateContentSize(animationSpec = tween(durationMillis = 200))
            .width(if (expanded) 210.dp else 68.dp)
            .background(
                color = if (expanded) Color(0xCC0A142F) else Color.Transparent,
                shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
            )
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        NavDestination.entries.forEach { destination ->
            NavItem(
                destination = destination,
                expanded = expanded,
                isSelected = destination == selected,
                onSelect = { onSelect(destination) },
            )
        }
    }
}

@Composable
private fun NavItem(
    destination: NavDestination,
    expanded: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val background by animateColorAsState(
        targetValue = when {
            isFocused -> SkyPalette.TextPrimary
            isSelected -> SkyPalette.CardFocused
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 160),
        label = "navItemBackground",
    )
    val contentColor = when {
        isFocused -> Color(0xFF060B1D)
        isSelected -> SkyPalette.TextPrimary
        else -> SkyPalette.TextSecondary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .background(background, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            BasicText(
                text = destination.glyph,
                style = TextStyle(color = contentColor, fontSize = 19.sp),
            )
        }
        if (expanded) {
            BasicText(
                text = destination.label,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                ),
            )
        }
    }
}
