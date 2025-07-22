package me.kelexine.azubimark

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Manages app shortcuts and deep linking for quick access to common features
 */
object ShortcutManager {
    
    private const val MAX_SHORTCUTS = 4 // Android limit for dynamic shortcuts
    
    // Shortcut IDs
    private const val SHORTCUT_BROWSE_FILES = "browse_files"
    private const val SHORTCUT_RECENT_FILES = "recent_files"
    private const val SHORTCUT_SETTINGS = "settings"
    private const val SHORTCUT_ABOUT = "about"
    
    /**
     * Initialize app shortcuts
     */
    fun initializeShortcuts(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = ContextCompat.getSystemService(context, ShortcutManager::class.java)
            
            if (shortcutManager != null) {
                val shortcuts = createDynamicShortcuts(context)
                shortcutManager.dynamicShortcuts = shortcuts
            }
        }
    }
    
    /**
     * Create dynamic shortcuts list
     */
    private fun createDynamicShortcuts(context: Context): List<ShortcutInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return emptyList()
        }
        
        val shortcuts = mutableListOf<ShortcutInfo>()
        
        // Browse Files shortcut
        val browseFilesIntent = Intent(context, EnhancedFileBrowser::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("shortcut_launched", true)
        }
        
        val browseFilesShortcut = ShortcutInfo.Builder(context, SHORTCUT_BROWSE_FILES)
            .setShortLabel("Browse Files")
            .setLongLabel("Browse Markdown Files")
            .setIcon(Icon.createWithResource(context, R.drawable.ic_folder))
            .setIntent(browseFilesIntent)
            .build()
        
        shortcuts.add(browseFilesShortcut)
        
        // Settings shortcut
        val settingsIntent = Intent(context, SettingsActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("shortcut_launched", true)
        }
        
        val settingsShortcut = ShortcutInfo.Builder(context, SHORTCUT_SETTINGS)
            .setShortLabel("Settings")
            .setLongLabel("App Settings")
            .setIcon(Icon.createWithResource(context, R.drawable.ic_settings))
            .setIntent(settingsIntent)
            .build()
        
        shortcuts.add(settingsShortcut)
        
        // About shortcut
        val aboutIntent = Intent(context, AboutActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("shortcut_launched", true)
        }
        
        val aboutShortcut = ShortcutInfo.Builder(context, SHORTCUT_ABOUT)
            .setShortLabel("About")
            .setLongLabel("About AzubiMark")
            .setIcon(Icon.createWithResource(context, R.drawable.ic_info))
            .setIntent(aboutIntent)
            .build()
        
        shortcuts.add(aboutShortcut)
        
        return shortcuts
    }
    
    /**
     * Add recent file shortcut
     */
    fun addRecentFileShortcut(context: Context, filePath: String, fileName: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        
        val shortcutManager = ContextCompat.getSystemService(context, ShortcutManager::class.java)
        if (shortcutManager == null || !shortcutManager.isRequestPinShortcutSupported) return
        
        val openFileIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.fromFile(File(filePath))
            putExtra("file_path", filePath)
            putExtra("file_name", fileName)
            putExtra("shortcut_launched", true)
        }
        
        val shortcutId = "recent_file_${filePath.hashCode()}"
        val shortcut = ShortcutInfo.Builder(context, shortcutId)
            .setShortLabel(fileName.take(10)) // Android limitation
            .setLongLabel("Open $fileName")
            .setIcon(Icon.createWithResource(context, R.drawable.ic_markdown))
            .setIntent(openFileIntent)
            .build()
        
        // Add to dynamic shortcuts
        try {
            val currentShortcuts = shortcutManager.dynamicShortcuts.toMutableList()
            
            // Remove existing shortcut for this file if it exists
            currentShortcuts.removeAll { it.id == shortcutId }
            
            // Add new shortcut at the beginning
            currentShortcuts.add(0, shortcut)
            
            // Keep only the maximum allowed shortcuts
            if (currentShortcuts.size > MAX_SHORTCUTS) {
                currentShortcuts.removeAt(currentShortcuts.size - 1)
            }
            
            shortcutManager.dynamicShortcuts = currentShortcuts
        } catch (e: Exception) {
            // Silently fail if shortcut creation fails
        }
    }
    
    /**
     * Update shortcuts based on recent activity
     */
    fun updateShortcuts(context: Context, recentFiles: List<Pair<String, String>>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        
        val shortcutManager = ContextCompat.getSystemService(context, ShortcutManager::class.java)
        if (shortcutManager == null) return
        
        val shortcuts = mutableListOf<ShortcutInfo>()
        
        // Add recent files shortcuts (max 2)
        recentFiles.take(2).forEachIndexed { index, (filePath, fileName) ->
            val openFileIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.fromFile(File(filePath))
                putExtra("file_path", filePath)
                putExtra("file_name", fileName)
                putExtra("shortcut_launched", true)
            }
            
            val shortcut = ShortcutInfo.Builder(context, "recent_$index")
                .setShortLabel(fileName.take(10))
                .setLongLabel("Open $fileName")
                .setIcon(Icon.createWithResource(context, R.drawable.ic_markdown))
                .setIntent(openFileIntent)
                .setRank(index)
                .build()
            
            shortcuts.add(shortcut)
        }
        
        // Add standard shortcuts
        shortcuts.addAll(createDynamicShortcuts(context).take(MAX_SHORTCUTS - shortcuts.size))
        
        try {
            shortcutManager.dynamicShortcuts = shortcuts
        } catch (e: Exception) {
            // Silently fail if shortcut update fails
        }
    }
    
    /**
     * Handle deep link from shortcut or external intent
     */
    fun handleDeepLink(intent: Intent): DeepLinkAction? {
        return when {
            intent.getBooleanExtra("shortcut_launched", false) -> {
                when {
                    intent.hasExtra("file_path") -> {
                        val filePath = intent.getStringExtra("file_path")
                        val fileName = intent.getStringExtra("file_name") ?: "Document"
                        if (filePath != null) {
                            DeepLinkAction.OpenFile(filePath, fileName)
                        } else null
                    }
                    intent.component?.className?.contains("FileBrowser") == true -> {
                        DeepLinkAction.OpenFileBrowser
                    }
                    intent.component?.className?.contains("Settings") == true -> {
                        DeepLinkAction.OpenSettings
                    }
                    intent.component?.className?.contains("About") == true -> {
                        DeepLinkAction.OpenAbout
                    }
                    else -> null
                }
            }
            intent.action == Intent.ACTION_VIEW && intent.data != null -> {
                val uri = intent.data!!
                when (uri.scheme) {
                    "file" -> {
                        val filePath = uri.path
                        if (filePath != null && isMarkdownFile(filePath)) {
                            val fileName = File(filePath).name
                            DeepLinkAction.OpenFile(filePath, fileName)
                        } else null
                    }
                    "content" -> {
                        // Handle content URI (from file managers, etc.)
                        DeepLinkAction.OpenContentUri(uri)
                    }
                    else -> null
                }
            }
            intent.action == Intent.ACTION_SEND && intent.type == "text/plain" -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (sharedText != null) {
                    DeepLinkAction.OpenSharedText(sharedText)
                } else null
            }
            else -> null
        }
    }
    
    /**
     * Check if file is a markdown file
     */
    private fun isMarkdownFile(filePath: String): Boolean {
        val extension = File(filePath).extension.lowercase()
        return extension in setOf("md", "markdown", "mdown", "mkd", "mdx")
    }
    
    /**
     * Report shortcut usage to Android for ranking
     */
    fun reportShortcutUsed(context: Context, shortcutId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = ContextCompat.getSystemService(context, ShortcutManager::class.java)
            shortcutManager?.reportShortcutUsed(shortcutId)
        }
    }
    
    /**
     * Clear all dynamic shortcuts
     */
    fun clearShortcuts(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = ContextCompat.getSystemService(context, ShortcutManager::class.java)
            shortcutManager?.removeAllDynamicShortcuts()
        }
    }
    
    /**
     * Check if shortcuts are supported
     */
    fun areShortcutsSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
    }
    
    /**
     * Deep link actions
     */
    sealed class DeepLinkAction {
        object OpenFileBrowser : DeepLinkAction()
        object OpenSettings : DeepLinkAction()
        object OpenAbout : DeepLinkAction()
        data class OpenFile(val filePath: String, val fileName: String) : DeepLinkAction()
        data class OpenContentUri(val uri: Uri) : DeepLinkAction()
        data class OpenSharedText(val text: String) : DeepLinkAction()
    }
}