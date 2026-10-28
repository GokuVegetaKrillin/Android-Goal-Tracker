package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.GoalTrackerApplication
import com.example.data.GoalProgressCalculator
import com.example.data.GoalProgressInfo
import com.example.data.GoalSortOrder
import com.example.data.PreferenceManager
import com.example.data.StartScreen
import com.example.data.model.GoalEntity
import com.example.data.model.GoalNoteEntity
import com.example.data.model.RecurrenceType
import com.example.data.model.TimeLogEntity
import com.example.service.TimerService
import com.example.service.TimerState
import com.example.service.TimerStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GoalWithProgress(
    val goal: GoalEntity,
    val progress: GoalProgressInfo
)

data class GoalCreationDefaults(
    val targetSeconds: Long,
    val recurrenceType: RecurrenceType,
    val customDaysInterval: Int,
    val colorHex: String,
    val category: String
)

class GoalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as GoalTrackerApplication).repository
    private val prefManager = PreferenceManager(application)

    private val _startScreen = MutableStateFlow(prefManager.getStartScreen())
    val startScreen: StateFlow<StartScreen> = _startScreen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(prefManager.getSortOrder())
    val sortOrder: StateFlow<GoalSortOrder> = _sortOrder.asStateFlow()

    private val _isHeaderExpanded = MutableStateFlow(prefManager.isToolsHeaderExpanded())
    val isHeaderExpanded: StateFlow<Boolean> = _isHeaderExpanded.asStateFlow()

    val timerState: StateFlow<TimerState> = TimerStateManager.timerState

    val allLogs: StateFlow<List<TimeLogEntity>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<GoalNoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filterSortFlow = combine(_searchQuery, _sortOrder) { q, sort ->
        Pair(q, sort)
    }

    private fun processGoals(
        goals: List<GoalEntity>,
        logs: List<TimeLogEntity>,
        notes: List<GoalNoteEntity>,
        query: String,
        sort: GoalSortOrder
    ): List<GoalWithProgress> {
        val trimmedQuery = query.trim().lowercase()

        val list = goals.mapNotNull { goal ->
            val gLogs = logs.filter { it.goalId == goal.id }
            val gNotes = notes.filter { it.goalId == goal.id }

            // Search filter: title, description, category, notes, and log memos
            val matches = if (trimmedQuery.isEmpty()) {
                true
            } else {
                goal.name.lowercase().contains(trimmedQuery) ||
                        goal.description.lowercase().contains(trimmedQuery) ||
                        goal.category.lowercase().contains(trimmedQuery) ||
                        gNotes.any { it.content.lowercase().contains(trimmedQuery) } ||
                        gLogs.any { it.note.lowercase().contains(trimmedQuery) }
            }

            if (!matches) {
                null
            } else {
                val progress = GoalProgressCalculator.calculateProgress(goal, gLogs)
                if (progress.isAutoCompletable && !goal.isCompleted) {
                    viewModelScope.launch(Dispatchers.IO) {
                        repository.markGoalComplete(goal)
                    }
                }
                GoalWithProgress(goal, progress)
            }
        }

        // Apply sorting
        return when (sort) {
            GoalSortOrder.RECENTLY_ACTIVE -> {
                list.sortedByDescending { item ->
                    val latestLogTime = logs.filter { it.goalId == item.goal.id }.maxOfOrNull { it.timestamp } ?: 0L
                    maxOf(item.goal.createdAt, latestLogTime)
                }
            }
            GoalSortOrder.ALPHABETICAL_ASC -> {
                list.sortedBy { it.goal.name.lowercase() }
            }
            GoalSortOrder.ALPHABETICAL_DESC -> {
                list.sortedByDescending { it.goal.name.lowercase() }
            }
            GoalSortOrder.PROGRESS_DESC -> {
                list.sortedByDescending { it.progress.progressRatio }
            }
            GoalSortOrder.PROGRESS_ASC -> {
                list.sortedBy { it.progress.progressRatio }
            }
            GoalSortOrder.TARGET_TIME_DESC -> {
                list.sortedByDescending { it.goal.targetSeconds }
            }
            GoalSortOrder.TARGET_TIME_ASC -> {
                list.sortedBy { it.goal.targetSeconds }
            }
            GoalSortOrder.NEWEST -> {
                list.sortedByDescending { it.goal.createdAt }
            }
            GoalSortOrder.OLDEST -> {
                list.sortedBy { it.goal.createdAt }
            }
        }
    }

    val activeGoalsWithProgress: StateFlow<List<GoalWithProgress>> = combine(
        repository.activeGoals,
        allLogs,
        allNotes,
        filterSortFlow
    ) { goals, logs, notes, (query, sort) ->
        processGoals(goals, logs, notes, query, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteGoalsWithProgress: StateFlow<List<GoalWithProgress>> = combine(
        repository.favoriteGoals,
        allLogs,
        allNotes,
        filterSortFlow
    ) { goals, logs, notes, (query, sort) ->
        processGoals(goals, logs, notes, query, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedGoalsWithProgress: StateFlow<List<GoalWithProgress>> = combine(
        repository.completedGoals,
        allLogs,
        allNotes,
        filterSortFlow
    ) { goals, logs, notes, (query, sort) ->
        processGoals(goals, logs, notes, query, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedGoalsWithProgress: StateFlow<List<GoalWithProgress>> = combine(
        repository.archivedGoals,
        allLogs,
        allNotes,
        filterSortFlow
    ) { goals, logs, notes, (query, sort) ->
        processGoals(goals, logs, notes, query, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: GoalSortOrder) {
        prefManager.setSortOrder(order)
        _sortOrder.value = order
    }

    fun setHeaderExpanded(expanded: Boolean) {
        prefManager.setToolsHeaderExpanded(expanded)
        _isHeaderExpanded.value = expanded
    }

    fun getGoalCreationDefaults(): GoalCreationDefaults {
        return GoalCreationDefaults(
            targetSeconds = prefManager.getLastTargetSeconds(),
            recurrenceType = prefManager.getLastRecurrenceType(),
            customDaysInterval = prefManager.getLastCustomDaysInterval(),
            colorHex = prefManager.getLastColorHex(),
            category = prefManager.getLastCategory()
        )
    }

    fun createGoal(
        name: String,
        description: String = "",
        colorHex: String = "#8B5CF6",
        targetSeconds: Long,
        recurrenceType: RecurrenceType,
        recurrenceDaysInterval: Int = 1,
        category: String = ""
    ) {
        viewModelScope.launch {
            // Save defaults for future new goals
            prefManager.saveGoalCreationDefaults(
                targetSeconds = targetSeconds,
                recurrenceType = recurrenceType,
                customDaysInterval = recurrenceDaysInterval,
                colorHex = colorHex,
                category = category
            )

            repository.insertGoal(
                GoalEntity(
                    name = name,
                    description = description,
                    colorHex = colorHex,
                    targetSeconds = targetSeconds,
                    recurrenceType = recurrenceType,
                    recurrenceDaysInterval = recurrenceDaysInterval,
                    category = category
                )
            )
        }
    }

    fun updateGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.updateGoal(goal)
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            if (timerState.value.goalId == goal.id) {
                stopTimer()
            }
            repository.deleteGoal(goal)
        }
    }

    fun toggleFavorite(goal: GoalEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(goal)
        }
    }

    fun archiveGoal(goal: GoalEntity) {
        viewModelScope.launch {
            if (timerState.value.goalId == goal.id) {
                stopTimer()
            }
            repository.archiveGoal(goal)
        }
    }

    fun unarchiveGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.unarchiveGoal(goal)
        }
    }

    fun markGoalComplete(goal: GoalEntity) {
        viewModelScope.launch {
            if (timerState.value.goalId == goal.id) {
                stopTimer()
            }
            repository.markGoalComplete(goal)
        }
    }

    fun markGoalIncomplete(goal: GoalEntity) {
        viewModelScope.launch {
            repository.markGoalIncomplete(goal)
        }
    }

    fun resetGoal(goalId: Long) {
        viewModelScope.launch {
            if (timerState.value.goalId == goalId) {
                stopTimer()
            }
            repository.resetGoal(goalId)
        }
    }

    fun addManualTime(goalId: Long, seconds: Long, note: String = "") {
        viewModelScope.launch {
            repository.logTime(
                goalId = goalId,
                durationSeconds = seconds,
                note = note,
                isManual = true
            )
        }
    }

    fun updateTimeLog(log: TimeLogEntity) {
        viewModelScope.launch {
            repository.updateLog(log)
        }
    }

    fun deleteTimeLog(logId: Long, goalId: Long) {
        viewModelScope.launch {
            repository.deleteLogById(logId, goalId)
        }
    }

    fun addNote(goalId: Long, content: String) {
        viewModelScope.launch {
            repository.insertNote(goalId, content)
        }
    }

    fun updateNote(note: GoalNoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNoteById(noteId)
        }
    }

    fun getNotesForGoal(goalId: Long) = repository.getNotesForGoal(goalId)

    fun getLogsForGoal(goalId: Long) = repository.getLogsForGoal(goalId)

    fun setStartScreen(screen: StartScreen) {
        prefManager.setStartScreen(screen)
        _startScreen.value = screen
    }

    fun startTimer(goal: GoalEntity) {
        TimerService.start(
            context = getApplication(),
            goalId = goal.id,
            goalName = goal.name,
            goalColor = goal.colorHex
        )
    }

    fun pauseTimer() {
        TimerService.pause(getApplication())
    }

    fun resumeTimer() {
        TimerService.resume(getApplication())
    }

    fun stopTimer() {
        TimerService.stop(getApplication())
    }

    suspend fun exportJson(): String {
        return repository.exportAllToJson()
    }

    fun importJson(json: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.importFromJson(json)
            result.fold(
                onSuccess = { count ->
                    onResult(true, "Successfully imported $count goal(s)!")
                },
                onFailure = { err ->
                    onResult(false, "Import failed: ${err.localizedMessage ?: "Invalid file"}")
                }
            )
        }
    }
}
