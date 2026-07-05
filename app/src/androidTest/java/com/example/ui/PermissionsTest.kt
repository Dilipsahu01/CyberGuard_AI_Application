package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testPermissionsOnboardingScreenRendersAndClicks() {
        val mockGrantPermissions = mockk<() -> Unit>(relaxed = true)

        composeTestRule.setContent {
            PermissionsOnboardingScreen(onGrantPermissions = mockGrantPermissions)
        }

        // Verify the screen renders the necessary explanations for the 7-stage pipeline
        composeTestRule.onNodeWithText("Protecting Your Calls").assertIsDisplayed()
        composeTestRule.onNodeWithText("Microphone").assertIsDisplayed()
        composeTestRule.onNodeWithText("Phone/Dialer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Contacts").assertIsDisplayed()

        // Perform click on the grant button
        composeTestRule.onNodeWithText("Grant Permissions").performClick()

        // Verify callback is triggered
        verify(exactly = 1) { mockGrantPermissions.invoke() }
    }

    /**
     * Helper function to automate clicking "Allow" on standard Android System Permission Dialogs.
     * This uses UIAutomator to find the system button by text/resource-id.
     */
    fun grantSystemPermissionAutomated() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        
        // This targets the standard Android "Allow" or "While using the app" buttons
        val allowButton = device.findObject(
            UiSelector().textMatches("(?i)allow|while using the app|only this time")
        )
        
        if (allowButton.exists()) {
            allowButton.click()
        }
    }
}
