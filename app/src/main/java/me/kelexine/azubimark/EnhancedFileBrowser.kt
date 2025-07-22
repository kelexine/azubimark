package me.kelexine.azubimark

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.text.format.DateFormat
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import me.kelexine.azubimark.databinding.ActivityEnhancedFileBrowserBinding
import java.io.File
import java.io.FileFilter
import java.util.*

class EnhancedFileBrowser : AppCompatActivity() {
    private lateinit var binding: ActivityEnhancedFileBrowserBinding
    private lateinit var adapter: EnhancedFileAdapter
    private lateinit var currentPath: File
    private var isGridView = false
    private var sortOrder = "name_asc"
    private var showHiddenFiles = false
    private var sortFoldersFirst = true
    
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadFiles()
        } else {
            showPermissionDeniedDialog()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply theme
        ThemeManager(this).applyTheme()
        
        // Initialize binding
        binding = ActivityEnhancedFileBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.browse_files)
        
        // Load preferences
        loadPreferences()
        
        // Initialize RecyclerView
        setupRecyclerView()
        
        // Set up file adapter
        adapter = EnhancedFileAdapter()
        binding.recyclerFiles.adapter = adapter
        
        // Get starting directory
        val startDir = getStartDirectory()
        currentPath = File(startDir)
        
        // Check permissions and load files
        checkPermissionsAndLoadFiles()
        
        // Set up FAB
        binding.fabCreateFolder.setOnClickListener {
            showCreateFolderDialog()
        }
    }
    
    private fun loadPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        showHiddenFiles = prefs.getBoolean("show_hidden_files", false)
        sortFoldersFirst = prefs.getBoolean("sort_folders_first", true)
        sortOrder = prefs.getString("file_sort_order", "name_asc") ?: "name_asc"
    }
    
    private fun getStartDirectory(): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val startLocation = prefs.getString("start_location", "downloads") ?: "downloads"
        
        return when (startLocation) {
            "downloads" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
            "documents" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path
            "internal" -> Environment.getExternalStorageDirectory().path
            "sdcard" -> Environment.getExternalStorageDirectory().path
            "last_opened" -> prefs.getString("last_opened_path", Environment.getExternalStorageDirectory().path) ?: Environment.getExternalStorageDirectory().path
            else -> Environment.getExternalStorageDirectory().path
        }
    }
    
    private fun setupRecyclerView() {
        if (isGridView) {
            binding.recyclerFiles.layoutManager = GridLayoutManager(this, 2)
        } else {
            binding.recyclerFiles.layoutManager = LinearLayoutManager(this)
            binding.recyclerFiles.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        }
    }
    
    private fun checkPermissionsAndLoadFiles() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
            == PackageManager.PERMISSION_GRANTED) {
            loadFiles()
        } else {
            requestStoragePermission()
        }
    }
    
    private fun requestStoragePermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.storage_permission_required)
                .setMessage(R.string.storage_permission_required)
                .setPositiveButton(R.string.grant_permission) { _, _ ->
                    storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    finish()
                }
                .show()
        } else {
            storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    
    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_denied)
            .setMessage(R.string.storage_permission_required)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                finish()
            }
            .show()
    }
    
    private fun loadFiles() {
        try {
            val fileFilter = FileFilter { file ->
                when {
                    !showHiddenFiles && file.name.startsWith(".") -> false
                    file.isDirectory -> true
                    file.name.endsWith(".md", ignoreCase = true) -> true
                    file.name.endsWith(".markdown", ignoreCase = true) -> true
                    file.name.endsWith(".txt", ignoreCase = true) -> true
                    else -> false
                }
            }
            
            val files = currentPath.listFiles(fileFilter)?.toMutableList() ?: mutableListOf()
            
            // Add parent directory option if not at root
            if (currentPath.parentFile != null) {
                files.add(0, File(currentPath, ".."))
            }
            
            // Sort files
            val sortedFiles = sortFiles(files)
            
            if (sortedFiles.isEmpty() || (sortedFiles.size == 1 && sortedFiles[0].name == "..")) {
                binding.emptyView.visibility = View.VISIBLE
                binding.recyclerFiles.visibility = View.GONE
            } else {
                binding.emptyView.visibility = View.GONE
                binding.recyclerFiles.visibility = View.VISIBLE
                adapter.setFiles(sortedFiles)
            }
            
            // Update toolbar subtitle and save last opened path
            supportActionBar?.subtitle = currentPath.absolutePath
            saveLastOpenedPath(currentPath.absolutePath)
            
        } catch (e: Exception) {
            showError("Error accessing directory: ${e.message}")
        }
    }
    
    private fun sortFiles(files: List<File>): List<File> {
        return files.sortedWith { file1, file2 ->
            // Handle parent directory
            if (file1.name == "..") return@sortedWith -1
            if (file2.name == "..") return@sortedWith 1
            
            // Sort folders first if enabled
            if (sortFoldersFirst) {
                if (file1.isDirectory && !file2.isDirectory) return@sortedWith -1
                if (!file1.isDirectory && file2.isDirectory) return@sortedWith 1
            }
            
            // Apply sort order
            when (sortOrder) {
                "name_asc" -> file1.name.compareTo(file2.name, ignoreCase = true)
                "name_desc" -> file2.name.compareTo(file1.name, ignoreCase = true)
                "date_desc" -> file2.lastModified().compareTo(file1.lastModified())
                "date_asc" -> file1.lastModified().compareTo(file2.lastModified())
                "size_desc" -> file2.length().compareTo(file1.length())
                "size_asc" -> file1.length().compareTo(file2.length())
                "type" -> {
                    val ext1 = file1.extension.lowercase()
                    val ext2 = file2.extension.lowercase()
                    ext1.compareTo(ext2)
                }
                else -> file1.name.compareTo(file2.name, ignoreCase = true)
            }
        }
    }
    
    private fun saveLastOpenedPath(path: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().putString("last_opened_path", path).apply()
    }
    
    private fun showCreateFolderDialog() {
        val editText = TextInputEditText(this)
        editText.hint = getString(R.string.folder_name)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.create_folder)
            .setView(editText)
            .setPositiveButton(R.string.create) { _, _ ->
                val folderName = editText.text?.toString()?.trim()
                if (!folderName.isNullOrEmpty()) {
                    createFolder(folderName)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun createFolder(name: String) {
        try {
            val newFolder = File(currentPath, name)
            if (newFolder.exists()) {
                showError("Folder already exists")
                return
            }
            
            if (newFolder.mkdirs()) {
                loadFiles()
                Snackbar.make(binding.root, "Folder created", Snackbar.LENGTH_SHORT).show()
            } else {
                showError("Failed to create folder")
            }
        } catch (e: Exception) {
            showError("Error creating folder: ${e.message}")
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.file_browser_menu, menu)
        
        // Update view type icon
        val viewTypeItem = menu.findItem(R.id.action_view_type)
        viewTypeItem?.setIcon(if (isGridView) R.drawable.ic_list_view else R.drawable.ic_grid_view)
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_refresh -> {
                loadFiles()
                true
            }
            R.id.action_sort -> {
                showSortDialog()
                true
            }
            R.id.action_view_type -> {
                toggleViewType()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showSortDialog() {
        val sortOptions = resources.getStringArray(R.array.file_sort_order_entries)
        val sortValues = resources.getStringArray(R.array.file_sort_order_values)
        val currentIndex = sortValues.indexOf(sortOrder)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sort_by)
            .setSingleChoiceItems(sortOptions, currentIndex) { dialog, which ->
                sortOrder = sortValues[which]
                loadFiles()
                dialog.dismiss()
            }
            .show()
    }
    
    private fun toggleViewType() {
        isGridView = !isGridView
        setupRecyclerView()
        binding.recyclerFiles.adapter = adapter
        invalidateOptionsMenu()
    }
    
    override fun onBackPressed() {
        if (currentPath.parentFile != null && currentPath.path != Environment.getExternalStorageDirectory().path) {
            currentPath = currentPath.parentFile!!
            loadFiles()
        } else {
            super.onBackPressed()
        }
    }
    
    inner class EnhancedFileAdapter : RecyclerView.Adapter<EnhancedFileAdapter.FileViewHolder>() {
        private var files: List<File> = emptyList()
        
        fun setFiles(files: List<File>) {
            this.files = files
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
            val layoutId = if (isGridView) R.layout.file_browser_grid_item else R.layout.file_browser_enhanced_item
            val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
            return FileViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
            holder.bind(files[position])
        }
        
        override fun getItemCount() = files.size
        
        inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val iconView: ImageView = itemView.findViewById(R.id.file_icon)
            private val nameView: TextView = itemView.findViewById(R.id.file_name)
            private val detailsView: TextView? = itemView.findViewById(R.id.file_details)
            
            fun bind(file: File) {
                when {
                    file.name == ".." -> {
                        nameView.text = getString(R.string.parent_directory)
                        iconView.setImageResource(R.drawable.ic_arrow_upward)
                        iconView.setColorFilter(ContextCompat.getColor(this@EnhancedFileBrowser, R.color.folder_color))
                        detailsView?.text = ""
                    }
                    file.isDirectory -> {
                        nameView.text = file.name
                        iconView.setImageResource(R.drawable.ic_folder)
                        iconView.setColorFilter(ContextCompat.getColor(this@EnhancedFileBrowser, R.color.folder_color))
                        detailsView?.text = getFileDetails(file)
                    }
                    else -> {
                        nameView.text = file.name
                        iconView.setImageResource(R.drawable.ic_markdown)
                        iconView.setColorFilter(ContextCompat.getColor(this@EnhancedFileBrowser, R.color.markdown_color))
                        detailsView?.text = getFileDetails(file)
                    }
                }
                
                // Set click listener
                itemView.setOnClickListener {
                    when {
                        file.name == ".." -> {
                            currentPath = currentPath.parentFile!!
                            loadFiles()
                        }
                        file.isDirectory -> {
                            currentPath = file
                            loadFiles()
                        }
                        else -> {
                            openMarkdownFile(file)
                        }
                    }
                }
                
                // Set long click listener for file operations
                if (file.name != "..") {
                    itemView.setOnLongClickListener {
                        showFileOptionsDialog(file)
                        true
                    }
                }
            }
            
            private fun getFileDetails(file: File): String {
                val size = if (file.isFile) Formatter.formatFileSize(this@EnhancedFileBrowser, file.length()) else ""
                val date = DateFormat.getDateFormat(this@EnhancedFileBrowser).format(Date(file.lastModified()))
                return if (size.isNotEmpty()) "$size • $date" else date
            }
        }
    }
    
    private fun openMarkdownFile(file: File) {
        try {
            val content = file.readText()
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("MARKDOWN_CONTENT", content)
            intent.putExtra("FILE_NAME", file.name)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            showError("Error opening file: ${e.message}")
        }
    }
    
    private fun showFileOptionsDialog(file: File) {
        val options = arrayOf(
            getString(R.string.rename),
            getString(R.string.delete),
            getString(R.string.share),
            getString(R.string.properties)
        )
        
        MaterialAlertDialogBuilder(this)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(file)
                    1 -> showDeleteConfirmation(file)
                    2 -> shareFile(file)
                    3 -> showFileProperties(file)
                }
            }
            .show()
    }
    
    private fun showRenameDialog(file: File) {
        val editText = TextInputEditText(this)
        editText.setText(file.nameWithoutExtension)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.rename)
            .setView(editText)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newName = editText.text?.toString()?.trim()
                if (!newName.isNullOrEmpty()) {
                    renameFile(file, newName + if (file.isFile) ".${file.extension}" else "")
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun renameFile(file: File, newName: String) {
        try {
            val newFile = File(file.parent, newName)
            if (file.renameTo(newFile)) {
                loadFiles()
                Snackbar.make(binding.root, "Renamed successfully", Snackbar.LENGTH_SHORT).show()
            } else {
                showError("Failed to rename")
            }
        } catch (e: Exception) {
            showError("Error renaming: ${e.message}")
        }
    }
    
    private fun showDeleteConfirmation(file: File) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.confirm_delete, file.name))
            .setMessage(R.string.delete_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteFile(file)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun deleteFile(file: File) {
        try {
            if (file.deleteRecursively()) {
                loadFiles()
                val message = if (file.isDirectory) getString(R.string.folder_deleted) else getString(R.string.file_deleted)
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            } else {
                showError("Failed to delete")
            }
        } catch (e: Exception) {
            showError("Error deleting: ${e.message}")
        }
    }
    
    private fun shareFile(file: File) {
        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, file.readText())
            intent.putExtra(Intent.EXTRA_SUBJECT, file.name)
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (e: Exception) {
            showError("Error sharing file: ${e.message}")
        }
    }
    
    private fun showFileProperties(file: File) {
        val size = if (file.isFile) getString(R.string.file_size, Formatter.formatFileSize(this, file.length())) else "Directory"
        val modified = getString(R.string.file_modified, DateFormat.getDateTimeInstance().format(Date(file.lastModified())))
        val path = "Path: ${file.absolutePath}"
        
        val details = "$size\n$modified\n$path"
        
        MaterialAlertDialogBuilder(this)
            .setTitle(file.name)
            .setMessage(details)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}