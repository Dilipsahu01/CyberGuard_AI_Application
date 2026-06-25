package com.example.models

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<CallLog>>

    @Query("SELECT * FROM call_logs WHERE isScam = 1 ORDER BY timestamp DESC")
    fun getScamLogs(): Flow<List<CallLog>>

    @Query("SELECT * FROM call_logs WHERE timestamp > :timestamp ORDER BY timestamp DESC")
    fun getLogsAfter(timestamp: Long): List<CallLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CallLog): Long

    @Delete
    suspend fun deleteLog(log: CallLog)

    @Query("SELECT COUNT(*) FROM call_logs WHERE callerNumber = :number")
    suspend fun getCallCountForNumber(number: String): Int

    @Query("SELECT MIN(timestamp) FROM call_logs WHERE callerNumber = :number")
    suspend fun getFirstCallTimestampForNumber(number: String): Long?

    @Query("DELETE FROM call_logs")
    suspend fun deleteAll()
}
