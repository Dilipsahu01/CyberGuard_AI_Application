package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScamDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScam(call: ScamCallEntity)

    @Query("SELECT * FROM scam_calls ORDER BY timestamp DESC")
    fun getAllScams(): Flow<List<ScamCallEntity>>

    @Query("DELETE FROM scam_calls WHERE timestamp < :threshold")
    suspend fun deleteOldRecords(threshold: Long)

    @Query("DELETE FROM scam_calls")
    suspend fun deleteAllScams()
}
