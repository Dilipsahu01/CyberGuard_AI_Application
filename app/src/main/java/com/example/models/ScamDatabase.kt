package com.example.models

/**
 * ScamDatabase.kt
 * 
 * PURPOSE: 
 * The central Room SQLite Database configuration for the app.
 * 
 * WHY IT EXISTS:
 * Defines the tables for `CallLog` (history), `ContactMemory` (Pig Butchering states), 
 * and `PendingSwarmReport` (Offline Telemetry). Ensures type safety and thread-safe DB access.
 */
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CallLog::class, PendingSwarmReport::class, ContactMemoryEntity::class], version = 3, exportSchema = false)
abstract class ScamDatabase : RoomDatabase() {
    abstract fun callLogDao(): CallLogDao
    abstract fun pendingSwarmReportDao(): PendingSwarmReportDao
    abstract fun contactMemoryDao(): ContactMemoryDao

    companion object {
        @Volatile
        private var INSTANCE: ScamDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pending_swarm_reports` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `timestamp` INTEGER NOT NULL, 
                        `payload` BLOB NOT NULL, 
                        `callerNumber` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `contact_memory` (
                        `numberHash` TEXT PRIMARY KEY NOT NULL, 
                        `memory` BLOB NOT NULL, 
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): ScamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScamDatabase::class.java,
                    "scam_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
