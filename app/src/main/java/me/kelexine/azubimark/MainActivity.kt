package me.kelexine.azubimark

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
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
    
    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val content = inputStream.bufferedReader().use { it.readText() }
                    markdownViewer.setMarkdownContent(content)
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
        findViewById<FloatingActionButton>(R.id.fab_browse).setOnClickListener {
            openFileBrowser()
        }
        
        // Handle intent if app was opened with a file
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    try {
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            val content = inputStream.bufferedReader().use { it.readText() }
                            markdownViewer.setMarkdownContent(content)
                        }
                    } catch (e: IOException) {
                        Toast.makeText(this, "Failed to open file: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    private fun openFileBrowser() {
        val intent = Intent(this, FileBrowser::class.java)
        startActivity(intent)
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
        val themes = arrayOf("Light", "Dark", "Material You (Dynamic)")
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
