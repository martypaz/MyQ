package com.martypaz.myq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martypaz.myq.data.model.LEAD_TIME_OPTIONS
import com.martypaz.myq.data.model.formatLeadTime
import com.martypaz.myq.ui.theme.SkyPalette

/**
 * Lead-time picker: a closed control that opens into a scrolling list.
 *
 * Thirteen options from five minutes to twelve hours will not fit in a row of
 * chips, and a row that long is a lot of D-pad presses to cross. Closed it
 * shows the current choice; open, the list starts on that choice so the common
 * case is one press.
 */
@Composable
fun LeadTimeDropdown(
    selectedMinutes: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DropdownButton(
            label = selectedMinutes?.let { "Reminder ${formatLeadTime(it)} before" }
                ?: "Choose when to be reminded",
            expanded = expanded,
            emphasised = selectedMinutes != null,
        ) { expanded = !expanded }

        if (expanded) {
            LeadTimeList(
                selectedMinutes = selectedMinutes,
                onSelect = {
                    expanded = false
                    onSelect(it)
                },
            )
        }

        if (selectedMinutes != null && onClear != null && !expanded) {
            DropdownButton(label = "Clear reminder", expanded = false) { onClear() }
        }
    }
}

@Composable
private fun LeadTimeList(selectedMinutes: Int?, onSelect: (Int) -> Unit) {
    val listState = rememberLazyListState()
    val selectedIndex = LEAD_TIME_OPTIONS.indexOf(selectedMinutes).takeIf { it >= 0 } ?: 0
    val firstItem = remember { FocusRequester() }

    // Open on the current choice rather than at the top of the list.
    LaunchedEffect(Unit) {
        listState.scrollToItem(selectedIndex)
        firstItem.requestFocus()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .width(DROPDOWN_WIDTH)
            .heightIn(max = DROPDOWN_MAX_HEIGHT)
            .glass(shape = RoundedCornerShape(12.dp))
            .padding(vertical = 6.dp),
    ) {
        items(LEAD_TIME_OPTIONS.size) { index ->
            val minutes = LEAD_TIME_OPTIONS[index]
            LeadTimeRow(
                minutes = minutes,
                selected = minutes == selectedMinutes,
                modifier = if (index == selectedIndex) Modifier.focusRequester(firstItem) else Modifier,
            ) { onSelect(minutes) }
        }
    }
}

@Composable
private fun LeadTimeRow(
    minutes: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    BasicText(
        text = if (selected) "✓  ${formatLeadTime(minutes)}" else formatLeadTime(minutes),
        style = TextStyle(
            color = if (isFocused) Color(0xFF060B1D) else SkyPalette.TextPrimary,
            fontSize = 15.sp,
            fontWeight = if (selected || isFocused) FontWeight.Bold else FontWeight.Medium,
        ),
        maxLines = 1,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .then(
                if (isFocused) {
                    Modifier.background(SkyPalette.TextPrimary, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                },
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

@Composable
private fun DropdownButton(
    label: String,
    expanded: Boolean,
    emphasised: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .width(DROPDOWN_WIDTH)
            .then(
                if (isFocused) {
                    Modifier.background(SkyPalette.TextPrimary, shape)
                } else {
                    Modifier.glass(shape = shape)
                },
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        BasicText(
            text = if (expanded) "$label  ▴" else "$label  ▾",
            style = TextStyle(
                color = if (isFocused) Color(0xFF060B1D) else SkyPalette.TextPrimary,
                fontSize = 15.sp,
                fontWeight = if (emphasised || isFocused) FontWeight.Bold else FontWeight.Medium,
            ),
            maxLines = 1,
        )
    }
}

private val DROPDOWN_WIDTH = 320.dp
private val DROPDOWN_MAX_HEIGHT = 260.dp
