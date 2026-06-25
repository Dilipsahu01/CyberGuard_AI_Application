package com.example.models

/**
 * CallLogRepository.kt
 * 
 * PURPOSE: 
 * This file acts as the Repository layer in the standard Android MVVM architecture. 
 * It completely separates the SQLite database access (CallLogDao) from the UI layer.
 * 
 * WHY IT EXISTS:
 * 1. Clean Architecture: Prevents the UI code from interacting directly with raw database queries.
 * 2. Thread Safety: It forces all database write operations (insert, delete) to safely execute 
 *    on a background thread (`Dispatchers.IO`). This guarantees the app's UI will never 
 *    freeze or crash due to a "DatabaseOnMainThread" exception.
 * 3. Reactive Data: Provides Kotlin `Flow` streams so the UI automatically updates 
 *    the moment a new scam call is inserted.
 */

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CallLogRepository(private val callLogDao: CallLogDao) {
    val allLogs: Flow<List<CallLog>> = callLogDao.getAllLogs()
    val scamLogs: Flow<List<CallLog>> = callLogDao.getScamLogs()

    suspend fun insertLog(log: CallLog): Long = withContext(Dispatchers.IO) {
        callLogDao.insertLog(log)
    }

    suspend fun deleteLog(log: CallLog) = withContext(Dispatchers.IO) {
        callLogDao.deleteLog(log)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        callLogDao.deleteAll()
    }
}
