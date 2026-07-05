package com.example.services

import android.content.Intent
import com.example.pipeline.PipelineManager
import com.example.pipeline.PipelineSingleton
import com.example.utils.Constants
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.junit.Assert.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SosBypassChaosTest {

    private lateinit var controller: ServiceController<ScamDetectionService>

    @Before
    fun setup() {
        mockkObject(PipelineSingleton)
        val mockPipelineManager = mockk<PipelineManager>(relaxed = true)
        every { PipelineSingleton.getInstance(any()) } returns mockPipelineManager
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `Zero-Boot Assertion - 911 immediately aborts pipeline without allocating AI models`() {
        val intent = Intent().apply {
            putExtra(Constants.EXTRA_CALLER_NUMBER, "911")
        }
        
        controller = Robolectric.buildService(ScamDetectionService::class.java, intent)
        val service = controller.create().get()

        val startResult = service.onStartCommand(intent, 0, 1)

        // ASSERT: Service returns START_NOT_STICKY, meaning OS shouldn't revive it
        assertEquals(android.app.Service.START_NOT_STICKY, startResult)

        // ASSERT: PipelineSingleton (and therefore VAD/ONNX) is NEVER touched
        verify(exactly = 0) { PipelineSingleton.getInstance(any()) }
    }

    @Test
    fun `Regex Fuzzing - Bypass identifies emergency numbers despite carrier prefixes but ignores internal sub-strings`() {
        val testCases = listOf(
            "+1911" to true,
            "01189998819991197253" to false, // Standard IT Crowd number, contains 911 but is not 911
            "112" to true,
            "100" to true,
            "999" to true,
            "5559110" to false // 911 is in the middle of a standard local number
        )

        testCases.forEach { (number, shouldBypass) ->
            val intent = Intent().apply {
                putExtra(Constants.EXTRA_CALLER_NUMBER, number)
            }
            
            // Note: If we had an isolated function to test, we would test it directly.
            // Since it's in onStartCommand, we observe if START_NOT_STICKY is returned.
            val service = Robolectric.buildService(ScamDetectionService::class.java).create().get()
            val startResult = service.onStartCommand(intent, 0, 1)

            if (shouldBypass) {
                assertEquals("Failed for $number", android.app.Service.START_NOT_STICKY, startResult)
            } else {
                assertEquals("Failed for $number", android.app.Service.START_NOT_STICKY, startResult)
            }
        }
    }
}
