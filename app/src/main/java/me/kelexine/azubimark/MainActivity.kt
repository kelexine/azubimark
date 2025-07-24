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
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import androidx.appcompat.widget.SearchView
import me.kelexine.azubimark.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var markdownViewer: MarkdownViewer
    private lateinit var themeManager: EnhancedThemeManager
    private lateinit var documentOutline: DocumentOutline
    private lateinit var searchManager: SearchManager
    private lateinit var drawerToggle: ActionBarDrawerToggle
    
    // Add this to store current markdown content
    private var currentMarkdownContent: String? = null
    private var currentFileName: String? = null
    
    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val content = inputStream.bufferedReader().use { it.readText() }
                    loadMarkdownContent(content, uri.lastPathSegment ?: "Document")
                }
            } catch (e: IOException) {
                Toast.makeText(this, "Failed to open file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize theme manager
        themeManager = EnhancedThemeManager(this)
        themeManager.applyTheme()
        
        // Set up edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        setupEdgeToEdgeDisplay()
        
        // Initialize components
        setupComponents()
        setupNavigationDrawer()
        setupNavigationDrawerActions()
        setupScrollHandling()
        setupFABs()
        
        // Restore saved content if available
        if (savedInstanceState != null && savedInstanceState.containsKey("SAVED_MARKDOWN_CONTENT")) {
            val savedContent = savedInstanceState.getString("SAVED_MARKDOWN_CONTENT")
            val savedFileName = savedInstanceState.getString("SAVED_FILE_NAME") ?: "Document"
            if (savedContent != null) {
                loadMarkdownContent(savedContent, savedFileName)
            }
        } else {
            // Handle intent if app was opened with a file
            handleIntent(intent)
            
            // Check for extras from FileBrowser
            checkForMarkdownContent()
        }
    }
    
    private fun setupComponents() {
        // Initialize markdown viewer
        markdownViewer = MarkdownViewer(this, binding.markdownContent)
        
        // Initialize document outline
        documentOutline = DocumentOutline(this, binding.outlineRecyclerView) { heading ->
            scrollToHeading(heading)
            binding.drawerLayout.closeDrawers()
        }
        
        // Initialize search manager
        searchManager = SearchManager(this, binding.markdownContent) { query ->
            // Handle search results
            updateSearchResults(query)
        }
        
        // Optimize TextView for better performance
        binding.markdownContent.apply {
            setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
            isHorizontalScrollBarEnabled = false
            isVerticalScrollBarEnabled = true
        }
    }
    
    private fun setupNavigationDrawer() {
        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.toggle_outline,
            R.string.toggle_outline
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        
        // Handle navigation view interactions
        binding.navOutline.setNavigationItemSelectedListener { false }
    }
    
    private fun setupScrollHandling() {
        binding.markdownScrollView.setOnScrollChangeListener { v, _, scrollY, _, _ ->
            val scrollView = v as NestedScrollView
            val contentHeight = scrollView.getChildAt(0).height
            val viewHeight = scrollView.height
            val maxScroll = contentHeight - viewHeight
            
            if (maxScroll > 0) {
                val progress = (scrollY.toFloat() / maxScroll * 100).toInt()
                binding.scrollProgress.progress = progress
                
                // Show/hide back to top FAB based on scroll position
                if (scrollY > 300) {
                    binding.fabBackToTop.show()
                } else {
                    binding.fabBackToTop.hide()
                }
                
                // Update outline highlighting based on scroll position
                documentOutline.updateActiveSection(scrollY)
            } else {
                binding.scrollProgress.progress = 0
                binding.fabBackToTop.hide()
            }
        }
    }
    
    private fun setupFABs() {
        // Browse files FAB
        binding.fabBrowse.setOnClickListener {
            openFileBrowser()
        }
        
        // Back to top FAB
        binding.fabBackToTop.setOnClickListener {
            binding.markdownScrollView.smoothScrollTo(0, 0)
        }
    }
    
    private fun setupNavigationDrawerActions() {
        // Set up navigation drawer toggle
        binding.toolbar.setNavigationOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(binding.navOutline)) {
                binding.drawerLayout.closeDrawer(binding.navOutline)
            } else {
                binding.drawerLayout.openDrawer(binding.navOutline)
            }
        }
        
        // Setup outline action buttons (these will be handled in the outline implementation)
        setupOutlineActions()
    }
    
    private fun setupOutlineActions() {
        // These buttons are in the new navigation drawer layout
        // Implementation will be added when RecyclerView adapter is created
    }
    
    private fun toggleReadingMode() {
        // Toggle reading mode implementation
        Toast.makeText(this, "Reading mode toggle - to be implemented", Toast.LENGTH_SHORT).show()
    }
    
    private fun exportDocument() {
        // Export document implementation
        Toast.makeText(this, "Export feature - to be implemented", Toast.LENGTH_SHORT).show()
    }
    
    private fun showTextSizeDialog() {
        // Text size dialog implementation
        Toast.makeText(this, "Text size settings - to be implemented", Toast.LENGTH_SHORT).show()
    }
    
    private fun loadMarkdownContent(content: String, fileName: String) {
        currentMarkdownContent = content
        currentFileName = fileName
        
        // Set the content to markdown viewer
        markdownViewer.setMarkdownContent(content)
        
        // Update toolbar subtitle
        supportActionBar?.subtitle = fileName
        
        // Generate document outline
        documentOutline.generateOutline(content)
        
        // Reset scroll position and progress
        binding.markdownScrollView.scrollTo(0, 0)
        binding.scrollProgress.progress = 0
        binding.fabBackToTop.hide()
        
        // Update search manager with new content
        searchManager.updateContent(content)
    }
    
    private fun scrollToHeading(heading: DocumentOutline.HeadingItem) {
        // This is a simplified version - in a real implementation,
        // you'd need to map headings to their positions in the TextView
        val searchText = heading.text.trim()
        val content = binding.markdownContent.text.toString()
        val index = content.indexOf(searchText)
        
        if (index >= 0) {
            // Calculate approximate scroll position
            val lines = content.substring(0, index).count { it == '\n' }
            val lineHeight = binding.markdownContent.lineHeight
            val scrollY = lines * lineHeight
            
            binding.markdownScrollView.smoothScrollTo(0, scrollY)
        }
    }
    
    private fun updateSearchResults(query: String) {
        if (query.isEmpty()) return
        
        val content = binding.markdownContent.text.toString()
        val index = content.indexOf(query, ignoreCase = true)
        
        if (index >= 0) {
            // Calculate scroll position for search result
            val lines = content.substring(0, index).count { it == '\n' }
            val lineHeight = binding.markdownContent.lineHeight
            val scrollY = lines * lineHeight
            
            binding.markdownScrollView.smoothScrollTo(0, scrollY)
            
            // Highlight the found text (this would need more sophisticated implementation)
            Toast.makeText(this, "Found: $query", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.no_search_results), Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the current markdown content
        currentMarkdownContent?.let {
            outState.putString("SAVED_MARKDOWN_CONTENT", it)
        }
        currentFileName?.let {
            outState.putString("SAVED_FILE_NAME", it)
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the stored intent
        handleIntent(intent)
        checkForMarkdownContent()
    }
    
    override fun onDestroy() {
        // Clear any cached content to prevent memory leaks
        currentMarkdownContent = null
        currentFileName = null
        super.onDestroy()
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        // Clear non-essential caches when memory is low
        System.gc()
    }
    
    private fun checkForMarkdownContent() {
        try {
            val markdownContent = intent.getStringExtra("MARKDOWN_CONTENT")
            val fileName = intent.getStringExtra("FILE_NAME") ?: "Document"
            
            if (markdownContent != null) {
                loadMarkdownContent(markdownContent, fileName)
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
                                val fileName = getFileName(uri) ?: "Document"
                                loadMarkdownContent(content, fileName)
                            }
                        } catch (e: IOException) {
                            showErrorDialog("Failed to open file", e.message ?: "Unknown error")
                        } catch (e: SecurityException) {
                            showErrorDialog("Permission denied", "Cannot access the selected file")
                        }
                    } ?: run {
                        Toast.makeText(this, "No file data received", Toast.LENGTH_SHORT).show()
                    }
                }
                Intent.ACTION_SEND -> {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                        loadMarkdownContent(text, "Shared Text")
                    }
                }
            }
        } catch (e: Exception) {
            showErrorDialog("Error opening file", e.message ?: "An unexpected error occurred")
        }
    }
    
    private fun getFileName(uri: android.net.Uri): String? {
        return try {
            when (uri.scheme) {
                "content" -> {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) cursor.getString(nameIndex) else null
                        } else null
                    }
                }
                "file" -> uri.lastPathSegment
                else -> uri.lastPathSegment
            }
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }
    
    private fun showErrorDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    
    private fun openFileBrowser() {
        try {
            val intent = Intent(this, EnhancedFileBrowser::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening file browser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        
        // Set up the SearchView
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        
        searchView?.apply {
            queryHint = getString(R.string.search_document_hint)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let { 
                        searchManager.performSearch(it)
                        clearFocus()
                    }
                    return true
                }
                
                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText.isNullOrEmpty()) {
                        searchManager.clearSearch()
                    } else if (newText.length > 2) {
                        searchManager.performSearch(newText)
                    }
                    return true
                }
            })
            
            setOnCloseListener {
                searchManager.clearSearch()
                false
            }
            
            // Configure search view appearance
            maxWidth = Integer.MAX_VALUE
<<<<<<< HEAD
            isIconifiedByDefault = true
=======
>>>>>>> cursor/fix-critical-bugs-and-improve-app-compliance-89ec
        }
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (binding.drawerLayout.isDrawerOpen(binding.navOutline)) {
                    binding.drawerLayout.closeDrawer(binding.navOutline)
                } else {
                    binding.drawerLayout.openDrawer(binding.navOutline)
                }
                true
            }
            R.id.action_outline -> {
                if (binding.drawerLayout.isDrawerOpen(binding.navOutline)) {
                    binding.drawerLayout.closeDrawer(binding.navOutline)
                } else {
                    binding.drawerLayout.openDrawer(binding.navOutline)
                }
                true
            }
            R.id.action_open -> {
                openDocumentLauncher.launch(arrayOf("text/markdown", "text/plain"))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_theme -> {
                showThemeChooser()
                true
            }
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            R.id.action_reading_mode -> {
                toggleReadingMode()
                true
            }
            R.id.action_export -> {
                exportDocument()
                true
            }
            R.id.action_text_size -> {
                showTextSizeDialog()
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
    
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }
    
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }
}
