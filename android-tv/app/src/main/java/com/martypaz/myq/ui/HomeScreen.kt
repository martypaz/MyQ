package com.martypaz.myq.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.martypaz.myq.data.model.Rail
import com.martypaz.myq.ui.components.HeroPanel
import com.martypaz.myq.ui.components.ProgrammeCard
import com.martypaz.myq.ui.theme.SkyPalette

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 27.dp), // TV overscan-safe margins
        ) {
            HeroPanel(
                programme = state.heroProgramme,
                hasReminder = state.heroProgramme?.let { it.id in state.reminders } == true,
            )

            Spacer(Modifier.height(12.dp))

            when {
                state.isLoading && state.rails.isEmpty() -> LoadingHint()
                else -> RailList(state, viewModel)
            }
        }

        if (!state.isLiveData && !state.isLoading) {
            OfflineBanner(modifier = Modifier.align(Alignment.TopEnd).padding(24.dp))
        }

        state.reminderTarget?.let { target ->
            ReminderDialog(
                programme = target,
                existingLeadHours = state.reminders[target.id]?.leadHours,
                onConfirm = viewModel::setReminder,
                onRemove = viewModel::removeReminder,
                onDismiss = viewModel::dismissReminderDialog,
            )
        }
    }
}

@Composable
private fun RailList(state: HomeUiState, viewModel: HomeViewModel) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 40.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(state.rails.size, key = { state.rails[it].id }) { index ->
            RailRow(
                rail = state.rails[index],
                state = state,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun RailRow(rail: Rail, state: HomeUiState, viewModel: HomeViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BasicText(
            text = rail.title,
            style = TextStyle(
                color = SkyPalette.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier.padding(start = 8.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        ) {
            items(rail.programmes.size, key = { "${rail.id}-${rail.programmes[it].id}" }) { index ->
                val programme = rail.programmes[index]
                ProgrammeCard(
                    programme = programme,
                    hasReminder = programme.id in state.reminders,
                    onFocused = viewModel::onProgrammeFocused,
                    onSelected = viewModel::onProgrammeSelected,
                )
            }
        }
    }
}

@Composable
private fun LoadingHint() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(
            text = "Fetching tonight's listings…",
            style = TextStyle(color = SkyPalette.TextSecondary, fontSize = 18.sp),
        )
    }
}

@Composable
private fun OfflineBanner(modifier: Modifier = Modifier) {
    BasicText(
        text = "Offline — showing sample listings",
        style = TextStyle(color = Color(0xFF060B1D), fontSize = 12.sp, fontWeight = FontWeight.Bold),
        modifier = modifier
            .background(SkyPalette.AccentBadge, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
