package com.example.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Database
import androidx.room.RoomDatabase

@Entity(tableName = "telemetry_queue")
data class TelemetryQueueItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "caller_hash") val callerHash: String,
    @ColumnInfo(name = "score") val score: Int,
    @ColumnInfo(name = "transcript") val transcript: String,
    @ColumnInfo(name = "intent_scores") val intentScores: String, // JSON string
    @ColumnInfo(name = "timestamp") val timestamp: Long
)

@Dao
interface TelemetryQueueDao {
    @Insert
    suspend fun insert(item: TelemetryQueueItem)

    @Query("SELECT * FROM telemetry_queue ORDER BY id ASC")
    suspend fun getAll(): List<TelemetryQueueItem>

    @Query("DELETE FROM telemetry_queue WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Database(entities = [TelemetryQueueItem::class], version = 1, exportSchema = false)
abstract class TelemetryDatabase : RoomDatabase() {
    abstract fun telemetryQueueDao(): TelemetryQueueDao
}
