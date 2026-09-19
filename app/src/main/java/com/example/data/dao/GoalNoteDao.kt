package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.GoalNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalNoteDao {
    @Query("SELECT * FROM goal_notes WHERE goalId = :goalId ORDER BY timestamp DESC")
    fun getNotesForGoal(goalId: Long): Flow<List<GoalNoteEntity>>

    @Query("SELECT * FROM goal_notes WHERE goalId = :goalId ORDER BY timestamp DESC")
    suspend fun getNotesForGoalList(goalId: Long): List<GoalNoteEntity>

    @Query("SELECT * FROM goal_notes ORDER BY timestamp DESC")
    suspend fun getAllNotesList(): List<GoalNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: GoalNoteEntity): Long

    @Query("DELETE FROM goal_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM goal_notes WHERE goalId = :goalId")
    suspend fun deleteNotesForGoal(goalId: Long)
}
