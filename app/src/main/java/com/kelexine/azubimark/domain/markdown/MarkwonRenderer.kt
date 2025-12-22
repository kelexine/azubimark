package com.kelexine.azubimark.domain.markdown

import android.content.Context
import android.text.Spanned
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.syntax.Prism4jTheme
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j

/**
 * Markwon-based implementation of MarkdownRenderer.
 * 
 * Provides comprehensive Markdown rendering with:
 * - Tables support
 * - Task lists (checkboxes)
 * - Strikethrough text
 * - Automatic link detection
 * - Syntax highlighting for code blocks
 * 
 * Validates: Requirements 1.1, 1.2, 1.4
 */
class MarkwonRenderer(private val context: Context) : MarkdownRenderer {

    private var markwon: Markwon
    private val grammarLocator: AzubiMarkGrammarLocator = AzubiMarkGrammarLocator()
    private val prism4j: Prism4j = Prism4j(grammarLocator)
    private var prism4jTheme: Prism4jTheme = Prism4jThemeDefault.create()
    private var syntaxHighlightingEnabled: Boolean = true
    private var isDarkTheme: Boolean = false
    private var dynamicColorScheme: ColorScheme? = null

    init {
        markwon = buildMarkwon()
    }

    /**
     * Build the Markwon instance with all configured plugins.
     */
    private fun buildMarkwon(): Markwon {
        val builder = Markwon.builder(context)
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(LinkifyPlugin.create())

        if (syntaxHighlightingEnabled) {
            builder.usePlugin(SyntaxHighlightPlugin.create(prism4j, prism4jTheme))
        }

        return builder.build()
    }

    /**
     * Parse Markdown content and return styled Spanned text.
     * 
     * Handles all standard Markdown elements including:
     * - Headers (h1-h6)
     * - Paragraphs
     * - Lists (ordered and unordered)
     * - Links and images
     * - Emphasis (bold, italic)
     * - Tables
     * - Task lists
     * - Strikethrough
     * - Code blocks with syntax highlighting
     */
    override fun parseMarkdown(content: String): Spanned {
        return markwon.toMarkdown(content)
    }

    /**
     * Configure the renderer's theme based on dark mode and dynamic colors.
     * 
     * Updates the syntax highlighting theme to match the app's current theme.
     * Rebuilds the Markwon instance to apply the new theme.
     */
    override fun configureTheme(isDark: Boolean, dynamicColors: ColorScheme?) {
        this.isDarkTheme = isDark
        this.dynamicColorScheme = dynamicColors

        // Update syntax highlighting theme based on dark mode
        prism4jTheme = if (isDark) {
            // Use Darkula theme for dark mode, optionally with dynamic colors
            dynamicColors?.let { colors ->
                createDynamicDarkTheme(colors)
            } ?: Prism4jThemeDarkula.create()
        } else {
            // Use default theme for light mode, optionally with dynamic colors
            dynamicColors?.let { colors ->
                createDynamicLightTheme(colors)
            } ?: Prism4jThemeDefault.create()
        }

        // Rebuild Markwon with new theme
        markwon = buildMarkwon()
    }

    /**
     * Enable or disable syntax highlighting for code blocks.
     * 
     * When disabled, code blocks will be rendered as plain monospace text.
     */
    override fun enableSyntaxHighlighting(enabled: Boolean) {
        if (this.syntaxHighlightingEnabled != enabled) {
            this.syntaxHighlightingEnabled = enabled
            markwon = buildMarkwon()
        }
    }

    /**
     * Create a dynamic dark theme using Material You colors.
     */
    private fun createDynamicDarkTheme(colors: ColorScheme): Prism4jTheme {
        return DynamicPrism4jTheme(
            backgroundColor = colors.surfaceVariant.toArgb(),
            textColor = colors.onSurface.toArgb(),
            keywordColor = colors.primary.toArgb(),
            stringColor = colors.tertiary.toArgb(),
            commentColor = colors.outline.toArgb()
        )
    }

    /**
     * Create a dynamic light theme using Material You colors.
     */
    private fun createDynamicLightTheme(colors: ColorScheme): Prism4jTheme {
        return DynamicPrism4jTheme(
            backgroundColor = colors.surfaceVariant.toArgb(),
            textColor = colors.onSurface.toArgb(),
            keywordColor = colors.primary.toArgb(),
            stringColor = colors.tertiary.toArgb(),
            commentColor = colors.outline.toArgb()
        )
    }

    /**
     * Get the current Markwon instance for direct access if needed.
     */
    fun getMarkwon(): Markwon = markwon

    /**
     * Check if syntax highlighting is currently enabled.
     */
    fun isSyntaxHighlightingEnabled(): Boolean = syntaxHighlightingEnabled

    /**
     * Check if dark theme is currently active.
     */
    fun isDarkTheme(): Boolean = isDarkTheme

    /**
     * Check if a language is supported for syntax highlighting.
     */
    fun isLanguageSupported(language: String): Boolean = 
        AzubiMarkGrammarLocator.isLanguageSupported(language)

    /**
     * Get the set of supported languages.
     */
    fun getSupportedLanguages(): Set<String> = 
        AzubiMarkGrammarLocator.SUPPORTED_LANGUAGES
}

/**
 * Dynamic Prism4j theme that uses Material You colors.
 */
private class DynamicPrism4jTheme(
    private val backgroundColor: Int,
    private val textColor: Int,
    private val keywordColor: Int,
    private val stringColor: Int,
    private val commentColor: Int
) : Prism4jTheme {

    override fun background(): Int = backgroundColor

    override fun textColor(): Int = textColor

    override fun apply(
        language: String,
        syntax: Prism4j.Syntax,
        builder: SpannableStringBuilder,
        start: Int,
        end: Int
    ) {
        val color = when (syntax.type()) {
            "keyword", "boolean", "number", "operator" -> keywordColor
            "string", "char" -> stringColor
            "comment", "prolog", "doctype", "cdata" -> commentColor
            "punctuation" -> textColor
            "function", "class-name" -> keywordColor
            else -> textColor
        }
        builder.setSpan(ForegroundColorSpan(color), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}
