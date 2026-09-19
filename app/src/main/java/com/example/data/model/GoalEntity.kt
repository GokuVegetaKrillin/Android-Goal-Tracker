package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RecurrenceType(val displayName: String) {
    IN_TOTAL("In total"),
    HOURLY("Every hour"),
    DAILY("Every day"),
    CUSTOM_DAYS("Every X days"),
    WEEKLY("Every week"),
    YEARLY("Every year")
}

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val colorHex: String = "#8B5CF6", // Default purple
    val targetSeconds: Long = 3600L, // Default 1 hour
    val recurrenceType: RecurrenceType = RecurrenceType.DAILY,
    val recurrenceDaysInterval: Int = 1, // When recurrenceType == CUSTOM_DAYS
    val category: String = "",
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val totalCompletedDowntimeSeconds: Long = 0L,
    val lockedAverageSuccessRate: Double? = null
)
