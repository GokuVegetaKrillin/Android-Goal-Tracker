package com.example.data

import com.example.data.dao.GoalDao
import com.example.data.dao.GoalNoteDao
import com.example.data.dao.TimeLogDao
import com.example.data.model.GoalEntity
import com.example.data.model.GoalNoteEntity
import com.example.data.model.RecurrenceType
import com.example.data.model.TimeLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class GoalRepository(
    private val goalDao: GoalDao,
    private val timeLogDao: TimeLogDao,
    private val goalNoteDao: GoalNoteDao
) {
    val activeGoals: Flow<List<GoalEntity>> = goalDao.getAllActiveGoals()
    val favoriteGoals: Flow<List<GoalEntity>> = goalDao.getFavoriteGoals()
    val completedGoals: Flow<List<GoalEntity>> = goalDao.getCompletedGoals()
    val archivedGoals: Flow<List<GoalEntity>> = goalDao.getArchivedGoals()
    val allGoals: Flow<List<GoalEntity>> = goalDao.getAllGoals()
    val allLogs: Flow<List<TimeLogEntity>> = timeLogDao.getAllLogs()

    fun getGoalById(id: Long): Flow<GoalEntity?> = goalDao.getGoalById(id)

    suspend fun getGoalByIdDirect(id: Long): GoalEntity? = withContext(Dispatchers.IO) {
        goalDao.getGoalByIdDirect(id)
    }

    fun getLogsForGoal(goalId: Long): Flow<List<TimeLogEntity>> =
        timeLogDao.getLogsForGoal(goalId)

    fun getNotesForGoal(goalId: Long): Flow<List<GoalNoteEntity>> =
        goalNoteDao.getNotesForGoal(goalId)

    suspend fun insertGoal(goal: GoalEntity): Long = withContext(Dispatchers.IO) {
        goalDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: GoalEntity) = withContext(Dispatchers.IO) {
        goalDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: GoalEntity) = withContext(Dispatchers.IO) {
        goalDao.deleteGoal(goal)
    }

    suspend fun deleteGoalById(id: Long) = withContext(Dispatchers.IO) {
        goalDao.deleteGoalById(id)
    }

    suspend fun toggleFavorite(goal: GoalEntity) = withContext(Dispatchers.IO) {
        goalDao.updateGoal(goal.copy(isFavorite = !goal.isFavorite))
    }

    suspend fun archiveGoal(goal: GoalEntity) = withContext(Dispatchers.IO) {
        goalDao.updateGoal(goal.copy(isArchived = true))
    }

    suspend fun unarchiveGoal(goal: GoalEntity) = withContext(Dispatchers.IO) {
        goalDao.updateGoal(goal.copy(isArchived = false))
    }

    suspend fun markGoalComplete(goal: GoalEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val logs = timeLogDao.getLogsForGoalList(goal.id)
        val lockedAvg = if (goal.recurrenceType != RecurrenceType.IN_TOTAL) {
            GoalProgressCalculator.calculateDynamicAverageSuccessRate(goal, logs, now)
        } else {
            null
        }

        goalDao.updateGoal(
            goal.copy(
                isCompleted = true,
                completedAt = now,
                lockedAverageSuccessRate = lockedAvg
            )
        )
    }

    suspend fun markGoalIncomplete(goal: GoalEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val compAt = goal.completedAt ?: now
        val downtimeSec = ((now - compAt) / 1000L).coerceAtLeast(0L)

        goalDao.updateGoal(
            goal.copy(
                isCompleted = false,
                completedAt = null,
                totalCompletedDowntimeSeconds = goal.totalCompletedDowntimeSeconds + downtimeSec,
                lockedAverageSuccessRate = null
            )
        )
    }

    suspend fun resetGoal(goalId: Long) = withContext(Dispatchers.IO) {
        timeLogDao.deleteLogsForGoal(goalId)
        val goal = goalDao.getGoalByIdDirect(goalId)
        if (goal != null) {
            goalDao.updateGoal(
                goal.copy(
                    isCompleted = false,
                    completedAt = null,
                    totalCompletedDowntimeSeconds = 0L,
                    lockedAverageSuccessRate = null,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun logTime(
        goalId: Long,
        durationSeconds: Long,
        note: String = "",
        isManual: Boolean = false
    ): Long = withContext(Dispatchers.IO) {
        val log = TimeLogEntity(
            goalId = goalId,
            durationSeconds = durationSeconds,
            timestamp = System.currentTimeMillis(),
            note = note,
            isManual = isManual
        )
        val logId = timeLogDao.insertLog(log)

        // Check auto-completion for IN_TOTAL
        val goal = goalDao.getGoalByIdDirect(goalId)
        if (goal != null && goal.recurrenceType == RecurrenceType.IN_TOTAL && !goal.isCompleted) {
            val allLogs = timeLogDao.getLogsForGoalList(goalId)
            val totalLogged = allLogs.sumOf { it.durationSeconds }
            if (totalLogged >= goal.targetSeconds) {
                goalDao.updateGoal(
                    goal.copy(
                        isCompleted = true,
                        completedAt = System.currentTimeMillis()
                    )
                )
            }
        }

        logId
    }

    suspend fun insertNote(goalId: Long, content: String): Long = withContext(Dispatchers.IO) {
        goalNoteDao.insertNote(
            GoalNoteEntity(
                goalId = goalId,
                content = content,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteNoteById(id: Long) = withContext(Dispatchers.IO) {
        goalNoteDao.deleteNoteById(id)
    }

    suspend fun exportAllToJson(): String = withContext(Dispatchers.IO) {
        val allGoals = goalDao.getAllGoalsList()
        val allLogs = timeLogDao.getAllLogsList()
        val allNotes = goalNoteDao.getAllNotesList()

        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "Goal Tracker")
        root.put("exportTimestamp", System.currentTimeMillis())

        val goalsArray = JSONArray()
        for (g in allGoals) {
            val gObj = JSONObject()
            gObj.put("id", g.id)
            gObj.put("name", g.name)
            gObj.put("description", g.description)
            gObj.put("colorHex", g.colorHex)
            gObj.put("targetSeconds", g.targetSeconds)
            gObj.put("recurrenceType", g.recurrenceType.name)
            gObj.put("recurrenceDaysInterval", g.recurrenceDaysInterval)
            gObj.put("category", g.category)
            gObj.put("isFavorite", g.isFavorite)
            gObj.put("isArchived", g.isArchived)
            gObj.put("isCompleted", g.isCompleted)
            gObj.put("createdAt", g.createdAt)
            if (g.completedAt != null) gObj.put("completedAt", g.completedAt)
            gObj.put("totalCompletedDowntimeSeconds", g.totalCompletedDowntimeSeconds)
            if (g.lockedAverageSuccessRate != null) {
                gObj.put("lockedAverageSuccessRate", g.lockedAverageSuccessRate)
            }

            // Logs for this goal
            val logsArray = JSONArray()
            val gLogs = allLogs.filter { it.goalId == g.id }
            for (l in gLogs) {
                val lObj = JSONObject()
                lObj.put("durationSeconds", l.durationSeconds)
                lObj.put("timestamp", l.timestamp)
                lObj.put("note", l.note)
                lObj.put("isManual", l.isManual)
                logsArray.put(lObj)
            }
            gObj.put("logs", logsArray)

            // Notes for this goal
            val notesArray = JSONArray()
            val gNotes = allNotes.filter { it.goalId == g.id }
            for (n in gNotes) {
                val nObj = JSONObject()
                nObj.put("content", n.content)
                nObj.put("timestamp", n.timestamp)
                notesArray.put(nObj)
            }
            gObj.put("notes", notesArray)

            goalsArray.put(gObj)
        }

        root.put("goals", goalsArray)
        root.toString(2)
    }

    suspend fun importFromJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val goalsArray = root.optJSONArray("goals") ?: return@withContext Result.failure(
                Exception("Invalid JSON format: missing 'goals' array")
            )

            var importedCount = 0
            for (i in 0 until goalsArray.length()) {
                val gObj = goalsArray.getJSONObject(i)
                val recurrenceType = try {
                    RecurrenceType.valueOf(gObj.optString("recurrenceType", "DAILY"))
                } catch (e: Exception) {
                    RecurrenceType.DAILY
                }

                val goal = GoalEntity(
                    name = gObj.optString("name", "Goal ${i + 1}"),
                    description = gObj.optString("description", ""),
                    colorHex = gObj.optString("colorHex", "#8B5CF6"),
                    targetSeconds = gObj.optLong("targetSeconds", 3600L),
                    recurrenceType = recurrenceType,
                    recurrenceDaysInterval = gObj.optInt("recurrenceDaysInterval", 1),
                    category = gObj.optString("category", ""),
                    isFavorite = gObj.optBoolean("isFavorite", false),
                    isArchived = gObj.optBoolean("isArchived", false),
                    isCompleted = gObj.optBoolean("isCompleted", false),
                    createdAt = gObj.optLong("createdAt", System.currentTimeMillis()),
                    completedAt = if (gObj.has("completedAt")) gObj.getLong("completedAt") else null,
                    totalCompletedDowntimeSeconds = gObj.optLong("totalCompletedDowntimeSeconds", 0L),
                    lockedAverageSuccessRate = if (gObj.has("lockedAverageSuccessRate")) {
                        gObj.getDouble("lockedAverageSuccessRate")
                    } else null
                )

                val newGoalId = goalDao.insertGoal(goal)

                // Import logs
                val logsArray = gObj.optJSONArray("logs")
                if (logsArray != null) {
                    for (j in 0 until logsArray.length()) {
                        val lObj = logsArray.getJSONObject(j)
                        timeLogDao.insertLog(
                            TimeLogEntity(
                                goalId = newGoalId,
                                durationSeconds = lObj.optLong("durationSeconds", 0L),
                                timestamp = lObj.optLong("timestamp", System.currentTimeMillis()),
                                note = lObj.optString("note", ""),
                                isManual = lObj.optBoolean("isManual", false)
                            )
                        )
                    }
                }

                // Import notes
                val notesArray = gObj.optJSONArray("notes")
                if (notesArray != null) {
                    for (k in 0 until notesArray.length()) {
                        val nObj = notesArray.getJSONObject(k)
                        goalNoteDao.insertNote(
                            GoalNoteEntity(
                                goalId = newGoalId,
                                content = nObj.optString("content", ""),
                                timestamp = nObj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                }

                importedCount++
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
