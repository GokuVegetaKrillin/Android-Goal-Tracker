package com.example

import com.example.data.GoalProgressCalculator
import com.example.data.model.GoalEntity
import com.example.data.model.RecurrenceType
import com.example.data.model.TimeLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testReadingGoalAverageCalculation() {
    val now = 1000000000000L
    val oneDayMillis = 86400000L
    // Goal created 1 day ago (so 2 total periods: yesterday and today)
    val goal = GoalEntity(
      id = 1L,
      name = "Reading",
      targetSeconds = 3600L, // 1 hour
      recurrenceType = RecurrenceType.DAILY,
      createdAt = now - oneDayMillis
    )

    // Log 30 minutes (1800 sec) today (50% progress today)
    val logs = listOf(
      TimeLogEntity(
        id = 1L,
        goalId = 1L,
        durationSeconds = 1800L,
        timestamp = now - 1000L
      )
    )

    val progress = GoalProgressCalculator.calculateProgress(goal, logs, now)

    // Current period progress is 50%
    assertEquals(50, progress.progressPercentage)
    assertEquals(1800L, progress.secondsLeft)

    // Average success across 2 periods (0% yesterday + 50% today) / 2 = 25%
    assertEquals(25, progress.averageSuccessPercentage)
  }

  @Test
  fun testInTotalGoalCalculation() {
    val goal = GoalEntity(
      id = 2L,
      name = "Project",
      targetSeconds = 3600L,
      recurrenceType = RecurrenceType.IN_TOTAL,
      createdAt = 1000L
    )

    val logs = listOf(
      TimeLogEntity(
        id = 1L,
        goalId = 2L,
        durationSeconds = 1800L,
        timestamp = 2000L
      )
    )

    val progress = GoalProgressCalculator.calculateProgress(goal, logs, 3000L)
    assertEquals(50, progress.progressPercentage)
    assertNull(progress.averageSuccessRate)
    assertNull(progress.averageSuccessPercentage)
  }
}

