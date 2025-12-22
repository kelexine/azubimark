package com.kelexine.azubimark.domain.file

import android.content.Intent
import android.net.Uri
import com.kelexine.azubimark.data.model.FileItem

/**
 * Interface for file system operations.
 */
interface FileManager {
    /**
     * Browse a directory and return its contents.
     */
    suspend fun browseDirectory(uri: Uri?): List<FileItem>

    /**
     * Read file content from URI.
     */
    suspend fun readFile(uri: Uri): String

    /**
     * Check if a file is a Markdown file based on its name.
     */
    fun isMarkdownFile(fileName: String): Boolean

    /**
     * Handle external intent to open a file.
     */
    fun handleExternalIntent(intent: Intent): Uri?
}
