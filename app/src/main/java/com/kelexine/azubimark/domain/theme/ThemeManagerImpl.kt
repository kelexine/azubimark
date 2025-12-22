package com.kelexine.azubimark.domain.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import com.kelexine.azubimark.data.model.AppTheme
import com.kelexine.azubimark.data.model.ThemeType
import com.kelexine.azubimark.data.repository.ThemePreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Implementation of ThemeManager with Material You support.
 * 
 * Manages application theme state and provides immediate theme switching.
 * Supports Material You dynamic colors on Android 12+ (API 31+).
 */
class ThemeManagerImpl(
    private val context: Context,
    private val repository: ThemePreferencesRepository,
    private val scope: CoroutineScope
) : ThemeManager {
    
    private val _currentTheme = MutableStateFlow(createDefaultTheme())
    override val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()
    
    // Track system theme changes
    private val _systemThemeIsDark = MutableStateFlow(isSystemInDarkTheme())
    
    init {
        // Observe theme preferences and system theme changes
        scope.launch {
            combine(
                repository.themePreferencesFlow,
                _systemThemeIsDark
            ) { preferences, systemIsDark ->
                createAppTheme(preferences.themeType, preferences.isDynamicColors, systemIsDark)
            }.collect { theme ->
                _currentTheme.value = theme
            }
        }
    }
    
    override suspend fun setTheme(theme: ThemeType) {
        repository.setThemeType(theme)
        // StateFlow will be updated automatically through the combine flow
    }
    
    override suspend fun setDynamicColors(enabled: Boolean) {
        repository.setDynamicColors(enabled)
        // StateFlow will be updated automatically through the combine flow
    }
    
    /**
     * Call this method when system theme changes (e.g., from configuration change).
     */
    fun onSystemThemeChanged(isDark: Boolean) {
        _systemThemeIsDark.value = isDark
    }
    
    private fun createDefaultTheme(): AppTheme {
        val isDark = isSystemInDarkTheme()
        return AppTheme(
            type = ThemeType.SYSTEM,
            isDynamicColors = true,
            colorScheme = getColorScheme(ThemeType.SYSTEM, true, isDark)
        )
    }
    
    private fun createAppTheme(
        themeType: ThemeType,
        isDynamicColors: Boolean,
        systemIsDark: Boolean
    ): AppTheme {
        return AppTheme(
            type = themeType,
            isDynamicColors = isDynamicColors,
            colorScheme = getColorScheme(themeType, isDynamicColors, systemIsDark)
        )
    }
    
    private fun getColorScheme(
        themeType: ThemeType,
        isDynamicColors: Boolean,
        systemIsDark: Boolean
    ): ColorScheme {
        val shouldUseDarkTheme = when (themeType) {
            ThemeType.LIGHT -> false
            ThemeType.DARK -> true
            ThemeType.SYSTEM -> systemIsDark
        }
        
        return if (isDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (shouldUseDarkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        } else {
            if (shouldUseDarkTheme) {
                darkColorScheme()
            } else {
                lightColorScheme()
            }
        }
    }
    
    private fun isSystemInDarkTheme(): Boolean {
        val uiMode = context.resources.configuration.uiMode
        return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}
