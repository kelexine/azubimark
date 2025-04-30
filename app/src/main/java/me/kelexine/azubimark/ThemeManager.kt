package me.kelexine.azubimark

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.color.DynamicColors

class ThemeManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    fun getCurrentTheme(): Int {
        return prefs.getInt(PREF_THEME, THEME_SYSTEM)
    }
    
    fun setTheme(theme: Int) {
        prefs.edit().putInt(PREF_THEME, theme).apply()
    }
    
    fun applyTheme() {
        when (getCurrentTheme()) {
            THEME_LIGHT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            THEME_DARK -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            THEME_MATERIAL_YOU -> {
                // Apply Material You theme if available, or fall back to system theme
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    DynamicColors.applyToActivityIfAvailable(context as AppCompatActivity)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
            else -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
    
    companion object {
        const val PREF_NAME = "theme_prefs"
        const val PREF_THEME = "selected_theme"
        
        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
        const val THEME_MATERIAL_YOU = 3
    }
}
