package com.example.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedNumberDao {
    @Query("SELECT * FROM blocked_numbers ORDER BY blockedAt DESC")
    fun getAll(): Flow<List<BlockedNumberEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE phoneNumber = :number)")
    suspend fun isBlocked(number: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun block(entity: BlockedNumberEntity)

    @Delete
    suspend fun unblock(entity: BlockedNumberEntity)
}
