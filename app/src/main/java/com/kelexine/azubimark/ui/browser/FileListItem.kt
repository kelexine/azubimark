package com.kelexine.azubimark.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.kelexine.azubimark.data.model.FileItem
import java.text.SimpleDateFormat
import java.util.*

/**
 * Individual file/folder list item component.
 * 
 * Displays file or folder with appropriate Material 3 icon, name, and metadata.
 * Provides ripple effect touch feedback.
 * 
 * Validates: Requirements 2.2
 * 
 * @param fileItem The file or folder to display
 * @param onClick Callback when the item is clicked
 * @param modifier Optional modifier for the component
 */
@Composable
fun FileListItem(
    fileItem: FileItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(
                text = fileItem.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            val metadata = buildFileMetadata(fileItem)
            if (metadata.isNotEmpty()) {
                Text(
                    text = metadata,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = if (fileItem.isDirectory) {
                    Icons.Filled.Folder
                } else {
                    Icons.Filled.Description
                },
                contentDescription = if (fileItem.isDirectory) "Folder" else "Markdown file",
                tint = if (fileItem.isDirectory) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        modifier = modifier.clickable(onClick = onClick)
    )
}

/**
 * Build metadata string for a file item.
 * 
 * @param fileItem The file item to build metadata for
 * @return Formatted metadata string (size and/or date)
 */
private fun buildFileMetadata(fileItem: FileItem): String {
    val parts = mutableListOf<String>()
    
    // Add file size for files
    if (!fileItem.isDirectory && fileItem.size != null) {
        parts.add(formatFileSize(fileItem.size))
    }
    
    // Add last modified date if available
    fileItem.lastModified?.let { timestamp ->
        if (timestamp > 0) {
            parts.add(formatDate(timestamp))
        }
    }
    
    return parts.joinToString(" • ")
}

/**
 * Format file size to human-readable string.
 * 
 * @param bytes File size in bytes
 * @return Formatted size string (e.g., "1.5 MB")
 */
fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

/**
 * Format timestamp to human-readable date string.
 * 
 * @param timestamp Timestamp in milliseconds
 * @return Formatted date string
 */
private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
