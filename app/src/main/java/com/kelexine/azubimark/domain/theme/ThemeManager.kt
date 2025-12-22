package com.kelexine.azubimark.domain.theme

import com.kelexine.azubimark.data.model.AppTheme
import com.kelexine.azubimark.data.model.ThemeType
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for managing application theme.
 */
interface ThemeManager {
    /**
     * Current theme state as a flow.
     */
    val currentTheme: StateFlow<AppTheme>

    /**
     * Set the theme type (Light, Dark, System).
     */
    suspend fun setTheme(theme: ThemeType)

    /**
     * Enable or disable dynamic colors (Material You).
     */
    suspend fun setDynamicColors(enabled: Boolean)
}
