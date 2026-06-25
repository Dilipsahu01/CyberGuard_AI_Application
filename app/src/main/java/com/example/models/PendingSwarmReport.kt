package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.OnConflictStrategy

@Entity(tableName = "pending_swarm_reports")
data class PendingSwarmReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val payload: ByteArray,
    val callerNumber: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PendingSwarmReport
        if (id != other.id) return false
        if (timestamp != other.timestamp) return false
        if (!payload.contentEquals(other.payload)) return false
        if (callerNumber != other.callerNumber) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + callerNumber.hashCode()
        return result
    }
}

@Dao
interface PendingSwarmReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: PendingSwarmReport)

    @Query("SELECT * FROM pending_swarm_reports ORDER BY timestamp ASC")
    suspend fun getAllPendingReports(): List<PendingSwarmReport>

    @Delete
    suspend fun deleteReport(report: PendingSwarmReport)

    @Query("DELETE FROM pending_swarm_reports WHERE id = :id")
    suspend fun deleteReportById(id: Int)

    @Query("DELETE FROM pending_swarm_reports")
    suspend fun clearAll()
}
