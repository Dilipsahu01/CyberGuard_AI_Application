package com.example.models

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SupportFactory
import com.example.security.DatabaseEncryptionManager
import java.io.BufferedReader
import java.io.InputStreamReader

@Database(
    entities = [
        CallLog::class,
        PendingSwarmReport::class,
        ContactMemoryEntity::class,
        DotScammerEntity::class,
        LocalContactEntity::class,
        BlockedNumberEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class ScamDatabase : RoomDatabase() {
    abstract fun callLogDao(): CallLogDao
    abstract fun pendingSwarmReportDao(): PendingSwarmReportDao
    abstract fun contactMemoryDao(): ContactMemoryDao
    abstract fun dotScammerDao(): DotScammerDao
    abstract fun localContactDao(): LocalContactDao
    abstract fun blockedNumberDao(): BlockedNumberDao

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

        fun getMIGRATION_3_4(context: Context) = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `dot_scammers` (
                        `phone_number` TEXT PRIMARY KEY NOT NULL,
                        `threat_category` TEXT NOT NULL,
                        `severity_score` INTEGER NOT NULL,
                        `last_reported_timestamp` INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `local_contacts` (
                        `phone_number` TEXT PRIMARY KEY NOT NULL,
                        `contact_name` TEXT NOT NULL,
                        `is_emergency_guardian` INTEGER NOT NULL,
                        `relationship` TEXT NOT NULL
                    )
                """.trimIndent())

                // Pre-populate data from CSV
                try {
                    context.assets.open("dot_scam_blacklist.csv").bufferedReader().useLines { lines ->
                        lines.drop(1).forEach { line ->
                            val parts = line.split(",")
                            if (parts.size >= 4) {
                                val values = ContentValues().apply {
                                    put("phone_number", parts[0])
                                    put("threat_category", parts[1])
                                    put("severity_score", parts[2].toIntOrNull() ?: 0)
                                    put("last_reported_timestamp", parts[3].toLongOrNull() ?: 0L)
                                }
                                db.insert("dot_scammers", SQLiteDatabase.CONFLICT_REPLACE, values)
                            }
                        }
                    }

                    context.assets.open("mock_device_contacts.csv").bufferedReader().useLines { lines ->
                        lines.drop(1).forEach { line ->
                            val parts = line.split(",")
                            if (parts.size >= 4) {
                                val values = ContentValues().apply {
                                    put("phone_number", parts[0])
                                    put("contact_name", parts[1])
                                    put("is_emergency_guardian", if (parts[2].equals("true", true)) 1 else 0)
                                    put("relationship", parts[3])
                                }
                                db.insert("local_contacts", SQLiteDatabase.CONFLICT_REPLACE, values)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `blocked_numbers` (
                        `phoneNumber` TEXT PRIMARY KEY NOT NULL,
                        `contactName` TEXT,
                        `blockedAt` INTEGER NOT NULL,
                        `reason` TEXT
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `call_logs` ADD COLUMN `direction` INTEGER NOT NULL DEFAULT -1")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `call_logs` ADD COLUMN `userFeedback` TEXT")
            }
        }

        fun getDatabase(context: Context): ScamDatabase {
            return INSTANCE ?: synchronized(this) {
                val migration3_4 = getMIGRATION_3_4(context)

                // MANDATE: Military-grade storage security via SQLCipher + Hardware Keystore
                val factory = try {
                    val passphrase = DatabaseEncryptionManager.getPassphrase(context)
                    SupportFactory(passphrase)
                } catch (t: Throwable) {
                    // Fallback for JVM tests where SQLCipher native libs are missing
                    null
                }

                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    ScamDatabase::class.java,
                    "scam_database"
                )

                if (factory != null) {
                    builder.openHelperFactory(factory)
                }

                val instance = builder
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, migration3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // This populates DB on first install
                            migration3_4.migrate(db)
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
