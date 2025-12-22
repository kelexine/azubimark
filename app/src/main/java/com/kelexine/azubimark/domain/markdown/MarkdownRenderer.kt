package com.kelexine.azubimark.domain.markdown

import android.text.Spanned
import androidx.compose.material3.ColorScheme

/**
 * Interface for Markdown rendering functionality.
 * Implementations should handle parsing and rendering of Markdown content.
 */
interface MarkdownRenderer {
    /**
     * Parse Markdown content and return styled text.
     */
    fun parseMarkdown(content: String): Spanned

    /**
     * Configure the renderer's theme based on dark mode and dynamic colors.
     */
    fun configureTheme(isDark: Boolean, dynamicColors: ColorScheme?)

    /**
     * Enable or disable syntax highlighting for code blocks.
     */
    fun enableSyntaxHighlighting(enabled: Boolean)
}
