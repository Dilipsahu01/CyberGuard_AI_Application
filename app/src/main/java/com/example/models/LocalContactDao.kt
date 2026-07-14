package com.example.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocalContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<LocalContactEntity>)

    @Query("SELECT * FROM local_contacts WHERE phone_number = :phoneNumber LIMIT 1")
    suspend fun getContact(phoneNumber: String): LocalContactEntity?
    
    @Query("SELECT * FROM local_contacts")
    suspend fun getAll(): List<LocalContactEntity>
}
