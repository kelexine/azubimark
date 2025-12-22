package com.kelexine.azubimark.util

/**
 * Utility functions for file type detection and validation.
 */

/**
 * Checks if a filename represents a Markdown file based on its extension.
 *
 * Recognizes files with .md or .markdown extensions (case-insensitive).
 *
 * @param fileName The name of the file to check
 * @return true if the file has a Markdown extension, false otherwise
 */
fun isMarkdownFile(fileName: String): Boolean {
    val lowerCaseName = fileName.lowercase()
    return lowerCaseName.endsWith(".md") || lowerCaseName.endsWith(".markdown")
}

/**
 * Validates if a filename has a valid Markdown extension.
 *
 * This is an alias for isMarkdownFile for clarity in validation contexts.
 *
 * @param fileName The name of the file to validate
 * @return true if the file has a valid Markdown extension, false otherwise
 */
fun validateMarkdownExtension(fileName: String): Boolean {
    return isMarkdownFile(fileName)
}
