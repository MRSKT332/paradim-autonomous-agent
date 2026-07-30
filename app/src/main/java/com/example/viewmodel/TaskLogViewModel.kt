package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.TaskHistoryDatabase
import com.example.data.entity.ExecutionPathType
import com.example.data.entity.TaskExecutionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskLogViewModel(application: Application) : AndroidViewModel(application) {

    private val historyDb = TaskHistoryDatabase.getDatabase(application)
    private val mainDb = AppDatabase.getDatabase(application)

    private val _selectedFilter = MutableStateFlow<ExecutionPathType?>(null)
    val selectedFilter: StateFlow<ExecutionPathType?> = _selectedFilter.asStateFlow()

    // Real-time flow combining both databases for task execution history
    val taskExecutions: StateFlow<List<TaskExecutionEntity>> = combine(
        historyDb.taskExecutionDao().getAllTaskExecutions(),
        mainDb.taskExecutionDao().getAllTaskExecutions(),
        _selectedFilter
    ) { historyTasks, mainTasks, filter ->
        val mergedMap = mutableMapOf<String, TaskExecutionEntity>()
        (historyTasks + mainTasks).forEach { task ->
            val existing = mergedMap[task.id]
            if (existing == null || task.timestamp >= existing.timestamp) {
                mergedMap[task.id] = task
            }
        }
        val allSorted = mergedMap.values.sortedByDescending { it.timestamp }
        if (filter == null) {
            allSorted
        } else {
            allSorted.filter { it.pathType == filter }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: ExecutionPathType?) {
        _selectedFilter.value = filter
    }

    fun clearTaskHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyDb.taskExecutionDao().clearHistory()
            mainDb.taskExecutionDao().clearHistory()
        }
    }

    fun recordTaskExecution(task: TaskExecutionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            historyDb.taskExecutionDao().insertOrUpdate(task)
            mainDb.taskExecutionDao().insertOrUpdate(task)
        }
    }
}
