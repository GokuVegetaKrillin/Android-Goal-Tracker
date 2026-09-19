package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TimerState(
    val goalId: Long? = null,
    val goalName: String = "",
    val goalColorHex: String = "#8B5CF6",
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Long = 0L
)

object TimerStateManager {
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    fun updateState(transform: (TimerState) -> TimerState) {
        _timerState.value = transform(_timerState.value)
    }

    fun reset() {
        _timerState.value = TimerState()
    }
}
