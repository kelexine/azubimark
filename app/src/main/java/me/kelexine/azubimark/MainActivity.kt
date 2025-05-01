package me.kelexine.azubimark

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import me.kelexine.azubimark.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var markdownViewer: MarkdownViewer
    private lateinit var themeManager: ThemeManager
    
    // Add this to store current markdown content
    private var currentMarkdownContent: String? = null
    
    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val content = inputStream.bufferedReader().use { it.readText() }
                    markdownViewer.setMarkdownContent(content)
                    currentMarkdownContent = content // Store the content
                }
            } catch (e: IOException) {
                Toast.makeText(this, "Failed to open file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize theme manager
        themeManager = ThemeManager(this)
        themeManager.applyTheme()
        
        // Set up edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        
        // Initialize markdown viewer
        markdownViewer = MarkdownViewer(this, binding.markdownContent)
        
        // Set up FAB for file browser
        findViewById<FloatingActionButton>(R.id.fab_browse)?.setOnClickListener {
            openFileBrowser()
        }
        
        // Restore saved content if available
        if (savedInstanceState != null && savedInstanceState.containsKey("SAVED_MARKDOWN_CONTENT")) {
            val savedContent = savedInstanceState.getString("SAVED_MARKDOWN_CONTENT")
            if (savedContent != null) {
                markdownViewer.setMarkdownContent(savedContent)
                currentMarkdownContent = savedContent
            }
        } else {
            // Handle intent if app was opened with a file
            handleIntent(intent)
            
            // Check for extras from FileBrowser
            checkForMarkdownContent()
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the current markdown content
        currentMarkdownContent?.let {
            outState.putString("SAVED_MARKDOWN_CONTENT", it)
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the stored intent
        handleIntent(intent)
        checkForMarkdownContent()
    }
    
    private fun checkForMarkdownContent() {
        try {
            val markdownContent = intent.getStringExtra("MARKDOWN_CONTENT")
            val fileName = intent.getStringExtra("FILE_NAME")
            
            if (markdownContent != null) {
                markdownViewer.setMarkdownContent(markdownContent)
                currentMarkdownContent = markdownContent // Store the content
                supportActionBar?.subtitle = fileName
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading markdown: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupEdgeToEdgeDisplay() {
    // Enable edge-to-edge display
    WindowCompat.setDecorFitsSystemWindows(window, false)
    
    // Handle insets for toolbar
    ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        
        // Apply padding to the toolbar to avoid overlap with status bar
        view.setPadding(
            view.paddingLeft,
            insets.top, // This ensures content starts below status bar
            view.paddingRight,
            view.paddingBottom
        )
        
        WindowInsetsCompat.CONSUMED
    }
    
    // Fix the status bar icon colors based on background
    WindowInsetsControllerCompat(window, window.decorView).apply {
        isAppearanceLightStatusBars = ThemeUtils.shouldUseLightStatusBar(this@MainActivity)
    }
}
    
    private fun handleIntent(intent: Intent) {
        try {
            when (intent.action) {
                Intent.ACTION_VIEW -> {
                    intent.data?.let { uri ->
                        try {
                            contentResolver.openInputStream(uri)?.use { inputStream ->
                                val content = inputStream.bufferedReader().use { it.readText() }
                                markdownViewer.setMarkdownContent(content)
                                currentMarkdownContent = content // Store the content
                            }
                        } catch (e: IOException) {
                            Toast.makeText(this, "Failed to open file: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error handling intent: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openFileBrowser() {
        try {
            val intent = Intent(this, FileBrowser::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening file browser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_theme -> {
                showThemeChooser()
                true
            }
            R.id.action_open -> {
                openDocumentLauncher.launch(arrayOf("text/markdown", "text/plain"))
                true
            }
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showThemeChooser() {
        val themes = arrayOf("System", "Light", "Dark", "Material You (Dynamic)")
        MaterialAlertDialogBuilder(this)
            .setTitle("Choose Theme")
            .setSingleChoiceItems(themes, themeManager.getCurrentTheme()) { dialog, which ->
                themeManager.setTheme(which)
                dialog.dismiss()
                recreate()
            }
            .show()
    }
}
