package com.kelexine.azubimark.data.model

/**
 * Data class for serializing theme preferences to DataStore.
 */
data class ThemePreferences(
    val themeType: ThemeType = ThemeType.SYSTEM,
    val isDynamicColors: Boolean = true,
    val customAccentColor: Long? = null
)
