package com.martypaz.myq.ui

import android.app.Application
import android.content.pm.ApplicationInfo
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.martypaz.myq.MyQApp
import com.martypaz.myq.data.model.Newness
import com.martypaz.myq.data.model.Platform
import com.martypaz.myq.data.model.Programme
import com.martypaz.myq.data.model.Rail
import com.martypaz.myq.data.model.RecordEntry
import com.martypaz.myq.data.model.Reminder
import com.martypaz.myq.data.model.Verdict
import com.martypaz.myq.data.epg.EpgRepository
import com.martypaz.myq.data.epg.isLikelyFilm
import com.martypaz.myq.data.epg.isOnNow
import com.martypaz.myq.data.epg.isScheduleFiller
import com.martypaz.myq.data.prefs.Profile
import com.martypaz.myq.data.prefs.TasteProfile
import com.martypaz.myq.data.streaming.StreamingApp
import com.martypaz.myq.data.streaming.openInStreamingApp
import com.martypaz.myq.data.streaming.streamingAppFor
import com.martypaz.myq.data.tv.TvChannel
import com.martypaz.myq.data.tv.tuneTo
import com.martypaz.myq.recs.Recommender
import com.martypaz.myq.reminders.ReminderReadiness
import com.martypaz.myq.reminders.checkReminderReadiness
import com.martypaz.myq.reminders.showAlertNow
import com.martypaz.myq.reminders.migrateReminderIds
import com.martypaz.myq.ui.components.NavDestination
import com.martypaz.myq.ui.screens.SeriesOpinion
import com.martypaz.myq.ui.screens.searchProgrammes
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class HomeUiState(
    val isLoading: Boolean = true,
    val isLiveData: Boolean = true,
    /** Which listings sources answered on the last load. */
    val sources: Set<EpgRepository.Source> = emptySet(),
    val showWelcome: Boolean = true,
    val profile: Profile = Profile(),
    /** False until the stored profile has been read; see [showWelcome]. */
    val isProfileLoaded: Boolean = false,
    val destination: NavDestination = NavDestination.HOME,
    val rails: List<Rail> = emptyList(),
    val heroProgramme: Programme? = null,
    /** programmeId -> reminder, for badge state and toggling. */
    val reminders: Map<String, Reminder> = emptyMap(),
    val recordings: List<RecordEntry> = emptyList(),
    val opinions: List<SeriesOpinion> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Programme> = emptyList(),
    /** Non-null while the programme actions overlay is open. */
    val dialog: ProgrammeDialogState? = null,
    /** Gates the Developer section in the left rail. */
    val isDeveloperMode: Boolean = false,
    /** Permission state behind reminders, refreshed when Settings is shown. */
    val reminderReadiness: ReminderReadiness? = null,
    /** One-off feedback from a developer action. */
    val developerMessage: String? = null,
) {
    /** Developer is only listed when it is reachable. */
    val destinations: List<NavDestination>
        get() = NavDestination.entries.filter { it != NavDestination.DEVELOPER || isDeveloperMode }

    fun isRecording(programmeId: String) = recordings.any { it.programmeId == programmeId }
    fun isSeriesRecording(title: String) = recordings.any { it.title == title && it.isSeries }
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MyQApp

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val programmes = MutableStateFlow<List<Programme>>(emptyList())
    private var browseSignalJob: Job? = null

    init {
        _uiState.value = _uiState.value.copy(
            isDeveloperMode = isDeveloperMode(application),
            reminderReadiness = checkReminderReadiness(application),
        )
        refresh()

        // The profile is collected on its own rather than through the bundle
        // below. The welcome screen cannot decide what to show until it knows
        // whether there is a name, and combine() waits for every one of its
        // flows, so sharing meant the greeting waited on listings, reminders,
        // taste and recordings as well.
        viewModelScope.launch {
            app.profileStore.profile.collect { profile ->
                _uiState.value = _uiState.value.copy(profile = profile, isProfileLoaded = true)
            }
        }

        viewModelScope.launch {
            combine(
                programmes,
                app.reminderStore.reminders,
                app.tasteStore.profile,
                app.recordingStore.recordings,
            ) { all, reminders, taste, recordings ->
                Bundle(all, reminders, taste, recordings)
            }.collect { bundle ->
                val forYou = app.recommender.forYou(bundle.taste, bundle.all)
                val current = _uiState.value
                _uiState.value = current.copy(
                    rails = buildRails(bundle.all, forYou, bundle.taste),
                    reminders = bundle.reminders.associateBy { it.programmeId },
                    recordings = bundle.recordings,
                    opinions = bundle.taste.toOpinions(),
                    heroProgramme = current.heroProgramme ?: bundle.all.firstOrNull(),
                    searchResults = searchProgrammes(bundle.all, current.searchQuery),
                    dialog = current.dialog?.let { refreshDialog(it, bundle) },
                )
            }
        }
    }

    private data class Bundle(
        val all: List<Programme>,
        val reminders: List<Reminder>,
        val taste: TasteProfile,
        val recordings: List<RecordEntry>,
    )

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Each source publishes as it lands, so the guide appears on the
            // first one rather than waiting for the slower of the two.
            app.epgRepository.load().collect { result ->
                // Reminders are stored against the programme id the listings
                // used when they were set, and those ids change when the
                // listings do. Reattach them before anything renders, so a
                // card the user has already flagged never appears unflagged.
                if (result.isLive) {
                    migrateReminderIds(app.reminderStore, app.reminderScheduler, result.programmes)
                }

                programmes.value = result.programmes
                _uiState.value = _uiState.value.copy(
                    isLoading = !result.isComplete,
                    isLiveData = result.isLive,
                    sources = result.sources,
                )
            }
        }
    }

    // --- developer tools ---

    /**
     * Fires a reminder five seconds from now through the real alarm, receiver
     * and full-screen intent, so the whole path can be seen working. Navigate
     * away from MyQ once it is armed and the alert should still arrive.
     */
    fun fireTestReminder() {
        val readiness = checkReminderReadiness(getApplication())
        app.reminderScheduler.scheduleTest()

        // A test that fails silently is worse than no test: the whole point is
        // to find out which of the three permissions is in the way.
        _uiState.value = _uiState.value.copy(
            reminderReadiness = readiness,
            developerMessage = if (readiness.isReady) {
                "Armed for 15 seconds' time. Leave MyQ now — a full-screen alert is usually suppressed while the app that posted it is still in front of you."
            } else {
                "Armed, but it will not be seen: ${readiness.blockers.first()}"
            },
        )
    }

    /**
     * Opens the alert directly, skipping the alarm and the notification. If
     * this appears and a timed test does not, the alert is fine and the
     * notification path is where to look.
     */
    fun showAlertNow() {
        val shown = app.reminderScheduler.showAlertNow(getApplication())
        _uiState.value = _uiState.value.copy(
            developerMessage = if (shown) {
                "Alert opened directly. If the timed test never appears, the alert is not the problem."
            } else {
                "Could not open the alert at all, which points at the activity rather than any permission."
            },
        )
    }

    fun refreshReminderReadiness() {
        _uiState.value = _uiState.value.copy(
            reminderReadiness = checkReminderReadiness(getApplication()),
        )
    }

    fun dismissDeveloperMessage() {
        _uiState.value = _uiState.value.copy(developerMessage = null)
    }

    /**
     * True on a debug build, or when the device has developer options turned
     * on — the two situations where someone is deliberately poking at MyQ
     * rather than watching television.
     */
    private fun isDeveloperMode(application: Application): Boolean {
        val debuggable = application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val developerOptions = Settings.Global.getInt(
            application.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0,
        ) == 1
        return debuggable || developerOptions
    }

    // --- welcome ---

    fun onWelcomeFinished() {
        _uiState.value = _uiState.value.copy(showWelcome = false)
    }

    fun onNameEntered(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) app.profileStore.setFirstName(name)
            _uiState.value = _uiState.value.copy(showWelcome = false)
        }
    }

    // --- navigation ---

    fun onDestinationSelected(destination: NavDestination) {
        _uiState.value = _uiState.value.copy(destination = destination)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            searchResults = searchProgrammes(programmes.value, query),
        )
    }

    // --- programme interaction ---

    /** Focus moved onto a card; update the hero and, after a dwell, learn from it. */
    fun onProgrammeFocused(programme: Programme) {
        _uiState.value = _uiState.value.copy(heroProgramme = programme)
        browseSignalJob?.cancel()
        browseSignalJob = viewModelScope.launch {
            delay(2_000) // only count it as interest if they actually lingered
            app.recommender.record(programme, Recommender.Signal.BROWSED)
        }
    }

    /** OK pressed on a card: open the actions overlay. */
    fun onProgrammeSelected(programme: Programme) {
        viewModelScope.launch { app.recommender.record(programme, Recommender.Signal.SELECTED) }
        val state = _uiState.value
        _uiState.value = state.copy(
            dialog = ProgrammeDialogState(
                programme = programme,
                existingLeadMinutes = state.reminders[programme.id]?.effectiveLeadMinutes,
                verdict = state.opinions.firstOrNull { it.title == programme.title }?.verdict ?: Verdict.NONE,
                isRecording = state.isRecording(programme.id),
                isSeriesRecording = state.isSeriesRecording(programme.title),
                streamingApp = streamingAppFor(programme.channelName),
                tunableChannel = app.tvLineup.channelFor(programme.channelName),
                isOnNow = programme.isOnNow(),
            ),
        )
    }

    /** Switches the television to a channel the box actually receives. */
    fun tuneTo(channel: TvChannel) {
        getApplication<Application>().tuneTo(channel)
        _uiState.value = _uiState.value.copy(dialog = null)
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(dialog = null)
    }

    /**
     * Leaves MyQ for the service carrying this programme. Wanting to watch
     * something now is the strongest taste signal there is, so it counts the
     * same as setting a reminder.
     */
    fun openInStreamingApp(programme: Programme, target: StreamingApp) {
        viewModelScope.launch {
            app.recommender.record(programme, Recommender.Signal.REMINDER_SET)
            getApplication<Application>().openInStreamingApp(target, programme.title)
            _uiState.value = _uiState.value.copy(dialog = null)
        }
    }

    fun setReminder(programme: Programme, leadMinutes: Int) {
        viewModelScope.launch {
            val reminder = Reminder(
                programmeId = programme.id,
                title = programme.title,
                channelName = programme.channelName,
                startMillis = programme.startMillis,
                leadMinutes = leadMinutes,
            )
            app.reminderStore.upsert(reminder)
            app.reminderScheduler.schedule(reminder)
            app.recommender.record(programme, Recommender.Signal.REMINDER_SET)
            _uiState.value = _uiState.value.copy(dialog = null)
        }
    }

    fun removeReminder(programmeId: String) {
        viewModelScope.launch {
            app.reminderStore.remove(programmeId)
            app.reminderScheduler.cancel(programmeId)
            _uiState.value = _uiState.value.copy(dialog = null)
        }
    }

    fun setVerdict(programme: Programme, verdict: Verdict) {
        viewModelScope.launch { app.tasteStore.setVerdict(programme.title, verdict) }
    }

    fun cycleVerdict(opinion: SeriesOpinion) {
        val next = when (opinion.verdict) {
            Verdict.LOVE -> Verdict.HATE
            Verdict.HATE -> Verdict.NONE
            Verdict.NONE -> Verdict.LOVE
        }
        viewModelScope.launch { app.tasteStore.setVerdict(opinion.title, next) }
    }

    /**
     * Saves a programme to the record list, and a reminder with it — a record
     * that never tells you it is about to start would be no use.
     */
    fun record(programme: Programme, asSeries: Boolean) {
        viewModelScope.launch {
            app.recordingStore.upsert(
                RecordEntry(
                    programmeId = programme.id,
                    title = programme.title,
                    channelName = programme.channelName,
                    startMillis = programme.startMillis,
                    isSeries = asSeries,
                ),
            )
            if (_uiState.value.reminders[programme.id] == null) {
                setReminder(programme, _uiState.value.profile.defaultLeadMinutes)
            }
            app.recommender.record(programme, Recommender.Signal.REMINDER_SET)
            _uiState.value = _uiState.value.copy(dialog = null)
        }
    }

    fun cancelRecord(programme: Programme, asSeries: Boolean) {
        viewModelScope.launch {
            if (asSeries) {
                app.recordingStore.removeSeries(programme.title)
            } else {
                app.recordingStore.remove(programme.id)
            }
            _uiState.value = _uiState.value.copy(dialog = null)
        }
    }

    fun removeRecording(entry: RecordEntry) {
        viewModelScope.launch { app.recordingStore.remove(entry.programmeId) }
    }

    // --- settings ---

    fun setName(name: String) {
        viewModelScope.launch { app.profileStore.setFirstName(name) }
    }

    /** Resolves a postcode to a transmitter region, then reloads the guide. */
    fun setPostcode(postcode: String) {
        viewModelScope.launch {
            val network = app.freeviewApi.networkFor(postcode)
            if (network == null) {
                _uiState.value = _uiState.value.copy(
                    developerMessage = "Could not find a TV region for that postcode.",
                )
                return@launch
            }
            app.profileStore.setRegion(postcode, network.network_id, network.network_name)
            refresh()
        }
    }

    fun setDefaultLeadMinutes(minutes: Int) {
        viewModelScope.launch { app.profileStore.setDefaultLeadMinutes(minutes) }
    }

    fun clearTaste() {
        viewModelScope.launch { app.tasteStore.update { TasteProfile() } }
    }

    // --- internals ---

    private fun refreshDialog(dialog: ProgrammeDialogState, bundle: Bundle): ProgrammeDialogState =
        dialog.copy(
            existingLeadMinutes = bundle.reminders.firstOrNull {
                it.programmeId == dialog.programme.id
            }?.effectiveLeadMinutes,
            verdict = bundle.taste.verdictFor(dialog.programme.title),
            isRecording = bundle.recordings.any { it.programmeId == dialog.programme.id },
            isSeriesRecording = bundle.recordings.any {
                it.title == dialog.programme.title && it.isSeries
            },
        )

    private fun TasteProfile.toOpinions(): List<SeriesOpinion> =
        verdicts.mapNotNull { (title, raw) ->
            runCatching { Verdict.valueOf(raw) }.getOrNull()?.let { SeriesOpinion(title, it) }
        }.sortedWith(compareBy({ it.verdict.ordinal }, { it.title }))

    /**
     * What the browse rails are allowed to suggest: a series with enough
     * episodes ahead to be worth starting, or a film.
     *
     * A fortnight of the full Freeview line-up is mostly things nobody browses
     * for — continuity, shopping, one-off filler — and burying the four
     * programmes someone might actually plan around under them makes the rails
     * useless. Search is deliberately not filtered, so anything dropped here
     * is still findable by name.
     */
    private fun worthBrowsing(programmes: List<Programme>): List<Programme> {
        val episodeCounts = programmes.groupingBy { it.title.trim().lowercase() }.eachCount()
        return programmes.filter { programme ->
            if (programme.isScheduleFiller()) return@filter false
            if (programme.isLikelyFilm()) return@filter true
            (episodeCounts[programme.title.trim().lowercase()] ?: 0) >= MIN_EPISODES_TO_BROWSE
        }
    }

    private fun buildRails(
        all: List<Programme>,
        forYou: List<Programme>,
        taste: TasteProfile,
    ): List<Rail> {
        if (all.isEmpty()) return emptyList()

        // A hated series should not resurface anywhere MyQ is making suggestions.
        val visible = all.filterNot { taste.verdictFor(it.title) == Verdict.HATE }
            .let(::worthBrowsing)

        val tonightEnd = LocalDate.now().plusDays(1).atTime(LocalTime.of(6, 0))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val visibleIds = visible.mapTo(HashSet(visible.size)) { it.id }
        val recommended = forYou.filter { it.id in visibleIds }

        return listOfNotNull(
            recommended.takeIf { it.isNotEmpty() }?.let { Rail("for-you", "For You", it) },
            visible.filter { it.newness == Newness.NEW_SERIES }
                .takeIf { it.isNotEmpty() }?.let { Rail("new-series", "New Series", it.take(30)) },
            visible.filter { it.newness == Newness.NEW_SEASON }
                .takeIf { it.isNotEmpty() }?.let { Rail("new-seasons", "Returning Series", it.take(30)) },
            visible.filter { it.startMillis < tonightEnd && it.platform == Platform.FREEVIEW }
                .takeIf { it.isNotEmpty() }?.let { Rail("tonight", "Tonight on Freeview", it.take(30)) },
            visible.filter { it.platform == Platform.STREAMING }
                .takeIf { it.isNotEmpty() }?.let { Rail("streaming", "New on Streaming", it.take(30)) },
            Rail("coming-up", "Coming Up", visible.take(40)),
        )
    }

    companion object {
        /** Enough episodes ahead that starting the series is worth it. */
        const val MIN_EPISODES_TO_BROWSE = 4

        fun factory(application: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(application) as T
        }
    }
}
