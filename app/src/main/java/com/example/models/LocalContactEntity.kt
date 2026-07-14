package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "local_contacts")
data class LocalContactEntity(
    @PrimaryKey
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "contact_name")
    val contactName: String,

    @ColumnInfo(name = "is_emergency_guardian")
    val isEmergencyGuardian: Boolean,

    @ColumnInfo(name = "relationship")
    val relationship: String
)
