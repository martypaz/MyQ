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
import com.martypaz.myq.data.model.Reminder
import com.martypaz.myq.recs.Recommender
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
    val rails: List<Rail> = emptyList(),
    val heroProgramme: Programme? = null,
    /** programmeId → reminder, for badge state and toggling. */
    val reminders: Map<String, Reminder> = emptyMap(),
    /** Programme the reminder picker dialog is open for, if any. */
    val reminderTarget: Programme? = null,
)

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
            ) { all, reminders, profile ->
                Triple(all, reminders, profile)
            }.collect { (all, reminders, profile) ->
                val forYou = app.recommender.forYou(profile, all)
                _uiState.value = _uiState.value.copy(
                    rails = buildRails(all, forYou),
                    reminders = reminders.associateBy { it.programmeId },
                    heroProgramme = _uiState.value.heroProgramme ?: all.firstOrNull(),
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = app.epgRepository.load()
            programmes.value = result.programmes
            _uiState.value = _uiState.value.copy(isLoading = false, isLiveData = result.isLive)
        }
    }

    /** Focus moved onto a card; update the hero and, after a dwell, learn from it. */
    fun onProgrammeFocused(programme: Programme) {
        _uiState.value = _uiState.value.copy(heroProgramme = programme)
        browseSignalJob?.cancel()
        browseSignalJob = viewModelScope.launch {
            delay(2_000) // only count it as interest if they actually lingered
            app.recommender.record(programme, Recommender.Signal.BROWSED)
        }
    }

    /** OK pressed on a card: open the reminder picker (or offer to clear). */
    fun onProgrammeSelected(programme: Programme) {
        viewModelScope.launch { app.recommender.record(programme, Recommender.Signal.SELECTED) }
        _uiState.value = _uiState.value.copy(reminderTarget = programme)
    }

    fun dismissReminderDialog() {
        _uiState.value = _uiState.value.copy(reminderTarget = null)
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
            _uiState.value = _uiState.value.copy(reminderTarget = null)
        }
    }

    fun removeReminder(programmeId: String) {
        viewModelScope.launch {
            app.reminderStore.remove(programmeId)
            app.reminderScheduler.cancel(programmeId)
            _uiState.value = _uiState.value.copy(reminderTarget = null)
        }
    }

    private fun buildRails(all: List<Programme>, forYou: List<Programme>): List<Rail> {
        if (all.isEmpty()) return emptyList()

        val tonightEnd = LocalDate.now().plusDays(1).atTime(LocalTime.of(6, 0))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        fun tonight(p: Programme) = p.startMillis < tonightEnd

        return listOfNotNull(
            forYou.takeIf { it.isNotEmpty() }?.let { Rail("for-you", "For You", it) },
            all.filter { it.newness == Newness.NEW_SERIES }
                .takeIf { it.isNotEmpty() }?.let { Rail("new-series", "New Series", it.take(30)) },
            all.filter { it.newness == Newness.NEW_SEASON }
                .takeIf { it.isNotEmpty() }?.let { Rail("new-seasons", "Returning Series", it.take(30)) },
            all.filter { tonight(it) && it.platform == Platform.FREEVIEW }
                .takeIf { it.isNotEmpty() }?.let { Rail("tonight", "Tonight on Freeview", it.take(30)) },
            all.filter { it.platform == Platform.STREAMING }
                .takeIf { it.isNotEmpty() }?.let { Rail("streaming", "New on Streaming", it.take(30)) },
            Rail("coming-up", "Coming Up", all.take(40)),
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
