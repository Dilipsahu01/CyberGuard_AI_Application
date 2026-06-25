package com.example.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.example.database.TelemetryDatabase
import com.example.database.TelemetryQueueItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class TelemetryQueueTest {
    private lateinit var db: TelemetryDatabase
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(context, TelemetryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testEnqueueAndFlush() = runBlocking {
        // Insert a telemetry item
        val item = TelemetryQueueItem(
            callerHash = "abc123",
            score = 42,
            transcript = "test transcript",
            intentScores = "[10,20,30,40,50]",
            timestamp = System.currentTimeMillis()
        )
        db.telemetryQueueDao().insert(item)

        // Verify insertion
        var items = db.telemetryQueueDao().getAll()
        assertEquals(1, items.size)
        assertEquals("abc123", items[0].callerHash)

        // Simulate a successful flush (delete by id)
        db.telemetryQueueDao().deleteById(items[0].id)
        items = db.telemetryQueueDao().getAll()
        assertTrue(items.isEmpty())
    }
}
