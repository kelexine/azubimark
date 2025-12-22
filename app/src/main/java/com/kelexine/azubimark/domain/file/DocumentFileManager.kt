package com.kelexine.azubimark.domain.file

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.kelexine.azubimark.data.model.FileItem
import com.kelexine.azubimark.util.EncodingDetector
import com.kelexine.azubimark.util.isMarkdownFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Implementation of FileManager using DocumentFile and Storage Access Framework.
 * 
 * Provides secure file access following Android's scoped storage guidelines.
 * Supports browsing directories, reading Markdown files, and handling external intents.
 *
 * @property context Application context for accessing content resolver and file system
 */
class DocumentFileManager(private val context: Context) : FileManager {

    /**
     * Browse a directory and return its contents filtered to show only
     * directories and Markdown files (.md and .markdown extensions).
     *
     * @param uri The URI of the directory to browse, or null to browse the default storage location
     * @return List of FileItem objects representing the directory contents
     */
    override suspend fun browseDirectory(uri: Uri?): List<FileItem> = withContext(Dispatchers.IO) {
        try {
            when {
                uri == null -> {
                    // Default: browse external storage using direct file access
                    // This works on Android 10 and below, or with MANAGE_EXTERNAL_STORAGE on 11+
                    browseFileDirectory(Environment.getExternalStorageDirectory())
                }
                uri.scheme == "file" -> {
                    // Handle file:// URIs with direct file access
                    val file = File(uri.path ?: return@withContext emptyList())
                    browseFileDirectory(file)
                }
                uri.scheme == "content" -> {
                    // Handle content:// URIs from SAF
                    browseContentDirectory(uri)
                }
                else -> emptyList()
            }
        } catch (e: SecurityException) {
            // Permission denied - return empty list
            emptyList()
        } catch (e: Exception) {
            // Other errors - return empty list
            emptyList()
        }
    }

    /**
     * Browse a directory using direct file access.
     * Returns FileItems with content:// URIs for files (using FileProvider would be ideal,
     * but for simplicity we'll read files directly when selected).
     */
    private fun browseFileDirectory(directory: File): List<FileItem> {
        if (!directory.exists() || !directory.isDirectory) {
            return emptyList()
        }
        
        return directory.listFiles()?.mapNotNull { file ->
            val fileName = file.name
            
            // Filter: only show directories and Markdown files
            if (file.isDirectory || isMarkdownFile(fileName)) {
                FileItem(
                    name = fileName,
                    uri = Uri.fromFile(file),
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else null,
                    lastModified = file.lastModified().takeIf { it > 0 },
                    mimeType = if (isMarkdownFile(fileName)) "text/markdown" else null
                )
            } else {
                null
            }
        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()
    }

    /**
     * Browse a directory using SAF content:// URIs.
     */
    private fun browseContentDirectory(uri: Uri): List<FileItem> {
        val documentFile = DocumentFile.fromTreeUri(context, uri)
            ?: DocumentFile.fromSingleUri(context, uri)
            ?: return emptyList()

        return documentFile.listFiles().mapNotNull { file ->
            val fileName = file.name ?: return@mapNotNull null
            
            // Filter: only show directories and Markdown files
            if (file.isDirectory || isMarkdownFile(fileName)) {
                FileItem(
                    name = fileName,
                    uri = file.uri,
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else null,
                    lastModified = file.lastModified().takeIf { it > 0 },
                    mimeType = file.type
                )
            } else {
                null
            }
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }


    /**
     * Read the content of a file from the given URI.
     *
     * Supports UTF-8 and UTF-16 encoded files with automatic encoding detection.
     * Uses the EncodingDetector to determine the correct charset and properly
     * handle BOM (Byte Order Mark) if present.
     *
     * @param uri The URI of the file to read
     * @return The content of the file as a String
     * @throws FileNotFoundException if the file cannot be found
     * @throws IOException if there's an error reading the file
     * @throws SecurityException if permission to read the file is denied
     */
    override suspend fun readFile(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            when (uri.scheme) {
                "file" -> {
                    // Handle file:// URIs directly
                    val file = File(uri.path ?: throw FileNotFoundException("Invalid file path"))
                    if (!file.exists()) {
                        throw FileNotFoundException("File not found: ${uri.path}")
                    }
                    if (!file.canRead()) {
                        throw SecurityException("Cannot read file: ${uri.path}. Storage permission may be required.")
                    }
                    readFileWithEncodingDetection(file.readBytes())
                }
                "content" -> {
                    // Handle content:// URIs through ContentResolver
                    val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.readBytes()
                    } ?: throw FileNotFoundException("Cannot open file: $uri")
                    
                    readFileWithEncodingDetection(bytes)
                }
                else -> {
                    throw IOException("Unsupported URI scheme: ${uri.scheme}")
                }
            }
        } catch (e: SecurityException) {
            throw SecurityException("Permission denied to read file: $uri. Please grant storage access.", e)
        } catch (e: FileNotFoundException) {
            throw e
        } catch (e: com.kelexine.azubimark.util.EncodingException) {
            throw IOException("Encoding error reading file: ${e.message}", e)
        } catch (e: IOException) {
            throw IOException("Error reading file: $uri", e)
        }
    }

    /**
     * Read file content with automatic encoding detection and proper BOM handling.
     *
     * Detects UTF-8, UTF-16 BE, and UTF-16 LE encodings based on BOM.
     * Falls back to UTF-8 if no BOM is detected.
     * Handles encoding errors gracefully with fallback mechanisms.
     *
     * @param bytes The raw bytes of the file
     * @return The decoded string content
     * @throws IOException if the content cannot be decoded with any supported encoding
     */
    private fun readFileWithEncodingDetection(bytes: ByteArray): String {
        // Try to decode with auto-detection first
        return try {
            EncodingDetector.decodeWithAutoDetect(bytes)
        } catch (e: com.kelexine.azubimark.util.EncodingException) {
            // If auto-detect fails, try fallback mechanism
            EncodingDetector.decodeWithFallback(bytes)
                ?: throw IOException("Unable to decode file content: ${e.message}", e)
        }
    }

    /**
     * Check if a file is a Markdown file based on its name.
     *
     * Recognizes files with .md or .markdown extensions (case-insensitive).
     *
     * @param fileName The name of the file to check
     * @return true if the file has a Markdown extension, false otherwise
     */
    override fun isMarkdownFile(fileName: String): Boolean {
        return com.kelexine.azubimark.util.isMarkdownFile(fileName)
    }

    /**
     * Handle an external intent to open a Markdown file.
     *
     * Extracts the file URI from intents with ACTION_VIEW or ACTION_SEND.
     * Validates that the URI points to a Markdown file before returning.
     *
     * @param intent The intent received from an external application
     * @return The URI of the Markdown file, or null if the intent doesn't contain a valid Markdown file
     */
    override fun handleExternalIntent(intent: Intent): Uri? {
        return when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    // Validate that it's a Markdown file or accept any file opened via intent
                    // (per requirement 7.3: attempt to render files opened via intent regardless of extension)
                    uri
                }
            }
            Intent.ACTION_SEND -> {
                // Handle shared content
                if (intent.type?.startsWith("text/") == true) {
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                } else {
                    null
                }
            }
            else -> null
        }
    }

    companion object {
        /**
         * MIME types recognized as Markdown files.
         */
        val MARKDOWN_MIME_TYPES = setOf(
            "text/markdown",
            "text/x-markdown",
            "text/plain"
        )
    }
}
