package com.kelexine.azubimark.data.model

import android.net.Uri

/**
 * Represents a file or directory item in the file system.
 *
 * @property name The display name of the file or directory
 * @property uri The URI pointing to the file or directory
 * @property isDirectory True if this item is a directory, false if it's a file
 * @property size The size of the file in bytes, null for directories
 * @property lastModified The last modified timestamp in milliseconds, null if unavailable
 * @property mimeType The MIME type of the file, null if unavailable
 */
data class FileItem(
    val name: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val size: Long? = null,
    val lastModified: Long? = null,
    val mimeType: String? = null
)
