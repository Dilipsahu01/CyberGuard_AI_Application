package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "dot_scammers")
data class DotScammerEntity(
    @PrimaryKey
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "threat_category")
    val threatCategory: String,

    @ColumnInfo(name = "severity_score")
    val severityScore: Int,

    @ColumnInfo(name = "last_reported_timestamp")
    val lastReportedTimestamp: Long
)
