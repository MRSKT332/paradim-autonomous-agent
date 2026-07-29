package com.example.data.dao

import androidx.room.*
import com.example.data.entity.TaskExecutionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskExecutionDao {
    @Query("SELECT * FROM task_executions ORDER BY timestamp DESC")
    fun getAllTaskExecutions(): Flow<List<TaskExecutionEntity>>

    @Query("SELECT * FROM task_executions WHERE id = :id")
    suspend fun getById(id: String): TaskExecutionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(task: TaskExecutionEntity)

    @Query("DELETE FROM task_executions")
    suspend fun clearHistory()
}
