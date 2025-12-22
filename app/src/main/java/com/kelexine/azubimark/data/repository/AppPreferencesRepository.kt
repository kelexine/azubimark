package com.kelexine.azubimark.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences"
)

/**
 * Repository for managing app-wide preferences like onboarding state and recent files.
 */
class AppPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val RECENT_FILE_URIS = stringSetPreferencesKey("recent_file_uris")
    }

    /**
     * Flow indicating whether onboarding has been completed.
     */
    val onboardingCompleted: Flow<Boolean> = context.appPreferencesDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        }

    /**
     * Check if onboarding has been completed (blocking).
     */
    suspend fun isOnboardingCompleted(): Boolean {
        return context.appPreferencesDataStore.data.first()[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }

    /**
     * Mark onboarding as completed.
     */
    suspend fun setOnboardingCompleted() {
        context.appPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = true
        }
    }

    /**
     * Flow of recent file URIs.
     */
    val recentFileUris: Flow<Set<String>> = context.appPreferencesDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.RECENT_FILE_URIS] ?: emptySet()
        }

    /**
     * Add a file URI to recent files.
     */
    suspend fun addRecentFile(uri: String) {
        context.appPreferencesDataStore.edit { preferences ->
            val currentSet = preferences[PreferencesKeys.RECENT_FILE_URIS]?.toMutableSet() ?: mutableSetOf()
            // Keep only last 10 files
            if (currentSet.size >= 10) {
                currentSet.remove(currentSet.first())
            }
            currentSet.add(uri)
            preferences[PreferencesKeys.RECENT_FILE_URIS] = currentSet
        }
    }

    /**
     * Clear all recent files.
     */
    suspend fun clearRecentFiles() {
        context.appPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.RECENT_FILE_URIS] = emptySet()
        }
    }
}
