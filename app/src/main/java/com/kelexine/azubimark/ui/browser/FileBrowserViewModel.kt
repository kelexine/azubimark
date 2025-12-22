package com.kelexine.azubimark.ui.browser

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelexine.azubimark.data.model.FileItem
import com.kelexine.azubimark.domain.file.FileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the file browser screen.
 * 
 * Manages directory navigation state, file listing, and breadcrumb navigation.
 * 
 * Validates: Requirements 2.1, 2.5
 */
class FileBrowserViewModel(
    private val fileManager: FileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileBrowserUiState())
    val uiState: StateFlow<FileBrowserUiState> = _uiState.asStateFlow()

    private val _navigationStack = mutableListOf<NavigationEntry>()

    init {
        loadDirectory(null)
    }

    /**
     * Load the contents of a directory.
     * 
     * @param uri The URI of the directory to load, or null for the root directory
     */
    fun loadDirectory(uri: Uri?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val files = fileManager.browseDirectory(uri)
                val directoryName = uri?.lastPathSegment ?: "Storage"
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    files = files,
                    currentDirectory = uri,
                    currentDirectoryName = directoryName,
                    canNavigateBack = _navigationStack.isNotEmpty(),
                    breadcrumbs = buildBreadcrumbs()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load directory"
                )
            }
        }
    }

    /**
     * Navigate into a directory.
     * 
     * @param fileItem The directory to navigate into
     */
    fun navigateToDirectory(fileItem: FileItem) {
        if (!fileItem.isDirectory) return
        
        // Save current state to navigation stack
        _navigationStack.add(
            NavigationEntry(
                uri = _uiState.value.currentDirectory,
                name = _uiState.value.currentDirectoryName
            )
        )
        
        loadDirectory(fileItem.uri)
    }

    /**
     * Navigate back to the previous directory.
     * 
     * @return true if navigation occurred, false if already at root
     */
    fun navigateBack(): Boolean {
        if (_navigationStack.isEmpty()) return false
        
        val previousEntry = _navigationStack.removeAt(_navigationStack.lastIndex)
        loadDirectory(previousEntry.uri)
        return true
    }

    /**
     * Navigate to a specific breadcrumb position.
     * 
     * @param index The index of the breadcrumb to navigate to
     */
    fun navigateToBreadcrumb(index: Int) {
        if (index < 0 || index >= _navigationStack.size) return
        
        // Remove all entries after the target index
        while (_navigationStack.size > index) {
            _navigationStack.removeAt(_navigationStack.lastIndex)
        }
        
        val targetEntry = if (index == 0) {
            NavigationEntry(null, "Storage")
        } else {
            _navigationStack[index - 1]
        }
        
        loadDirectory(targetEntry.uri)
    }

    /**
     * Refresh the current directory.
     */
    fun refresh() {
        loadDirectory(_uiState.value.currentDirectory)
    }

    /**
     * Clear any error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Build the breadcrumb list from the navigation stack.
     */
    private fun buildBreadcrumbs(): List<BreadcrumbItem> {
        val breadcrumbs = mutableListOf<BreadcrumbItem>()
        
        // Add root
        breadcrumbs.add(BreadcrumbItem("Storage", null, 0))
        
        // Add navigation stack entries
        _navigationStack.forEachIndexed { index, entry ->
            breadcrumbs.add(BreadcrumbItem(entry.name, entry.uri, index + 1))
        }
        
        // Add current directory
        _uiState.value.currentDirectory?.let { uri ->
            breadcrumbs.add(
                BreadcrumbItem(
                    _uiState.value.currentDirectoryName,
                    uri,
                    breadcrumbs.size
                )
            )
        }
        
        return breadcrumbs
    }
}

/**
 * UI state for the file browser screen.
 */
data class FileBrowserUiState(
    val isLoading: Boolean = false,
    val files: List<FileItem> = emptyList(),
    val currentDirectory: Uri? = null,
    val currentDirectoryName: String = "Storage",
    val canNavigateBack: Boolean = false,
    val breadcrumbs: List<BreadcrumbItem> = listOf(BreadcrumbItem("Storage", null, 0)),
    val error: String? = null
)

/**
 * Represents an entry in the navigation stack.
 */
data class NavigationEntry(
    val uri: Uri?,
    val name: String
)

/**
 * Represents a breadcrumb item for navigation.
 */
data class BreadcrumbItem(
    val name: String,
    val uri: Uri?,
    val index: Int
)
