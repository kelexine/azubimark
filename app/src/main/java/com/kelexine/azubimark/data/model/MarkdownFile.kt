package com.kelexine.azubimark.data.model

import android.net.Uri

/**
 * Represents a Markdown file with its content and metadata.
 *
 * @property uri The URI pointing to the Markdown file
 * @property name The display name of the file
 * @property content The raw Markdown content of the file
 * @property lastModified The last modified timestamp in milliseconds
 * @property taskStates Map of task list positions to their checked state (true = checked, false = unchecked)
 */
data class MarkdownFile(
    val uri: Uri,
    val name: String,
    val content: String,
    val lastModified: Long,
    val taskStates: Map<Int, Boolean> = emptyMap()
)
