package com.kelexine.azubimark.ui.viewer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelexine.azubimark.data.model.MarkdownFile
import com.kelexine.azubimark.domain.file.FileManager
import com.kelexine.azubimark.domain.markdown.InteractiveTaskListHandler
import com.kelexine.azubimark.domain.markdown.MarkdownRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Markdown viewer screen.
 * 
 * Manages file content loading, task state, and scroll position preservation.
 * Supports switching between files while preserving state.
 * 
 * Validates: Requirements 4.4, 6.4
 */
class MarkdownViewerViewModel(
    private val fileManager: FileManager,
    private val markdownRenderer: MarkdownRenderer
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarkdownViewerUiState())
    val uiState: StateFlow<MarkdownViewerUiState> = _uiState.asStateFlow()

    /**
     * Cache of file states for preserving scroll position and task states
     * when switching between files.
     * Key: File URI string
     * Value: FileState containing scroll position and task states
     */
    private val fileStateCache = mutableMapOf<String, FileState>()

    /**
     * Current task list handler for the active file.
     */
    private var currentTaskHandler: InteractiveTaskListHandler? = null

    // Track the currently requested file to support retries when load fails
    private var activeUri: Uri? = null
    private var activeFileName: String? = null

    /**
     * Load a Markdown file from the given URI.
     * 
     * @param uri The URI of the file to load
     * @param fileName Optional display name for the file
     */
    fun loadFile(uri: Uri, fileName: String? = null) {
        // Update active file tracking
        activeUri = uri
        activeFileName = fileName

        // Save current file state before loading new file
        saveCurrentFileState()

        viewModelScope.launch {
            android.util.Log.d("AzubiMarkDebug", "ViewModel loading file: $uri")
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val content = fileManager.readFile(uri)
                val displayName = fileName ?: uri.lastPathSegment ?: "Markdown File"

                processAndDisplayContent(uri, content, displayName)
            } catch (e: SecurityException) {
                android.util.Log.e("AzubiMarkDebug", "SecurityException loading file", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = FileError.PermissionDenied(e.message ?: "Permission denied")
                )
            } catch (e: java.io.FileNotFoundException) {
                android.util.Log.e("AzubiMarkDebug", "FileNotFoundException loading file", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = FileError.FileNotFound(e.message ?: "File not found")
                )
            } catch (e: java.io.IOException) {
                android.util.Log.e("AzubiMarkDebug", "IOException loading file", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = FileError.EncodingError(e.message ?: "Error reading file")
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = FileError.ParseError(e.message ?: "Failed to parse file")
                )
            }
        }
    }

    /**
     * Load a Markdown file with pre-loaded content.
     * Used when content was read immediately from an external intent.
     * 
     * @param uri The URI of the file
     * @param content The pre-loaded file content
     * @param fileName Optional display name for the file
     */
    fun loadFileWithContent(uri: Uri, content: String, fileName: String? = null) {
        // Update active file tracking
        activeUri = uri
        activeFileName = fileName

        // Save current file state before loading new file
        saveCurrentFileState()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val displayName = fileName ?: uri.lastPathSegment ?: "Markdown File"
                processAndDisplayContent(uri, content, displayName)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = FileError.ParseError(e.message ?: "Failed to parse file")
                )
            }
        }
    }

    /**
     * Process content and update UI state.
     */
    private fun processAndDisplayContent(uri: Uri, content: String, displayName: String) {
        // Initialize task handler
        val taskHandler = InteractiveTaskListHandler()
        taskHandler.processTaskList(content)
        currentTaskHandler = taskHandler

        // Restore previous state if available
        val cachedState = fileStateCache[uri.toString()]
        val taskStates = cachedState?.taskStates ?: taskHandler.getTaskStates()
        
        // Apply cached task states if available
        cachedState?.taskStates?.forEach { (position, isChecked) ->
            taskHandler.toggleTask(position, isChecked)
        }

        val markdownFile = MarkdownFile(
            uri = uri,
            name = displayName,
            content = taskHandler.getRenderedMarkdown(),
            lastModified = System.currentTimeMillis(),
            taskStates = taskStates
        )

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            currentFile = markdownFile,
            scrollPosition = cachedState?.scrollPosition ?: 0,
            error = null
        )
    }


    /**
     * Toggle a task's checked state.
     * 
     * @param position The position of the task to toggle
     * @param isChecked The new checked state
     */
    fun toggleTask(position: Int, isChecked: Boolean) {
        val handler = currentTaskHandler ?: return
        val currentFile = _uiState.value.currentFile ?: return

        handler.toggleTask(position, isChecked)

        val updatedFile = currentFile.copy(
            content = handler.getRenderedMarkdown(),
            taskStates = handler.getTaskStates()
        )

        _uiState.value = _uiState.value.copy(
            currentFile = updatedFile
        )

        // Update cache with new task states
        fileStateCache[currentFile.uri.toString()] = FileState(
            scrollPosition = _uiState.value.scrollPosition,
            taskStates = handler.getTaskStates()
        )
    }

    /**
     * Update the scroll position for the current file.
     * 
     * @param position The new scroll position
     */
    fun updateScrollPosition(position: Int) {
        _uiState.value = _uiState.value.copy(scrollPosition = position)
        
        // Update cache
        _uiState.value.currentFile?.let { file ->
            val currentState = fileStateCache[file.uri.toString()]
            fileStateCache[file.uri.toString()] = FileState(
                scrollPosition = position,
                taskStates = currentState?.taskStates ?: emptyMap()
            )
        }
    }

    /**
     * Save the current file state to cache before switching files.
     */
    private fun saveCurrentFileState() {
        val currentFile = _uiState.value.currentFile ?: return
        val handler = currentTaskHandler ?: return

        fileStateCache[currentFile.uri.toString()] = FileState(
            scrollPosition = _uiState.value.scrollPosition,
            taskStates = handler.getTaskStates()
        )
    }

    /**
     * Get the task states for the current file.
     * 
     * @return Map of task positions to their checked state
     */
    fun getTaskStates(): Map<Int, Boolean> {
        return currentTaskHandler?.getTaskStates() ?: emptyMap()
    }

    /**
     * Get the cached state for a file.
     * 
     * @param uri The URI of the file
     * @return The cached FileState, or null if not cached
     */
    fun getCachedFileState(uri: Uri): FileState? {
        return fileStateCache[uri.toString()]
    }

    /**
     * Clear the error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Retry loading the current file.
     */
    fun retry() {
        activeUri?.let { uri ->
            loadFile(uri, activeFileName)
        }
    }

    /**
     * Close the current file and clear state.
     */
    fun closeFile() {
        saveCurrentFileState()
        currentTaskHandler = null
        _uiState.value = MarkdownViewerUiState()
    }

    /**
     * Check if a file has cached state.
     * 
     * @param uri The URI of the file to check
     * @return true if the file has cached state
     */
    fun hasFileState(uri: Uri): Boolean {
        return fileStateCache.containsKey(uri.toString())
    }

    /**
     * Clear cached state for a specific file.
     * 
     * @param uri The URI of the file to clear state for
     */
    fun clearFileState(uri: Uri) {
        fileStateCache.remove(uri.toString())
    }

    /**
     * Clear all cached file states.
     */
    fun clearAllFileStates() {
        fileStateCache.clear()
    }

    /**
     * Get all cached file URIs.
     * 
     * @return Set of URI strings for all cached files
     */
    fun getCachedFileUris(): Set<String> {
        return fileStateCache.keys.toSet()
    }

    /**
     * Get the number of cached file states.
     * 
     * @return The count of cached file states
     */
    fun getCachedFileCount(): Int {
        return fileStateCache.size
    }

    /**
     * Check if the given URI is currently loaded or in the process of loading.
     * This helps prevent redundant load calls from the UI when a load is already initiated
     * (e.g. via loadFileWithContent).
     */
    fun isSameFileLoadedOrLoading(uri: Uri): Boolean {
        // Use string comparison to avoid issues with different encoding of the same URI
        val activeUriString = activeUri?.toString()
        val requestedUriString = uri.toString()
        
        android.util.Log.d("AzubiMarkDebug", "isSameFileCheck: active=$activeUriString, requested=$requestedUriString")
        
        // Check if it matches the active request
        if (activeUriString == requestedUriString) {
            val isLoading = _uiState.value.isLoading
            val currentUriString = _uiState.value.currentFile?.uri?.toString()
            
            android.util.Log.d("AzubiMarkDebug", "isSameFileCheck match! isLoading=$isLoading, current=$currentUriString")
            
            // It matches what we are trying to load (or have loaded)
            return isLoading || currentUriString == requestedUriString
        }
        return false
    }
}

/**
 * UI state for the Markdown viewer screen.
 */
data class MarkdownViewerUiState(
    val isLoading: Boolean = false,
    val currentFile: MarkdownFile? = null,
    val scrollPosition: Int = 0,
    val error: FileError? = null
)

/**
 * Represents cached state for a file.
 */
data class FileState(
    val scrollPosition: Int = 0,
    val taskStates: Map<Int, Boolean> = emptyMap()
)

/**
 * Sealed class representing different types of file errors.
 */
sealed class FileError(open val message: String) {
    data class FileNotFound(override val message: String) : FileError(message)
    data class PermissionDenied(override val message: String) : FileError(message)
    data class EncodingError(override val message: String) : FileError(message)
    data class ParseError(override val message: String) : FileError(message)
    data class Unknown(override val message: String) : FileError(message)
}
