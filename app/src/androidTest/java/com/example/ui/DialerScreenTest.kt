package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
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
    fun testInputFuzzing_MassiveString_DoesNotCrash() {
        val mockCallClick = mockk<(String) -> Unit>(relaxed = true)

        composeTestRule.setContent {
            DialerScreen(onCallClick = mockCallClick)
        }

        // Type a massive 50 digit string
        for (i in 1..50) {
            composeTestRule.onNodeWithText("9").performClick()
        }

        // Verify the UI scaled or truncated without throwing OutOfMemory or Compose layout exceptions
        val expectedNumber = "+91 " + "9".repeat(50)
        composeTestRule.onNodeWithText(expectedNumber).assertIsDisplayed()
    }

    @Test
    fun testInputFuzzing_SpecialCharacters_GracefullyHandlesT9() {
        val mockCallClick = mockk<(String) -> Unit>(relaxed = true)

        composeTestRule.setContent {
            DialerScreen(onCallClick = mockCallClick)
        }

        // Type complex engineering codes: *#*#4636#*#*
        val inputs = listOf("*", "#", "*", "#", "4", "6", "3", "6", "#", "*", "#", "*")
        inputs.forEach { digit ->
            composeTestRule.onNodeWithText(digit).performClick()
        }

        // Verify the T9 engine didn't throw a NullPointerException when encountering special chars
        composeTestRule.onNodeWithText("+91 *#*#4636#*#*").assertIsDisplayed()
    }

    @Test
    fun testKeypadAppendsDigitsAndShowsT9Contact() {
        val mockCallClick = mockk<(String) -> Unit>(relaxed = true)

        composeTestRule.setContent {
            DialerScreen(onCallClick = mockCallClick)
        }

        // Click keys for 3, 2, 3 (which matches the mock contact 'Dad')
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
        composeTestRule.onAllNodesWithText("+91 9876543211", substring = true).onFirst().assertIsDisplayed()
    }
}
