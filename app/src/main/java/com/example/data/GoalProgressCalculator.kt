package com.example.data

import com.example.data.model.GoalEntity
import com.example.data.model.RecurrenceType
import com.example.data.model.TimeLogEntity
import java.util.Calendar

data class GoalProgressInfo(
    val currentPeriodLoggedSeconds: Long,
    val targetSeconds: Long,
    val progressRatio: Double, // 0.0 to 1.0+
    val progressPercentage: Int, // e.g. 52%
    val secondsLeft: Long,
    val averageSuccessRate: Double?, // null for IN_TOTAL
    val averageSuccessPercentage: Int?, // e.g. 25%
    val formattedCurrentStatus: String, // e.g. "57 sec left until target" or "Goal reached!"
    val isAutoCompletable: Boolean
)

object GoalProgressCalculator {

    fun formatDuration(seconds: Long): String {
        val s = seconds.coerceAtLeast(0L)
        val hours = s / 3600
        val minutes = (s % 3600) / 60
        val remainingSeconds = s % 60

        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours} hrs"
            minutes > 0 && remainingSeconds > 0 -> "${minutes}m ${remainingSeconds}s"
            minutes > 0 -> "${minutes} mins"
            else -> "${remainingSeconds} sec"
        }
    }

    fun formatTimerClock(seconds: Long): String {
        val s = seconds.coerceAtLeast(0L)
        val hours = s / 3600
        val minutes = (s % 3600) / 60
        val remSec = s % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, remSec)
        } else {
            String.format("%02d:%02d", minutes, remSec)
        }
    }

    fun calculateProgress(
        goal: GoalEntity,
        logs: List<TimeLogEntity>,
        now: Long = System.currentTimeMillis()
    ): GoalProgressInfo {
        val target = goal.targetSeconds.coerceAtLeast(1L)

        if (goal.recurrenceType == RecurrenceType.IN_TOTAL) {
            val totalLogged = logs.sumOf { it.durationSeconds }
            val ratio = totalLogged.toDouble() / target.toDouble()
            val percentage = (ratio * 100).toInt()
            val left = maxOf(0L, target - totalLogged)
            val isComplete = totalLogged >= target

            val status = if (left == 0L) {
                "Goal achieved!"
            } else {
                "${formatDuration(left)} left until target"
            }

            return GoalProgressInfo(
                currentPeriodLoggedSeconds = totalLogged,
                targetSeconds = target,
                progressRatio = ratio,
                progressPercentage = percentage,
                secondsLeft = left,
                averageSuccessRate = null,
                averageSuccessPercentage = null,
                formattedCurrentStatus = status,
                isAutoCompletable = isComplete
            )
        }

        // Periodic goals
        val (periodStart, periodEnd) = getCurrentPeriodRange(goal, now)
        val currentPeriodLogged = logs
            .filter { it.timestamp in periodStart..periodEnd }
            .sumOf { it.durationSeconds }

        val ratio = currentPeriodLogged.toDouble() / target.toDouble()
        val percentage = (ratio * 100).toInt()
        val left = maxOf(0L, target - currentPeriodLogged)

        val status = if (left == 0L) {
            "Target reached for this period!"
        } else {
            "${formatDuration(left)} left until target"
        }

        // Average calculation
        val avgSuccessRate = if (goal.isCompleted && goal.lockedAverageSuccessRate != null) {
            goal.lockedAverageSuccessRate
        } else {
            calculateDynamicAverageSuccessRate(goal, logs, now)
        }

        val avgPercentage = avgSuccessRate?.let { (it * 100).toInt() }

        return GoalProgressInfo(
            currentPeriodLoggedSeconds = currentPeriodLogged,
            targetSeconds = target,
            progressRatio = ratio,
            progressPercentage = percentage,
            secondsLeft = left,
            averageSuccessRate = avgSuccessRate,
            averageSuccessPercentage = avgPercentage,
            formattedCurrentStatus = status,
            isAutoCompletable = false
        )
    }

    private fun getCurrentPeriodRange(goal: GoalEntity, now: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = now }

        return when (goal.recurrenceType) {
            RecurrenceType.HOURLY -> {
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.HOUR_OF_DAY, 1)
                val end = cal.timeInMillis - 1
                Pair(start, end)
            }
            RecurrenceType.DAILY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val end = cal.timeInMillis - 1
                Pair(start, end)
            }
            RecurrenceType.CUSTOM_DAYS -> {
                val interval = goal.recurrenceDaysInterval.coerceAtLeast(1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val todayMidnight = cal.timeInMillis

                val createdCal = Calendar.getInstance().apply {
                    timeInMillis = goal.createdAt
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val createdMidnight = createdCal.timeInMillis
                val dayDiff = ((todayMidnight - createdMidnight) / (86400_000L)).coerceAtLeast(0L)
                val cycleIndex = dayDiff / interval
                val start = createdMidnight + cycleIndex * interval * 86400_000L
                val end = start + interval * 86400_000L - 1
                Pair(start, end)
            }
            RecurrenceType.WEEKLY -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                val end = cal.timeInMillis - 1
                Pair(start, end)
            }
            RecurrenceType.YEARLY -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.YEAR, 1)
                val end = cal.timeInMillis - 1
                Pair(start, end)
            }
            RecurrenceType.IN_TOTAL -> Pair(0L, Long.MAX_VALUE)
        }
    }

    fun calculateDynamicAverageSuccessRate(
        goal: GoalEntity,
        logs: List<TimeLogEntity>,
        now: Long = System.currentTimeMillis()
    ): Double {
        val target = goal.targetSeconds.coerceAtLeast(1L)
        val periodDurationMillis = when (goal.recurrenceType) {
            RecurrenceType.HOURLY -> 3600_000L
            RecurrenceType.DAILY -> 86400_000L
            RecurrenceType.CUSTOM_DAYS -> goal.recurrenceDaysInterval.coerceAtLeast(1) * 86400_000L
            RecurrenceType.WEEKLY -> 7 * 86400_000L
            RecurrenceType.YEARLY -> 365 * 86400_000L
            RecurrenceType.IN_TOTAL -> return 0.0
        }

        // Active elapsed time excludes completed downtime
        val totalActiveDurationMillis = (now - goal.createdAt - goal.totalCompletedDowntimeSeconds * 1000L)
            .coerceAtLeast(0L)

        val totalPeriods = ((totalActiveDurationMillis / periodDurationMillis) + 1).coerceIn(1L, 10000L)

        // Calculate success rate per period bucket
        // Bucket 0 is the current period, bucket 1 is previous, etc.
        var totalSuccessRatioSum = 0.0

        for (i in 0 until totalPeriods) {
            val pEnd = now - i * periodDurationMillis
            val pStart = maxOf(goal.createdAt, pEnd - periodDurationMillis)

            val periodLoggedSeconds = logs
                .filter { it.timestamp in pStart..pEnd }
                .sumOf { it.durationSeconds }

            // Success capped at 1.0 (100% reached)
            val successInPeriod = (periodLoggedSeconds.toDouble() / target.toDouble()).coerceIn(0.0, 1.0)
            totalSuccessRatioSum += successInPeriod
        }

        return totalSuccessRatioSum / totalPeriods.toDouble()
    }
}
