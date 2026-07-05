package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertExists
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDashboardRendersCoreComponentsAndSwarmStatus() {
        composeTestRule.setContent {
            DashboardScreen()
        }

        // Verify the Swarm Status badge renders
        composeTestRule.onNodeWithText("Swarm Intelligence Network Active").assertExists()

        // Verify the Hero Stats Card renders the mocked data
        composeTestRule.onNodeWithText("Threats Neutralized").assertExists()
        composeTestRule.onNodeWithText("27").assertExists() // Mocked count

        // Verify the Analytics Breakdown section displays all three AI models
        composeTestRule.onNodeWithText("Financial Coercion").assertExists()
        composeTestRule.onNodeWithText("Tech Support Scam").assertExists()
        composeTestRule.onNodeWithText("Romance / Trust").assertExists()
    }
}
