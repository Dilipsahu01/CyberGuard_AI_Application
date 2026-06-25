package com.example.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ContactMemoryDao {
    @Query("SELECT * FROM contact_memory WHERE numberHash = :hash LIMIT 1")
    suspend fun getMemory(hash: String): ContactMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMemory(memory: ContactMemoryEntity)
}
