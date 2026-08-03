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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.martypaz.myq.R
import com.martypaz.myq.ui.theme.SkyPalette

/** The sections reachable from the left-hand navigation. */
enum class NavDestination(val label: String, val glyph: String) {
    HOME("Home", "⌂"),          // house
    SEARCH("Search", "⌕"),      // magnifier
    RECORDINGS("Recordings", "●"),
    SERIES("Manage series", "☰"),
    SETTINGS("Settings", "⚙"),
    DEVELOPER("Developer", "⚑"),
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
    /** Developer is only listed when it is reachable. */
    destinations: List<NavDestination> = NavDestination.entries,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .onFocusChanged { expanded = it.hasFocus }
            .animateContentSize(animationSpec = tween(durationMillis = 200))
            .width(if (expanded) 210.dp else 68.dp)
            .then(
                // The rail only becomes a pane once it is in use; collapsed it
                // should not compete with the content beside it.
                if (expanded) Modifier.glass(shape = RoundedCornerShape(16.dp)) else Modifier,
            )
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                .height(26.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_myq_logo),
                contentDescription = "MyQ",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(if (expanded) 96.dp else 46.dp)
                    .height(24.dp),
            )
        }
        Spacer(Modifier.height(4.dp))

        destinations.forEach { destination ->
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

    val contentColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color(0xFF060B1D)
            isSelected -> SkyPalette.TextPrimary
            else -> SkyPalette.TextSecondary
        },
        animationSpec = tween(durationMillis = 160),
        label = "navItemContent",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .then(
                when {
                    // Focus is solid so the glyph inverts and reads instantly.
                    isFocused -> Modifier.background(SkyPalette.TextPrimary, RoundedCornerShape(10.dp))
                    isSelected -> Modifier.glass(shape = RoundedCornerShape(10.dp))
                    else -> Modifier
                },
            )
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
