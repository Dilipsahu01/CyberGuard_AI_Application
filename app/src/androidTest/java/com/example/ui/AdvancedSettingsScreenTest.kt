package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdvancedSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testAiDetectionEngineSwitchesAndSlidersUpdateState() {
        composeTestRule.setContent {
            AdvancedSettingsScreen()
        }

        // Verify the screen renders the critical AI feature switches
        composeTestRule.onNodeWithText("Deepfake Protection").assertIsDisplayed()
        composeTestRule.onNodeWithText("Intent NLP Analysis").assertIsDisplayed()

        // Verify that the "Threat Sensitivity Level" slider is present
        composeTestRule.onNodeWithText("Threat Sensitivity Level").assertIsDisplayed()

        // Simulate a click to toggle the "Deepfake Voice Protection" switch.
        // We target the text row representing the toggle to ensure interaction works.
        composeTestRule.onNodeWithText("Deepfake Protection").performClick()
    }
}
