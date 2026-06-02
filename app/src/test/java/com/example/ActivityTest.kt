package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ActivityTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testNavigation() {
        composeTestRule.onNodeWithText("MLock").assertExists()
        composeTestRule.onNodeWithText("Security Setup").performClick()
        composeTestRule.onNodeWithText("Authentication Methods").assertExists()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        
        composeTestRule.onNodeWithText("App Lock").performClick()
        composeTestRule.onNodeWithText("App Lock").assertExists()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Hide Notifications").assertExists()
    }
}
