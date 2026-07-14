package com.example.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.models.DotScammerDao
import com.example.models.DotScammerEntity
import com.example.models.LocalContactDao
import com.example.models.LocalContactEntity
import com.example.models.ScamDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScamDatabaseTest {
    private lateinit var db: ScamDatabase
    private lateinit var dotDao: DotScammerDao
    private lateinit var contactDao: LocalContactDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Using an in-memory database for testing
        db = Room.inMemoryDatabaseBuilder(
            context, ScamDatabase::class.java
        ).allowMainThreadQueries().build()
        dotDao = db.dotScammerDao()
        contactDao = db.localContactDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeAndReadDotScammer() = runBlocking {
        val scammer = DotScammerEntity(
            phoneNumber = "+911234567890",
            threatCategory = "financial_fraud",
            severityScore = 90,
            lastReportedTimestamp = 123456789L
        )
        dotDao.insertAll(listOf(scammer))

        val retrieved = dotDao.getScammer("+911234567890")
        assertNotNull(retrieved)
        assertEquals("financial_fraud", retrieved?.threatCategory)
        assertEquals(90, retrieved?.severityScore)
        
        // Assert that a non-existent scammer returns null
        val missing = dotDao.getScammer("+910000000000")
        assertNull(missing)
    }

    @Test
    fun writeAndReadLocalContact() = runBlocking {
        val contact = LocalContactEntity(
            phoneNumber = "+919999999999",
            contactName = "Mom",
            isEmergencyGuardian = true,
            relationship = "family"
        )
        contactDao.insertAll(listOf(contact))

        val retrieved = contactDao.getContact("+919999999999")
        assertNotNull(retrieved)
        assertEquals("Mom", retrieved?.contactName)
        assertEquals(true, retrieved?.isEmergencyGuardian)
    }
}
