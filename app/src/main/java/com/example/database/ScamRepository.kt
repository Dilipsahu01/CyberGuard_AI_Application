package com.example.database

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ScamRepository private constructor(private val scamDao: ScamDao) {

    // Singleton pattern holding reference to AppDatabase DAO
    companion object {
        @Volatile
        private var instance: ScamRepository? = null

        fun getInstance(appDatabase: AppDatabase): ScamRepository {
            return instance ?: synchronized(this) {
                instance ?: ScamRepository(appDatabase.scamDao()).also { instance = it }
            }
        }
    }

    val scamHistory: Flow<List<ScamCallEntity>> = scamDao.getAllScams()

    // Using `suspend` forces the caller to provide the Thread context
    suspend fun insertScam(call: ScamCallEntity) {
        scamDao.insertScam(call)
        
        // Auto-cleanup records older than 30 days (2,592,000,000 ms)
        val threshold = System.currentTimeMillis() - 2592000000L
        scamDao.deleteOldRecords(threshold)
    }

    suspend fun deleteOldRecords(threshold: Long) {
        scamDao.deleteOldRecords(threshold)
    }
}
