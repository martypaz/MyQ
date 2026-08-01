package com.martypaz.myq.ui

import android.app.Application
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
import com.martypaz.myq.data.prefs.Profile
import com.martypaz.myq.data.prefs.TasteProfile
import com.martypaz.myq.data.streaming.StreamingApp
import com.martypaz.myq.data.streaming.openInStreamingApp
import com.martypaz.myq.data.streaming.streamingAppFor
import com.martypaz.myq.recs.Recommender
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
) {
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
        refresh()
        viewModelScope.launch {
            combine(
                programmes,
                app.reminderStore.reminders,
                app.tasteStore.profile,
                app.recordingStore.recordings,
                app.profileStore.profile,
            ) { all, reminders, taste, recordings, profile ->
                Bundle(all, reminders, taste, recordings, profile)
            }.collect { bundle ->
                val forYou = app.recommender.forYou(bundle.taste, bundle.all)
                val current = _uiState.value
                _uiState.value = current.copy(
                    rails = buildRails(bundle.all, forYou, bundle.taste),
                    reminders = bundle.reminders.associateBy { it.programmeId },
                    recordings = bundle.recordings,
                    opinions = bundle.taste.toOpinions(),
                    profile = bundle.profile,
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
        val profile: Profile,
    )

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = app.epgRepository.load()
            programmes.value = result.programmes
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLiveData = result.isLive,
                sources = result.sources,
            )
        }
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
                existingLeadHours = state.reminders[programme.id]?.leadHours,
                verdict = state.opinions.firstOrNull { it.title == programme.title }?.verdict ?: Verdict.NONE,
                isRecording = state.isRecording(programme.id),
                isSeriesRecording = state.isSeriesRecording(programme.title),
                streamingApp = streamingAppFor(programme.channelName),
            ),
        )
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

    fun setReminder(programme: Programme, leadHours: Int) {
        viewModelScope.launch {
            val reminder = Reminder(
                programmeId = programme.id,
                title = programme.title,
                channelName = programme.channelName,
                startMillis = programme.startMillis,
                leadHours = leadHours,
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
                setReminder(programme, _uiState.value.profile.defaultLeadHours)
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

    fun setDefaultLeadHours(hours: Int) {
        viewModelScope.launch { app.profileStore.setDefaultLeadHours(hours) }
    }

    fun clearTaste() {
        viewModelScope.launch { app.tasteStore.update { TasteProfile() } }
    }

    // --- internals ---

    private fun refreshDialog(dialog: ProgrammeDialogState, bundle: Bundle): ProgrammeDialogState =
        dialog.copy(
            existingLeadHours = bundle.reminders.firstOrNull {
                it.programmeId == dialog.programme.id
            }?.leadHours,
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

    private fun buildRails(
        all: List<Programme>,
        forYou: List<Programme>,
        taste: TasteProfile,
    ): List<Rail> {
        if (all.isEmpty()) return emptyList()

        // A hated series should not resurface anywhere MyQ is making suggestions.
        val visible = all.filterNot { taste.verdictFor(it.title) == Verdict.HATE }

        val tonightEnd = LocalDate.now().plusDays(1).atTime(LocalTime.of(6, 0))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return listOfNotNull(
            forYou.takeIf { it.isNotEmpty() }?.let { Rail("for-you", "For You", it) },
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
        fun factory(application: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(application) as T
        }
    }
}
