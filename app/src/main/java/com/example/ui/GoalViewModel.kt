package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.GoalTrackerApplication
import com.example.data.GoalProgressCalculator
import com.example.data.GoalProgressInfo
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

class GoalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as GoalTrackerApplication).repository
    private val prefManager = PreferenceManager(application)

    private val _startScreen = MutableStateFlow(prefManager.getStartScreen())
    val startScreen: StateFlow<StartScreen> = _startScreen.asStateFlow()

    val timerState: StateFlow<TimerState> = TimerStateManager.timerState

    val allLogs: StateFlow<List<TimeLogEntity>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeGoalsWithProgress: StateFlow<List<GoalWithProgress>> = combine(
        repository.activeGoals,
        allLogs
    ) { goals, logs ->
        goals.map { goal ->
            val gLogs = logs.filter { it.goalId == goal.id }
            val progress = GoalProgressCalculator.calculateProgress(goal, gLogs)
            if (progress.isAutoCompletable && !goal.isCompleted) {
                // Auto-complete IN_TOTAL goal
                viewModelScope.launch(Dispatchers.IO) {
                    repository.markGoalComplete(goal)
                }
            }
            GoalWithProgress(goal, progress)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteGoalsWithProgress: StateFlow<List<GoalWithProgress>> = combine(
        repository.favoriteGoals,
        allLogs
    ) { goals, logs ->
        goals.map { goal ->
            val gLogs = logs.filter { it.goalId == goal.id }
            val progress = GoalProgressCalculator.calculateProgress(goal, gLogs)
            GoalWithProgress(goal, progress)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedGoalsWithProgress: StateFlow<List<GoalWithProgress>> = combine(
        repository.completedGoals,
        allLogs
    ) { goals, logs ->
        goals.map { goal ->
            val gLogs = logs.filter { it.goalId == goal.id }
            val progress = GoalProgressCalculator.calculateProgress(goal, gLogs)
            GoalWithProgress(goal, progress)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedGoalsWithProgress: StateFlow<List<GoalWithProgress>> = combine(
        repository.archivedGoals,
        allLogs
    ) { goals, logs ->
        goals.map { goal ->
            val gLogs = logs.filter { it.goalId == goal.id }
            val progress = GoalProgressCalculator.calculateProgress(goal, gLogs)
            GoalWithProgress(goal, progress)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun addNote(goalId: Long, content: String) {
        viewModelScope.launch {
            repository.insertNote(goalId, content)
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
