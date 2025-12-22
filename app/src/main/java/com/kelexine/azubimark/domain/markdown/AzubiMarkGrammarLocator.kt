package com.kelexine.azubimark.domain.markdown

import io.noties.prism4j.GrammarLocator
import io.noties.prism4j.Prism4j
import io.noties.prism4j.annotations.PrismBundle

/**
 * Grammar locator for Prism4j syntax highlighting.
 * 
 * Provides support for common programming languages used in Markdown code blocks.
 * The annotation processor generates the actual grammar definitions.
 * 
 * Validates: Requirements 1.3
 */
@PrismBundle(
    include = [
        "c",
        "clike",
        "clojure",
        "cpp",
        "csharp",
        "css",
        "dart",
        "git",
        "go",
        "groovy",
        "java",
        "javascript",
        "json",
        "kotlin",
        "latex",
        "makefile",
        "markdown",
        "markup",
        "python",
        "scala",
        "sql",
        "swift",
        "yaml"
    ],
    grammarLocatorClassName = ".AzubiMarkGrammarLocatorDef"
)
class AzubiMarkGrammarLocator : GrammarLocator {
    
    private val delegate: GrammarLocator? = try {
        // Try to load the generated grammar locator
        Class.forName("com.kelexine.azubimark.domain.markdown.AzubiMarkGrammarLocatorDef")
            .getDeclaredConstructor()
            .newInstance() as? GrammarLocator
    } catch (e: Exception) {
        null
    }

    override fun grammar(prism4j: Prism4j, language: String): Prism4j.Grammar? {
        return delegate?.grammar(prism4j, language)
    }

    override fun languages(): Set<String> {
        return delegate?.languages() ?: SUPPORTED_LANGUAGES
    }

    companion object {
        /**
         * Set of supported programming languages for syntax highlighting.
         */
        val SUPPORTED_LANGUAGES = setOf(
            "c", "clike", "clojure", "cpp", "csharp", "css",
            "dart", "git", "go", "groovy", "java", "javascript",
            "json", "kotlin", "latex", "makefile", "markdown",
            "markup", "python", "scala", "sql", "swift", "yaml",
            // Common aliases
            "js", "ts", "typescript", "py", "rb", "ruby",
            "sh", "bash", "shell", "html", "xml"
        )

        /**
         * Map of language aliases to their canonical names.
         */
        val LANGUAGE_ALIASES = mapOf(
            "js" to "javascript",
            "ts" to "javascript",
            "typescript" to "javascript",
            "py" to "python",
            "rb" to "clike",
            "ruby" to "clike",
            "sh" to "clike",
            "bash" to "clike",
            "shell" to "clike",
            "html" to "markup",
            "xml" to "markup"
        )

        /**
         * Resolve a language name to its canonical form.
         */
        fun resolveLanguage(language: String): String {
            val normalized = language.lowercase().trim()
            return LANGUAGE_ALIASES[normalized] ?: normalized
        }

        /**
         * Check if a language is supported for syntax highlighting.
         */
        fun isLanguageSupported(language: String): Boolean {
            val resolved = resolveLanguage(language)
            return resolved in SUPPORTED_LANGUAGES || resolved in LANGUAGE_ALIASES.values
        }
    }
}
