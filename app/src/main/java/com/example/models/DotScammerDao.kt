package com.example.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DotScammerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scammers: List<DotScammerEntity>)

    @Query("SELECT * FROM dot_scammers WHERE phone_number = :phoneNumber LIMIT 1")
    suspend fun getScammer(phoneNumber: String): DotScammerEntity?
    
    @Query("SELECT * FROM dot_scammers")
    suspend fun getAll(): List<DotScammerEntity>
}
