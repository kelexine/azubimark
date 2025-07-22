package me.kelexine.azubimark

import android.app.Application
import android.util.Log
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Application class for AzubiMark
 * Initializes global systems and handles app-wide configuration
 */
class AzubiMarkApplication : Application() {
    
    companion object {
        private const val TAG = "AzubiMarkApp"
        lateinit var instance: AzubiMarkApplication
            private set
    }
    
    // Global managers
    val cacheManager: CacheManager by lazy { CacheManager.getInstance(this) }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        initializeApplication()
    }
    
    /**
     * Initialize application components
     */
    private fun initializeApplication() {
        Log.d(TAG, "Initializing AzubiMark application...")
        
        // Initialize error handling
        ErrorHandler.initializeGlobalErrorHandler(this)
        
        // Set default preferences if this is first launch
        setDefaultPreferences()
        
        // Initialize cache management
        initializeCacheManagement()
        
        // Perform background initialization
        performBackgroundInitialization()
        
        Log.d(TAG, "Application initialization complete")
    }
    
    /**
     * Set default preferences for first-time users
     */
    private fun setDefaultPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        
        // Only set defaults if this is the first launch
        if (!prefs.contains("first_launch_completed")) {
            prefs.edit().apply {
                // Appearance defaults
                putString("theme", "0") // System default
                putInt("font_size", 17)
                putInt("line_spacing", 18)
                putBoolean("reading_mode", false)
                
                // Editor defaults
                putBoolean("syntax_highlighting", true)
                putString("code_block_style", "rounded")
                putBoolean("auto_scroll_outline", true)
                putBoolean("show_line_numbers", false)
                
                // File browser defaults
                putString("start_location", "downloads")
                putBoolean("show_hidden_files", false)
                putBoolean("sort_folders_first", true)
                putString("file_sort_order", "name_asc")
                
                // Accessibility defaults
                putBoolean("high_contrast", false)
                putBoolean("large_touch_targets", false)
                putBoolean("voice_feedback", false)
                
                // Advanced defaults
                putBoolean("debug_mode", false)
                
                // Mark first launch as completed
                putBoolean("first_launch_completed", true)
                
                apply()
            }
            
            Log.d(TAG, "Default preferences set for first-time user")
        }
    }
    
    /**
     * Initialize cache management system
     */
    private fun initializeCacheManagement() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Clean up expired cache entries
                cacheManager.cleanupExpiredEntries()
                
                val stats = cacheManager.getCacheStats()
                Log.d(TAG, "Cache initialized: $stats")
            } catch (e: Exception) {
                Log.w(TAG, "Cache initialization failed", e)
            }
        }
    }
    
    /**
     * Perform background initialization tasks
     */
    private fun performBackgroundInitialization() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Pre-warm commonly used resources
                preWarmResources()
                
                // Check for app updates or migrations
                checkForMigrations()
                
                // Optimize database or cache if needed
                optimizeStorage()
                
                Log.d(TAG, "Background initialization completed")
            } catch (e: Exception) {
                Log.w(TAG, "Background initialization failed", e)
            }
        }
    }
    
    /**
     * Pre-warm resources that are commonly used
     */
    private fun preWarmResources() {
        // Pre-load theme configurations
        EnhancedThemeManager(this).getCurrentTheme()
        
        // Initialize markdown processor components
        // This could include pre-compiling regex patterns or loading syntax highlighting themes
    }
    
    /**
     * Check for app migrations between versions
     */
    private fun checkForMigrations() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val lastVersion = prefs.getInt("last_app_version", 0)
        val currentVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionCode
        } catch (e: Exception) {
            0
        }
        
        if (lastVersion < currentVersion) {
            Log.d(TAG, "Migrating from version $lastVersion to $currentVersion")
            
            // Perform version-specific migrations
            when {
                lastVersion < 120 -> {
                    // Migration for versions before 1.2.0
                    migrateLegacySettings()
                }
                lastVersion < 123 -> {
                    // Migration for current version
                    migrateToNewCacheSystem()
                }
            }
            
            // Update stored version
            prefs.edit().putInt("last_app_version", currentVersion).apply()
            Log.d(TAG, "Migration completed to version $currentVersion")
        }
    }
    
    /**
     * Migrate legacy settings to new format
     */
    private fun migrateLegacySettings() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        
        // Example: Migrate old theme settings
        if (prefs.contains("old_theme_setting")) {
            val oldTheme = prefs.getString("old_theme_setting", "system")
            val newTheme = when (oldTheme) {
                "light" -> "1"
                "dark" -> "2"
                "auto" -> "3"
                else -> "0"
            }
            prefs.edit()
                .putString("theme", newTheme)
                .remove("old_theme_setting")
                .apply()
        }
    }
    
    /**
     * Migrate to new cache system
     */
    private fun migrateToNewCacheSystem() {
        // Clear old cache format if it exists
        val oldCacheDir = java.io.File(cacheDir, "old_markdown_cache")
        if (oldCacheDir.exists()) {
            oldCacheDir.deleteRecursively()
            Log.d(TAG, "Cleared legacy cache directory")
        }
    }
    
    /**
     * Optimize storage and cache
     */
    private fun optimizeStorage() {
        // Clean up temporary files
        val tempDir = java.io.File(cacheDir, "temp")
        if (tempDir.exists()) {
            tempDir.listFiles()?.forEach { file ->
                if (System.currentTimeMillis() - file.lastModified() > 24 * 60 * 60 * 1000) {
                    file.delete()
                }
            }
        }
        
        // Optimize cache size
        val stats = cacheManager.getCacheStats()
        val diskCacheSize = stats["diskCacheSize"] as? Long ?: 0L
        val maxCacheSize = 50 * 1024 * 1024L // 50MB
        
        if (diskCacheSize > maxCacheSize) {
            Log.d(TAG, "Cache size ($diskCacheSize) exceeds limit, cleaning up...")
            cacheManager.clearMemoryCache()
        }
    }
    
    /**
     * Handle low memory situations
     */
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Low memory detected, clearing caches")
        
        // Clear memory caches to free up space
        cacheManager.clearMemoryCache()
        
        // Request garbage collection
        System.gc()
    }
    
    /**
     * Handle memory trim requests
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        when (level) {
            TRIM_MEMORY_UI_HIDDEN,
            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_MODERATE -> {
                // App is in background, clear some caches
                cacheManager.clearMemoryCache()
            }
            TRIM_MEMORY_COMPLETE -> {
                // System is very low on memory, clear everything possible
                cacheManager.clearAllCache()
            }
        }
        
        Log.d(TAG, "Memory trim requested at level $level")
    }
    
    /**
     * Get application version information
     */
    fun getVersionInfo(): Pair<String, Int> {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            Pair(packageInfo.versionName ?: "Unknown", packageInfo.versionCode)
        } catch (e: Exception) {
            Pair("Unknown", 0)
        }
    }
    
    /**
     * Check if this is a debug build
     */
    fun isDebugBuild(): Boolean {
        return BuildConfig.DEBUG
    }
}