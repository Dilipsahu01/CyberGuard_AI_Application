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

    suspend fun insertScam(call: ScamCallEntity) {
        scamDao.insertScam(call)
        // Cleanup records older than 30 days
        val threshold = System.currentTimeMillis() - 2592000000L
        scamDao.deleteOldRecords(threshold)
    }
}
