package com.martypaz.myq.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.martypaz.myq.data.model.Rail
import com.martypaz.myq.ui.components.GlassBackdrop
import com.martypaz.myq.ui.components.HeroPanel
import com.martypaz.myq.ui.components.NavDestination
import com.martypaz.myq.ui.components.NavRail
import com.martypaz.myq.ui.components.ProgrammeCard
import com.martypaz.myq.ui.components.Clock
import com.martypaz.myq.ui.screens.DeveloperScreen
import com.martypaz.myq.ui.screens.ManageSeriesScreen
import com.martypaz.myq.ui.screens.RecordingsScreen
import com.martypaz.myq.ui.screens.SearchScreen
import com.martypaz.myq.ui.screens.SettingsScreen
import com.martypaz.myq.ui.theme.SkyPalette

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.showWelcome) {
        WelcomeScreen(
            firstName = state.profile.firstName,
            onNameEntered = viewModel::onNameEntered,
            onContinue = viewModel::onWelcomeFinished,
            isProfileLoaded = state.isProfileLoaded,
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Everything above is glass; this is what it refracts.
        GlassBackdrop(imageUrl = state.heroProgramme?.imageUrl)

        Row(modifier = Modifier.fillMaxSize()) {
            NavRail(
                selected = state.destination,
                onSelect = viewModel::onDestinationSelected,
                destinations = state.destinations,
                modifier = Modifier.padding(start = 12.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // TV overscan-safe margins on the content side only.
                    .padding(start = 20.dp, end = 48.dp, top = 27.dp, bottom = 27.dp),
            ) {
                when (state.destination) {
                    NavDestination.HOME -> HomeContent(state, viewModel)
                    NavDestination.SEARCH -> SearchScreen(
                        query = state.searchQuery,
                        results = state.searchResults,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onSelect = viewModel::onProgrammeSelected,
                    )
                    NavDestination.RECORDINGS -> RecordingsScreen(
                        recordings = state.recordings,
                        onRemove = viewModel::removeRecording,
                    )
                    NavDestination.SERIES -> ManageSeriesScreen(
                        opinions = state.opinions,
                        onCycle = viewModel::cycleVerdict,
                    )
                    NavDestination.DEVELOPER -> DeveloperScreen(
                        readiness = state.reminderReadiness,
                        message = state.developerMessage,
                        onTestReminder = viewModel::fireTestReminder,
                        onShowAlertNow = viewModel::showAlertNow,
                        onRefresh = viewModel::refreshReminderReadiness,
                    )
                    NavDestination.SETTINGS -> SettingsScreen(
                        profile = state.profile,
                        isLiveData = state.isLiveData,
                        sources = state.sources,
                        onNameChange = viewModel::setName,
                        onDefaultLeadChange = viewModel::setDefaultLeadMinutes,
                        onPostcodeChange = viewModel::setPostcode,
                        onRefresh = viewModel::refresh,
                        onClearTaste = viewModel::clearTaste,
                    )
                }
            }
        }

        Clock(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 20.dp),
        )

        if (!state.isLiveData && !state.isLoading) {
            OfflineBanner(modifier = Modifier.align(Alignment.TopEnd).padding(24.dp))
        }

        state.dialog?.let { dialog ->
            ProgrammeDialog(
                state = dialog,
                onOpenInApp = viewModel::openInStreamingApp,
                onTune = viewModel::tuneTo,
                onSetReminder = viewModel::setReminder,
                onRemoveReminder = viewModel::removeReminder,
                onSetVerdict = viewModel::setVerdict,
                onRecord = viewModel::record,
                onCancelRecord = viewModel::cancelRecord,
                onDismiss = viewModel::dismissDialog,
            )
        }
    }
}

@Composable
private fun HomeContent(state: HomeUiState, viewModel: HomeViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        HeroPanel(
            programme = state.heroProgramme,
            hasReminder = state.heroProgramme?.let { it.id in state.reminders } == true,
            isRecording = state.heroProgramme?.let { state.isRecording(it.id) } == true,
        )

        Spacer(Modifier.height(8.dp))

        when {
            state.isLoading && state.rails.isEmpty() -> LoadingHint()
            else -> RailList(state, viewModel)
        }
    }
}

@Composable
private fun RailList(state: HomeUiState, viewModel: HomeViewModel) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        modifier = Modifier
            .fillMaxSize()
            // Cards scrolled up behind the hero fade out instead of being cut
            // off mid-image, which reads as a rendering fault on a big screen.
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = 0f,
                        endY = 44f,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        items(state.rails.size, key = { state.rails[it].id }) { index ->
            RailRow(rail = state.rails[index], state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun RailRow(rail: Rail, state: HomeUiState, viewModel: HomeViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(
            text = rail.title,
            style = TextStyle(
                color = SkyPalette.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier.padding(start = 8.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        ) {
            items(rail.programmes.size, key = { "${rail.id}-${rail.programmes[it].id}" }) { index ->
                val programme = rail.programmes[index]
                ProgrammeCard(
                    programme = programme,
                    hasReminder = programme.id in state.reminders,
                    isRecording = state.isRecording(programme.id),
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
