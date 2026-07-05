package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DialerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testKeypadAppendsDigitsAndShowsT9Contact() {
        val mockCallClick = mockk<(String) -> Unit>(relaxed = true)

        composeTestRule.setContent {
            DialerScreen(onCallClick = mockCallClick)
        }

        // Click keys for 3, 2, 3 (which matches the T9 mapping for 'DAD' and the mock contact 'Dad')
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()

        // Verify the dialer correctly displays the dialed number
        composeTestRule.onNodeWithText("+91 323", substring = true).assertIsDisplayed()

        // Verify that the T9 Predictive Search logic shows the "Dad" contact chip
        composeTestRule.onNodeWithText("Dad").assertIsDisplayed()

        // Click the "Dad" chip to auto-fill the number
        composeTestRule.onNodeWithText("Dad").performClick()

        // Verify it autofilled the full number "+91 9876543211"
        composeTestRule.onNodeWithText("+91 9876543211", substring = true).assertIsDisplayed()
    }
}
