package me.kelexine.azubimark

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.kelexine.azubimark.databinding.ActivityFileBrowserBinding
import java.io.File
import java.io.FileFilter

class FileBrowser : AppCompatActivity() {
    private lateinit var binding: ActivityFileBrowserBinding
    private lateinit var adapter: FileAdapter
    private lateinit var currentPath: File
    private var mainFileName: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply theme
        ThemeManager(this).applyTheme()
        
        // Initialize binding
        binding = ActivityFileBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Browse Markdown Files"
        
        // Initialize RecyclerView
        binding.recyclerFiles.layoutManager = LinearLayoutManager(this)
        binding.recyclerFiles.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        
        // Set up file adapter
        adapter = FileAdapter()
        binding.recyclerFiles.adapter = adapter
        
        // Get starting directory
        val startDir = intent.getStringExtra("path") ?: Environment.getExternalStorageDirectory().path
        currentPath = File(startDir)
        
        // Display files
        loadFiles()
    }
    
    private fun loadFiles() {
        try {
            val files = currentPath.listFiles(FileFilter { 
                it.isDirectory || 
                it.name.endsWith(".md", ignoreCase = true) || 
                it.name.endsWith(".markdown", ignoreCase = true)
            })
            
            // Sort: directories first, then files alphabetically
            val sortedFiles = files?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            
            if (sortedFiles.isNullOrEmpty()) {
                binding.emptyView.visibility = View.VISIBLE
                binding.recyclerFiles.visibility = View.GONE
            } else {
                binding.emptyView.visibility = View.GONE
                binding.recyclerFiles.visibility = View.VISIBLE
                adapter.setFiles(sortedFiles)
            }
            
            supportActionBar?.subtitle = currentPath.absolutePath
        } catch (e: Exception) {
            Toast.makeText(this, "Error accessing directory: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onBackPressed() {
        // Navigate up one directory or exit if at root
        if (currentPath.path != Environment.getExternalStorageDirectory().path && currentPath.parentFile != null) {
            currentPath = currentPath.parentFile!!
            loadFiles()
        } else {
            super.onBackPressed()
        }
    }
    
    inner class FileAdapter : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {
        private var files: List<File> = emptyList()
        
        fun setFiles(files: List<File>) {
            this.files = files
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.file_browser_item, parent, false)
            return FileViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
            holder.bind(files[position])
        }
        
        override fun getItemCount() = files.size
        
        inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val iconView: ImageView = itemView.findViewById(R.id.file_icon)
            private val nameView: TextView = itemView.findViewById(R.id.file_name)
            
            fun bind(file: File) {
                nameView.text = file.name
                
                // Set appropriate icon and color based on file type
                if (file.isDirectory) {
                    iconView.setImageResource(R.drawable.ic_folder)
                    iconView.setColorFilter(ContextCompat.getColor(this@FileBrowser, R.color.folder_color))
                } else {
                    iconView.setImageResource(R.drawable.ic_markdown)
                    iconView.setColorFilter(ContextCompat.getColor(this@FileBrowser, R.color.markdown_color))
                }
                
                // Set click listener
                itemView.setOnClickListener {
                    if (file.isDirectory) {
                        // Navigate to directory
                        currentPath = file
                        loadFiles()
                    } else {
                        // Open markdown file
                        try {
                            val content = file.readText()
                            val intent = Intent(this@FileBrowser, MainActivity::class.java)
                            intent.putExtra("MARKDOWN_CONTENT", content)
                            intent.putExtra("FILE_NAME", file.name)
                            startActivity(intent)
                            finish()
                        } catch (e: Exception) {
                            Toast.makeText(this@FileBrowser, "Error opening file: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}
