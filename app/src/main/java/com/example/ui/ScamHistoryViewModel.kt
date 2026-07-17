package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.database.ScamCallEntity
import com.example.database.ScamRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScamHistoryViewModel(private val repository: ScamRepository) : ViewModel() {

    // Expose the Flow as StateFlow using WhileSubscribed for resource efficiency
    val scamHistory: StateFlow<List<ScamCallEntity>> = repository.scamHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun clearHistory() {
        viewModelScope.launch {
            // Task 2: invokes the deleteOldRecords function with a threshold of 0
            repository.deleteOldRecords(0)
        }
    }

    // Factory for manual Dependency Injection since we need to pass the Repository
    class Factory(private val repository: ScamRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ScamHistoryViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ScamHistoryViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
