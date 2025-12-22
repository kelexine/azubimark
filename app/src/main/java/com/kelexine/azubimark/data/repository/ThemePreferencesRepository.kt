package com.kelexine.azubimark.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kelexine.azubimark.data.model.ThemePreferences
import com.kelexine.azubimark.data.model.ThemeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing theme preferences using DataStore.
 */
class ThemePreferencesRepository(private val context: Context) {
    
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")
    
    companion object {
        private val THEME_TYPE_KEY = stringPreferencesKey("theme_type")
        private val DYNAMIC_COLORS_KEY = booleanPreferencesKey("dynamic_colors")
        private val CUSTOM_ACCENT_COLOR_KEY = longPreferencesKey("custom_accent_color")
    }
    
    /**
     * Flow of theme preferences.
     */
    val themePreferencesFlow: Flow<ThemePreferences> = context.dataStore.data.map { preferences ->
        val themeTypeString = preferences[THEME_TYPE_KEY] ?: ThemeType.SYSTEM.name
        val themeType = try {
            ThemeType.valueOf(themeTypeString)
        } catch (e: IllegalArgumentException) {
            ThemeType.SYSTEM
        }
        
        ThemePreferences(
            themeType = themeType,
            isDynamicColors = preferences[DYNAMIC_COLORS_KEY] ?: true,
            customAccentColor = preferences[CUSTOM_ACCENT_COLOR_KEY]
        )
    }
    
    /**
     * Save theme type preference.
     */
    suspend fun setThemeType(themeType: ThemeType) {
        context.dataStore.edit { preferences ->
            preferences[THEME_TYPE_KEY] = themeType.name
        }
    }
    
    /**
     * Save dynamic colors preference.
     */
    suspend fun setDynamicColors(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLORS_KEY] = enabled
        }
    }
    
    /**
     * Save custom accent color preference.
     */
    suspend fun setCustomAccentColor(color: Long?) {
        context.dataStore.edit { preferences ->
            if (color != null) {
                preferences[CUSTOM_ACCENT_COLOR_KEY] = color
            } else {
                preferences.remove(CUSTOM_ACCENT_COLOR_KEY)
            }
        }
    }
}
