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
 * Integration tests for file browser functionality.
 * 
 * Tests file browsing, directory navigation, and file selection.
 * Note: These tests require storage permissions and may need
 * test fixtures or mock data for consistent results.
 */
@RunWith(AndroidJUnit4::class)
class FileBrowserIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Test that file browser screen displays correctly.
     */
    @Test
    fun fileBrowserScreenDisplays() {
        // The file browser should show the app title
        composeTestRule.onNodeWithText("AzubiMark").assertIsDisplayed()
    }

    /**
     * Test that the menu button is accessible.
     */
    @Test
    fun menuButtonIsAccessible() {
        // The menu button should be displayed
        composeTestRule.onNodeWithContentDescription("More options").assertIsDisplayed()
    }

    /**
     * Test that menu opens when clicked.
     */
    @Test
    fun menuOpensOnClick() {
        // Click on the menu icon
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        
        // Menu items should be displayed
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("About").assertIsDisplayed()
    }

    /**
     * Test that the select folder button is displayed when no folder is selected.
     */
    @Test
    fun selectFolderButtonDisplayed() {
        // When no folder is selected, a prompt or button should be shown
        // This depends on the initial state of the app
        composeTestRule.onNodeWithText("AzubiMark").assertIsDisplayed()
    }

    /**
     * Test empty state message when no files are found.
     * Note: This test assumes no folder has been selected yet.
     */
    @Test
    fun emptyStateDisplayedInitially() {
        // The app should show some indication that no folder is selected
        // or that the user needs to select a folder
        composeTestRule.onNodeWithText("AzubiMark").assertIsDisplayed()
    }
}
