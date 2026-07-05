package com.example.services

import android.content.Intent
import com.example.pipeline.PipelineManager
import com.example.pipeline.PipelineSingleton
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertTrue
import java.lang.reflect.Field

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
    fun `Rogue Coroutine Test - onDestroy deterministically cancels serviceScope and stops inference`() = runTest {
        val controller = Robolectric.buildService(ScamDetectionService::class.java)
        val service = controller.create().get()
        
        // Start the service to initialize the coroutine scopes
        service.onStartCommand(Intent(), 0, 1)
        
        // Use reflection to access the private serviceScope
        val scopeField: Field = ScamDetectionService::class.java.getDeclaredField("serviceScope")
        scopeField.isAccessible = true
        val serviceScope = scopeField.get(service) as CoroutineScope

        // Verify the scope is active
        assertTrue("ServiceScope should be active after start", serviceScope.isActive)

        // Destroy the service
        controller.destroy()

        // Verify the scope is immediately cancelled
        assertTrue("ServiceScope MUST be cancelled to prevent CPU thrashing", !serviceScope.isActive)
        
        // Verify serviceJob state if we had access to it, but isActive covers it.
    }

    @Test
    fun `Native Memory Flush - onDestroy strictly triggers PipelineSingleton clear for C++ teardown`() {
        val controller = Robolectric.buildService(ScamDetectionService::class.java)
        val service = controller.create().get()

        // Start service
        service.onStartCommand(Intent(), 0, 1)
        
        // Clear mock invocations from the startup phase
        clearMocks(PipelineSingleton, answers = false)
        
        // Trigger destruction
        controller.destroy()
        
        // Assert that exactly one call was made to clear() to release MappedByteBuffers
        verify(exactly = 1) { PipelineSingleton.clear() }
    }
}
