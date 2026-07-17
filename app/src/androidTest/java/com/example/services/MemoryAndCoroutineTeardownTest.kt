package com.example.services

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pipeline.PipelineManager
import com.example.pipeline.PipelineSingleton
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Field

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MemoryAndCoroutineTeardownTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockPipelineManager: PipelineManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockPipelineManager = mockk(relaxed = true)
        mockkObject(PipelineSingleton)
        every { PipelineSingleton.getInstance(any()) } returns mockPipelineManager
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun testRogueCoroutine_OnDestroyCancelsServiceScope() = runTest {
        // We simulate the lifecycle for the test scope
        val service = ScamDetectionService()
        
        // Use reflection to access the private serviceScope
        val scopeField: Field = ScamDetectionService::class.java.getDeclaredField("serviceScope")
        scopeField.isAccessible = true
        val serviceScope = scopeField.get(service) as CoroutineScope

        // Verify the scope is active
        assertTrue("ServiceScope should be active after start", serviceScope.isActive)

        // Call onDestroy manually for testing internal state (bypassing Android framework for this specific unit logic)
        service.onDestroy()

        // Verify the scope is immediately cancelled
        assertTrue("ServiceScope MUST be cancelled to prevent CPU thrashing", !serviceScope.isActive)
    }

    @Test
    fun testNativeMemoryFlush_OnDestroyTriggersPipelineClear() {
        // Instantiate the service directly to bypass Android framework permission checks
        val service = ScamDetectionService()
        
        // Trigger destruction
        service.onDestroy()
        
        // Assert that exactly one call was made to clear() to release MappedByteBuffers
        verify(exactly = 1) { PipelineSingleton.clear() }
    }
}
