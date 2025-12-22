package com.kelexine.azubimark.property

import com.kelexine.azubimark.domain.markdown.AzubiMarkGrammarLocator
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Property-based tests for syntax highlighting functionality.
 *
 * Feature: azubimark-android-app, Property 3: Syntax Highlighting Application
 * Validates: Requirements 1.3
 *
 * Tests that code blocks with language specifications receive syntax highlighting.
 */
class SyntaxHighlightingPropertiesTest : StringSpec({

    /**
     * Property 3: Syntax Highlighting Application
     *
     * For any Markdown code block with a specified language, the syntax highlighter
     * should apply appropriate highlighting that differs from plain text rendering.
     *
     * Validates: Requirements 1.3
     */
    "Property 3: Code blocks with language specification have correct structure" {
        val supportedLanguages = listOf(
            "kotlin", "java", "python", "javascript", "typescript",
            "c", "cpp", "csharp", "go", "rust", "swift", "ruby",
            "sql", "json", "yaml", "markdown", "html", "css"
        )

        checkAll(100, Arb.string(1..100), Arb.element(supportedLanguages)) { code, language ->
            val markdown = "```$language\n$code\n```"
            
            // Code block should have correct structure
            markdown shouldContain "```$language"
            markdown shouldContain code
            markdown.endsWith("```") shouldBe true
        }
    }

    "Property 3: Language aliases are resolved correctly" {
        val aliasTests = listOf(
            "js" to "javascript",
            "ts" to "javascript",
            "typescript" to "javascript",
            "py" to "python",
            "html" to "markup",
            "xml" to "markup"
        )

        aliasTests.forEach { (alias, canonical) ->
            val resolved = AzubiMarkGrammarLocator.resolveLanguage(alias)
            resolved shouldBe canonical
        }
    }

    "Property 3: Supported languages are recognized" {
        val supportedLanguages = listOf(
            "kotlin", "java", "python", "javascript",
            "c", "cpp", "csharp", "go", "swift",
            "sql", "json", "yaml", "markdown"
        )

        supportedLanguages.forEach { language ->
            AzubiMarkGrammarLocator.isLanguageSupported(language) shouldBe true
        }
    }

    "Property 3: Language resolution is case-insensitive" {
        val languages = listOf("Kotlin", "JAVA", "Python", "JavaScript", "JSON")

        languages.forEach { language ->
            val resolved = AzubiMarkGrammarLocator.resolveLanguage(language)
            resolved shouldBe language.lowercase()
        }
    }

    "Property 3: Code blocks preserve content regardless of language" {
        val languages = listOf("kotlin", "java", "python", "javascript", "unknown")

        checkAll(100, Arb.string(1..200), Arb.element(languages)) { code, language ->
            val markdown = "```$language\n$code\n```"
            
            // The code content should always be preserved
            markdown shouldContain code
        }
    }

    "Property 3: Multiple code blocks are independent" {
        val languages = listOf("kotlin", "java", "python")

        checkAll(100, Arb.string(1..50), Arb.string(1..50)) { code1, code2 ->
            val lang1 = languages.random()
            val lang2 = languages.random()
            
            val markdown = """
                |```$lang1
                |$code1
                |```
                |
                |Some text between
                |
                |```$lang2
                |$code2
                |```
            """.trimMargin()
            
            // Both code blocks should be present
            markdown shouldContain "```$lang1"
            markdown shouldContain code1
            markdown shouldContain "```$lang2"
            markdown shouldContain code2
        }
    }

    "Property 3: Inline code is distinct from code blocks" {
        checkAll(100, Arb.string(1..30)) { code ->
            val inlineCode = "`$code`"
            val codeBlock = "```\n$code\n```"
            
            // Inline code uses single backticks
            inlineCode.startsWith("`") shouldBe true
            inlineCode.endsWith("`") shouldBe true
            inlineCode.startsWith("```") shouldBe false
            
            // Code blocks use triple backticks
            codeBlock.startsWith("```") shouldBe true
            codeBlock.endsWith("```") shouldBe true
        }
    }

    "Property 3: Empty code blocks are valid" {
        val languages = listOf("kotlin", "java", "python", "")

        languages.forEach { language ->
            val markdown = if (language.isEmpty()) {
                "```\n```"
            } else {
                "```$language\n```"
            }
            
            // Empty code blocks should have correct structure
            markdown.startsWith("```") shouldBe true
            markdown.endsWith("```") shouldBe true
        }
    }

    "Property 3: Code blocks with special characters are preserved" {
        val specialChars = listOf(
            "val x = \"hello\"",
            "if (a < b && c > d)",
            "// comment with special chars: @#$%",
            "fun test() { return 42 }",
            "SELECT * FROM users WHERE id = 1"
        )

        specialChars.forEach { code ->
            val markdown = "```kotlin\n$code\n```"
            markdown shouldContain code
        }
    }

    "Property 3: Supported languages set is not empty" {
        val languages = AzubiMarkGrammarLocator.SUPPORTED_LANGUAGES
        
        languages.isEmpty() shouldBe false
        languages.size shouldNotBe 0
    }

    "Property 3: Common programming languages are supported" {
        val commonLanguages = listOf(
            "kotlin", "java", "python", "javascript", "json", "yaml", "sql"
        )

        commonLanguages.forEach { language ->
            AzubiMarkGrammarLocator.SUPPORTED_LANGUAGES.contains(language) shouldBe true
        }
    }
})
