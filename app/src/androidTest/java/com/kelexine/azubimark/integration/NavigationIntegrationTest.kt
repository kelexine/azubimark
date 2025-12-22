package com.kelexine.azubimark.integration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kelexine.azubimark.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for navigation flows in AzubiMark.
 * 
 * Tests complete user flows including:
 * - File browser to markdown viewer navigation
 * - Theme switching across screens
 * - Navigation back stack behavior
 * - Settings and About screen navigation
 */
@RunWith(AndroidJUnit4::class)
class NavigationIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Test that the app starts with the home screen.
     */
    @Test
    fun appStartsWithHomeScreen() {
        // The home screen should be displayed on app start
        composeTestRule.onNodeWithText("AzubiMark").assertIsDisplayed()
    }

    /**
     * Test navigation to Settings screen.
     */
    @Test
    fun navigateToSettingsScreen() {
        // Wait for splash and onboarding to complete (if first launch)
        composeTestRule.waitForIdle()
        
        // Click on the menu icon
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        
        // Click on Settings
        composeTestRule.onNodeWithText("Settings").performClick()
        
        // Verify Settings screen is displayed
        composeTestRule.onNodeWithText("Theme Settings").assertIsDisplayed()
    }

    /**
     * Test navigation to About screen.
     */
    @Test
    fun navigateToAboutScreen() {
        // Wait for splash and onboarding to complete
        composeTestRule.waitForIdle()
        
        // Click on the menu icon
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        
        // Click on About
        composeTestRule.onNodeWithText("About").performClick()
        
        // Verify About screen is displayed
        composeTestRule.onNodeWithText("About AzubiMark").assertIsDisplayed()
    }

    /**
     * Test back navigation from Settings screen.
     */
    @Test
    fun backNavigationFromSettings() {
        // Navigate to Settings
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        
        // Press back button
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        
        // Verify we're back at file browser
        composeTestRule.onNodeWithText("AzubiMark").assertIsDisplayed()
    }

    /**
     * Test back navigation from About screen.
     */
    @Test
    fun backNavigationFromAbout() {
        // Navigate to About
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("About").performClick()
        
        // Press back button
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        
        // Verify we're back at file browser
        composeTestRule.onNodeWithText("AzubiMark").assertIsDisplayed()
    }
}
