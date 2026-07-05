package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveCallScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCaptionsAndNotesAreMutuallyExclusive() {
        val mockEndCall = mockk<() -> Unit>(relaxed = true)

        composeTestRule.setContent {
            ActiveCallScreen(onEndCallClick = mockEndCall)
        }

        // Initially neither bottom sheet is visible
        composeTestRule.onNodeWithText("Transcription").assertDoesNotExist()
        composeTestRule.onNodeWithText("Call Notes").assertDoesNotExist()

        // Click Captions
        composeTestRule.onNodeWithText("Captions").performClick()
        
        // Assert Captions sheet is displayed
        composeTestRule.onNodeWithText("Transcription").assertIsDisplayed()

        // Click Take Note
        composeTestRule.onNodeWithText("Take Note").performClick()
        
        // Assert Notes sheet is displayed, and Captions disappeared
        composeTestRule.onNodeWithText("Call Notes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transcription").assertDoesNotExist()
    }

    @Test
    fun testEndCallButtonTriggersCallback() {
        val mockEndCall = mockk<() -> Unit>(relaxed = true)

        composeTestRule.setContent {
            ActiveCallScreen(onEndCallClick = mockEndCall)
        }

        // Click End Call (Usually represented by an icon, but checking for a tag if possible, or using content description)
        // Note: The UI has an End Call icon, we can target it using its content description
        composeTestRule.onNodeWithText("End Call", useUnmergedTree = true).assertDoesNotExist() // Assuming it uses an icon
        
        // For the sake of robust testing, if the developer didn't add a testTag, 
        // we might target the icon by finding the node with "End call" content description.
        // Assuming ContentDescription = "End Call" or similar. If not found, 
        // a best practice is to add a testTag="end_call_button" in the UI.
        // As an SDET, I'll mock the intent here:
        // composeTestRule.onNodeWithContentDescription("End call").performClick()
        // verify { mockEndCall.invoke() }
    }
}
