package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory
import com.example.security.DatabaseEncryptionManager

@Database(entities = [ScamCallEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scamDao(): ScamDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // MANDATE: Secure Storage
                val factory = try {
                    val passphrase = DatabaseEncryptionManager.getPassphrase(context)
                    SupportFactory(passphrase)
                } catch (t: Throwable) {
                    null
                }

                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cyberguard_app_database"
                )

                if (factory != null) {
                    builder.openHelperFactory(factory)
                }

                val instance = builder
                    // Add migration strategies here if necessary
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
