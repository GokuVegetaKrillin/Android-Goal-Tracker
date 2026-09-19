package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TimeLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeLogDao {
    @Query("SELECT * FROM time_logs WHERE goalId = :goalId ORDER BY timestamp DESC")
    fun getLogsForGoal(goalId: Long): Flow<List<TimeLogEntity>>

    @Query("SELECT * FROM time_logs WHERE goalId = :goalId ORDER BY timestamp DESC")
    suspend fun getLogsForGoalList(goalId: Long): List<TimeLogEntity>

    @Query("SELECT * FROM time_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<TimeLogEntity>>

    @Query("SELECT * FROM time_logs ORDER BY timestamp DESC")
    suspend fun getAllLogsList(): List<TimeLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TimeLogEntity): Long

    @Query("DELETE FROM time_logs WHERE goalId = :goalId")
    suspend fun deleteLogsForGoal(goalId: Long)

    @Query("DELETE FROM time_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)
}
