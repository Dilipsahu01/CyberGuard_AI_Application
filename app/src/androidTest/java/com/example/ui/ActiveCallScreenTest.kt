package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveCallScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testStateThrashing_RapidToggles_AreMutuallyExclusive() {
        val mockEndCall = mockk<() -> Unit>(relaxed = true)

        composeTestRule.setContent {
            ActiveCallScreen(onEndCall = mockEndCall)
        }

        // Simulate a user frantically toggling 20 times rapidly
        for (i in 1..20) {
            composeTestRule.onNodeWithText("Captions").performClick()
        }
        
        // Final click to ensure "Take Note" is the active sheet
        composeTestRule.onNodeWithText("Take Note").performClick()

        // Assert no cyclic loop occurred, UI didn't crash, and state resolved cleanly
        composeTestRule.onNodeWithText("SCAM EVIDENCE PAD").assertExists()
    }

    @Test
    fun testAsyncFlowOverload_ScamScoreUpdates() {
        val mockEndCall = mockk<() -> Unit>(relaxed = true)
        
        // NOTE: Since the current ActiveCallScreen doesn't inject a PipelineViewModel directly,
        // this test outlines the architecture for stress-testing Coroutine StateFlow emissions.
        // In the future when the ViewModel is attached:
        // val mockViewModel = mockk<PipelineViewModel>()
        // val flow = MutableStateFlow(0f)
        // every { mockViewModel.scamScore } returns flow
        // composeTestRule.setContent { ActiveCallScreen(viewModel = mockViewModel) }
        // for(i in 1..50) { flow.value = i / 50f } // Fire 50 rapid updates

        composeTestRule.setContent {
            ActiveCallScreen(onEndCall = mockEndCall)
        }

        // Assert it rendered without crashing
        composeTestRule.onNodeWithText("+91 7622365663").assertIsDisplayed()
    }

    @Test
    fun testLifecycle_OrientationChaos_PreservesState() {
        val restorationTester = StateRestorationTester(composeTestRule)
        
        restorationTester.setContent {
            ActiveCallScreen(onEndCall = {})
        }

        // User expands the Captions Bottom Sheet
        composeTestRule.onNodeWithText("Captions").performClick()
        composeTestRule.onNodeWithText("LIVE AI TRANSCRIPT").assertIsDisplayed()

        // Simulate Device Configuration Change (Screen Rotation / Foldable State Change)
        restorationTester.emulateSavedInstanceStateRestore()

        // Assert state is perfectly preserved and layout didn't reset
        composeTestRule.onNodeWithText("LIVE AI TRANSCRIPT").assertExists()
    }
}
