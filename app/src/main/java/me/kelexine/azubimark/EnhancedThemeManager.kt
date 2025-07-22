package me.kelexine.azubimark

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors

class EnhancedThemeManager(private val context: Context) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val themePrefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    fun getCurrentTheme(): Int {
        return prefs.getString("theme", "0")?.toIntOrNull() ?: THEME_SYSTEM
    }
    
    fun setTheme(theme: Int) {
        prefs.edit().putString("theme", theme.toString()).apply()
        themePrefs.edit().putInt(PREF_THEME, theme).apply()
    }
    
    fun getFontSize(): Int {
        return prefs.getInt("font_size", 17)
    }
    
    fun getLineSpacing(): Float {
        return prefs.getInt("line_spacing", 18) / 10f
    }
    
    fun isReadingModeEnabled(): Boolean {
        return prefs.getBoolean("reading_mode", false)
    }
    
    fun isSyntaxHighlightingEnabled(): Boolean {
        return prefs.getBoolean("syntax_highlighting", true)
    }
    
    fun getCodeBlockStyle(): String {
        return prefs.getString("code_block_style", "rounded") ?: "rounded"
    }
    
    fun isAutoScrollOutlineEnabled(): Boolean {
        return prefs.getBoolean("auto_scroll_outline", true)
    }
    
    fun shouldShowLineNumbers(): Boolean {
        return prefs.getBoolean("show_line_numbers", false)
    }
    
    fun isHighContrastEnabled(): Boolean {
        return prefs.getBoolean("high_contrast", false)
    }
    
    fun shouldUseLargeTouchTargets(): Boolean {
        return prefs.getBoolean("large_touch_targets", false)
    }
    
    fun isDebugModeEnabled(): Boolean {
        return prefs.getBoolean("debug_mode", false)
    }
    
    fun applyTheme() {
        val theme = getCurrentTheme()
        when (theme) {
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
        
        // Apply additional theme customizations
        applyAccessibilitySettings()
    }
    
    private fun applyAccessibilitySettings() {
        // Apply high contrast, large touch targets, etc.
        // These would typically be applied through custom themes or view modifications
    }
    
    fun getThemeName(): String {
        return when (getCurrentTheme()) {
            THEME_LIGHT -> "Light"
            THEME_DARK -> "Dark"
            THEME_MATERIAL_YOU -> "Material You"
            else -> "System"
        }
    }
    
    fun isDarkTheme(): Boolean {
        return when (getCurrentTheme()) {
            THEME_DARK -> true
            THEME_LIGHT -> false
            else -> {
                // Check system theme
                val nightModeFlags = context.resources.configuration.uiMode and 
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }
    
    fun exportSettings(): Map<String, Any?> {
        val allPrefs = prefs.all
        return mapOf(
            "theme" to getCurrentTheme(),
            "font_size" to getFontSize(),
            "line_spacing" to getLineSpacing(),
            "reading_mode" to isReadingModeEnabled(),
            "syntax_highlighting" to isSyntaxHighlightingEnabled(),
            "code_block_style" to getCodeBlockStyle(),
            "auto_scroll_outline" to isAutoScrollOutlineEnabled(),
            "show_line_numbers" to shouldShowLineNumbers(),
            "high_contrast" to isHighContrastEnabled(),
            "large_touch_targets" to shouldUseLargeTouchTargets(),
            "debug_mode" to isDebugModeEnabled(),
            "all_preferences" to allPrefs
        )
    }
    
    fun importSettings(settings: Map<String, Any?>) {
        val editor = prefs.edit()
        settings.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Float -> editor.putFloat(key, value)
                is Long -> editor.putLong(key, value)
            }
        }
        editor.apply()
    }
    
    fun resetToDefaults() {
        prefs.edit().clear().apply()
        themePrefs.edit().clear().apply()
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