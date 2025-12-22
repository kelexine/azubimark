package com.kelexine.azubimark.property

import com.kelexine.azubimark.util.isMarkdownFile
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Property-based tests for Markdown file recognition.
 *
 * Feature: azubimark-android-app, Property 18: Markdown File Recognition
 * Validates: Requirements 7.1, 7.2
 */
class MarkdownFileRecognitionTest : StringSpec({

    /**
     * Property 18: Markdown File Recognition
     *
     * For any file with .md or .markdown extension, the application should recognize it
     * as a valid Markdown file and allow opening.
     *
     * Validates: Requirements 7.1, 7.2
     */
    "Property 18: Files with .md or .markdown extensions are recognized as Markdown" {
        checkAll(100, Arb.string(1..50)) { baseName ->
            // Test .md extension (lowercase)
            val mdFile = "$baseName.md"
            isMarkdownFile(mdFile) shouldBe true

            // Test .markdown extension (lowercase)
            val markdownFile = "$baseName.markdown"
            isMarkdownFile(markdownFile) shouldBe true

            // Test .MD extension (uppercase)
            val mdUpperFile = "$baseName.MD"
            isMarkdownFile(mdUpperFile) shouldBe true

            // Test .MARKDOWN extension (uppercase)
            val markdownUpperFile = "$baseName.MARKDOWN"
            isMarkdownFile(markdownUpperFile) shouldBe true

            // Test .Md extension (mixed case)
            val mdMixedFile = "$baseName.Md"
            isMarkdownFile(mdMixedFile) shouldBe true
        }
    }

    /**
     * Additional property test: Non-markdown files should not be recognized
     */
    "Property: Files without markdown extensions are not recognized as Markdown" {
        val nonMarkdownExtensions = listOf(".txt", ".pdf", ".doc", ".html", ".json", ".xml", "")
        
        checkAll(100, Arb.string(1..50)) { baseName ->
            nonMarkdownExtensions.forEach { ext ->
                val fileName = "$baseName$ext"
                isMarkdownFile(fileName) shouldBe false
            }
        }
    }

    /**
     * Edge case: Files with markdown in the name but wrong extension
     */
    "Property: Files with 'markdown' in name but wrong extension are not recognized" {
        checkAll(100, Arb.string(1..30)) { baseName ->
            val fileName = "${baseName}_markdown.txt"
            isMarkdownFile(fileName) shouldBe false
        }
    }
})
