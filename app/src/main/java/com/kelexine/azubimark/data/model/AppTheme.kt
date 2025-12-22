package com.kelexine.azubimark.data.model

import androidx.compose.material3.ColorScheme

/**
 * Represents the current application theme configuration.
 */
data class AppTheme(
    val type: ThemeType,
    val isDynamicColors: Boolean,
    val colorScheme: ColorScheme
)
