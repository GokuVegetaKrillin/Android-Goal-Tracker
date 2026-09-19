package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE isArchived = 0 AND isCompleted = 0 ORDER BY id DESC")
    fun getAllActiveGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE isFavorite = 1 AND isArchived = 0 AND isCompleted = 0 ORDER BY id DESC")
    fun getFavoriteGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE isCompleted = 1 ORDER BY completedAt DESC, id DESC")
    fun getCompletedGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE isArchived = 1 ORDER BY id DESC")
    fun getArchivedGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY id DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY id DESC")
    suspend fun getAllGoalsList(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :id")
    fun getGoalById(id: Long): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalByIdDirect(id: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)
}
