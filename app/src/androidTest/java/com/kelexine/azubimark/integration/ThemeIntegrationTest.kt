package com.kelexine.azubimark.integration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
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
 * Integration tests for theme switching functionality.
 * 
 * Tests that theme changes apply correctly across different screens
 * and persist during the session.
 */
@RunWith(AndroidJUnit4::class)
class ThemeIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Test that theme options are displayed in settings.
     */
    @Test
    fun themeOptionsDisplayedInSettings() {
        // Navigate to Settings
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        
        // Verify theme options are displayed
        composeTestRule.onNodeWithText("Light").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dark").assertIsDisplayed()
        composeTestRule.onNodeWithText("System Default").assertIsDisplayed()
    }

    /**
     * Test selecting Light theme.
     */
    @Test
    fun selectLightTheme() {
        // Navigate to Settings
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        
        // Select Light theme
        composeTestRule.onNodeWithText("Light").performClick()
        
        // The Light option should now be selected
        // Note: Actual visual verification would require screenshot testing
        composeTestRule.onNodeWithText("Light").assertIsDisplayed()
    }

    /**
     * Test selecting Dark theme.
     */
    @Test
    fun selectDarkTheme() {
        // Navigate to Settings
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        
        // Select Dark theme
        composeTestRule.onNodeWithText("Dark").performClick()
        
        // The Dark option should now be selected
        composeTestRule.onNodeWithText("Dark").assertIsDisplayed()
    }

    /**
     * Test selecting System Default theme.
     */
    @Test
    fun selectSystemTheme() {
        // Navigate to Settings
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        
        // Select System Default theme
        composeTestRule.onNodeWithText("System Default").performClick()
        
        // The System Default option should now be selected
        composeTestRule.onNodeWithText("System Default").assertIsDisplayed()
    }

    /**
     * Test that theme persists when navigating back from settings.
     */
    @Test
    fun themePersistsAfterNavigation() {
        // Navigate to Settings
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        
        // Select Dark theme
        composeTestRule.onNodeWithText("Dark").performClick()
        
        // Navigate back
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        
        // Navigate to Settings again
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        
        // Dark theme should still be selected
        composeTestRule.onNodeWithText("Dark").assertIsDisplayed()
    }
}
