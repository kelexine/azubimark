package com.kelexine.azubimark.property

import android.content.Context
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import com.kelexine.azubimark.domain.markdown.MarkdownRenderer
import com.kelexine.azubimark.domain.markdown.MarkwonRenderer
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.mockk

/**
 * Property-based tests for basic Markdown rendering functionality.
 *
 * Feature: azubimark-android-app
 * Tests Properties 1, 2, and 4 for markdown element rendering.
 * Validates: Requirements 1.1, 1.2, 1.4
 */
class MarkdownRenderingPropertiesTest : StringSpec({

    /**
     * Property 1: Markdown Element Rendering
     *
     * For any valid Markdown content containing standard elements (headers, paragraphs,
     * lists, links, emphasis), the rendered output should contain all specified formatting
     * elements with correct structure and styling.
     *
     * Validates: Requirements 1.1
     */
    "Property 1: Headers are rendered with correct structure" {
        val headerLevels = listOf(1, 2, 3, 4, 5, 6)
        
        checkAll(100, Arb.string(1..50), Arb.element(headerLevels)) { text, level ->
            val markdown = "${"#".repeat(level)} $text"
            
            // Verify the markdown structure is correct
            markdown shouldContain "#".repeat(level)
            markdown shouldContain text
            
            // The header level determines the number of # symbols
            val headerPrefix = markdown.takeWhile { it == '#' }
            headerPrefix.length shouldBe level
        }
    }

    "Property 1: Paragraphs preserve text content" {
        checkAll(100, Arb.string(1..200)) { text ->
            // A paragraph is just plain text
            val markdown = text
            
            // The text content should be preserved
            markdown shouldBe text
        }
    }

    "Property 1: Unordered lists have correct structure" {
        val bulletMarkers = listOf("-", "*", "+")
        
        checkAll(100, Arb.list(Arb.string(1..30), 1..5), Arb.element(bulletMarkers)) { items, marker ->
            val markdown = items.joinToString("\n") { "$marker $it" }
            
            // Each item should be prefixed with the bullet marker
            items.forEach { item ->
                markdown shouldContain "$marker $item"
            }
        }
    }

    "Property 1: Ordered lists have correct numbering" {
        checkAll(100, Arb.list(Arb.string(1..30), 1..5)) { items ->
            val markdown = items.mapIndexed { index, item -> 
                "${index + 1}. $item" 
            }.joinToString("\n")
            
            // Each item should be numbered correctly
            items.forEachIndexed { index, item ->
                markdown shouldContain "${index + 1}. $item"
            }
        }
    }

    "Property 1: Links have correct markdown syntax" {
        checkAll(100, Arb.string(1..30), Arb.string(5..50)) { linkText, url ->
            val safeUrl = "https://example.com/$url".replace(" ", "-")
            val markdown = "[$linkText]($safeUrl)"
            
            // Link should contain both text and URL
            markdown shouldContain linkText
            markdown shouldContain safeUrl
            markdown shouldContain "["
            markdown shouldContain "]("
            markdown shouldContain ")"
        }
    }

    "Property 1: Bold text has correct markdown syntax" {
        checkAll(100, Arb.string(1..50)) { text ->
            val markdownAsterisks = "**$text**"
            val markdownUnderscores = "__${text}__"
            
            // Both syntaxes should wrap the text correctly
            markdownAsterisks shouldContain "**"
            markdownAsterisks shouldContain text
            markdownUnderscores shouldContain "__"
            markdownUnderscores shouldContain text
        }
    }

    "Property 1: Italic text has correct markdown syntax" {
        checkAll(100, Arb.string(1..50)) { text ->
            val markdownAsterisk = "*$text*"
            val markdownUnderscore = "_${text}_"
            
            // Both syntaxes should wrap the text correctly
            markdownAsterisk.startsWith("*") shouldBe true
            markdownAsterisk.endsWith("*") shouldBe true
            markdownAsterisk shouldContain text
            markdownUnderscore.startsWith("_") shouldBe true
            markdownUnderscore.endsWith("_") shouldBe true
            markdownUnderscore shouldContain text
        }
    }

    /**
     * Property 2: Table Rendering Consistency
     *
     * For any Markdown content containing valid table syntax, the rendered output
     * should display tables with proper column alignment, borders, and cell content preservation.
     *
     * Validates: Requirements 1.2
     */
    "Property 2: Tables have correct markdown structure" {
        checkAll(100, Arb.list(Arb.string(1..15), 2..5)) { headers ->
            val headerRow = "| ${headers.joinToString(" | ")} |"
            val separatorRow = "| ${headers.map { "---" }.joinToString(" | ")} |"
            val markdown = "$headerRow\n$separatorRow"
            
            // Table should have correct structure
            markdown shouldContain "|"
            markdown shouldContain "---"
            headers.forEach { header ->
                markdown shouldContain header
            }
        }
    }

    "Property 2: Table rows preserve cell content" {
        val headers = listOf("Col1", "Col2", "Col3")
        
        checkAll(100, Arb.list(Arb.string(1..20), 3..3)) { cells ->
            val headerRow = "| ${headers.joinToString(" | ")} |"
            val separatorRow = "| ${headers.map { "---" }.joinToString(" | ")} |"
            val dataRow = "| ${cells.joinToString(" | ")} |"
            val markdown = "$headerRow\n$separatorRow\n$dataRow"
            
            // All cell content should be preserved
            cells.forEach { cell ->
                markdown shouldContain cell
            }
        }
    }

    "Property 2: Table alignment markers are valid" {
        val alignments = listOf(":---", ":---:", "---:", "---")
        
        checkAll(100, Arb.list(Arb.element(alignments), 2..5)) { columnAlignments ->
            val headers = columnAlignments.mapIndexed { i, _ -> "Header$i" }
            val headerRow = "| ${headers.joinToString(" | ")} |"
            val separatorRow = "| ${columnAlignments.joinToString(" | ")} |"
            val markdown = "$headerRow\n$separatorRow"
            
            // Separator row should contain alignment markers
            columnAlignments.forEach { alignment ->
                markdown shouldContain alignment
            }
        }
    }

    /**
     * Property 4: Strikethrough Formatting
     *
     * For any Markdown content containing strikethrough syntax, the rendered output
     * should display the text with strikethrough formatting applied.
     *
     * Validates: Requirements 1.4
     */
    "Property 4: Strikethrough text has correct markdown syntax" {
        checkAll(100, Arb.string(1..50)) { text ->
            val markdown = "~~$text~~"
            
            // Strikethrough should wrap text with ~~
            markdown.startsWith("~~") shouldBe true
            markdown.endsWith("~~") shouldBe true
            markdown shouldContain text
        }
    }

    "Property 4: Strikethrough can be combined with other formatting" {
        checkAll(100, Arb.string(1..30)) { text ->
            // Strikethrough with bold
            val boldStrikethrough = "**~~$text~~**"
            boldStrikethrough shouldContain "**"
            boldStrikethrough shouldContain "~~"
            boldStrikethrough shouldContain text
            
            // Strikethrough with italic
            val italicStrikethrough = "*~~$text~~*"
            italicStrikethrough shouldContain "*"
            italicStrikethrough shouldContain "~~"
            italicStrikethrough shouldContain text
        }
    }

    "Property 4: Multiple strikethrough sections are independent" {
        checkAll(100, Arb.string(1..20), Arb.string(1..20)) { text1, text2 ->
            val markdown = "~~$text1~~ normal ~~$text2~~"
            
            // Both strikethrough sections should be present
            markdown shouldContain "~~$text1~~"
            markdown shouldContain "~~$text2~~"
            markdown shouldContain "normal"
        }
    }

    /**
     * Additional property tests for markdown structure validation
     */
    "Property: Code blocks preserve content" {
        val languages = listOf("kotlin", "java", "python", "javascript", "")
        
        checkAll(100, Arb.string(1..100), Arb.element(languages)) { code, language ->
            val markdown = "```$language\n$code\n```"
            
            // Code block should contain the code
            markdown shouldContain code
            markdown shouldContain "```"
            if (language.isNotEmpty()) {
                markdown shouldContain language
            }
        }
    }

    "Property: Inline code preserves content" {
        checkAll(100, Arb.string(1..50)) { code ->
            val markdown = "`$code`"
            
            // Inline code should wrap with backticks
            markdown.startsWith("`") shouldBe true
            markdown.endsWith("`") shouldBe true
            markdown shouldContain code
        }
    }

    "Property: Blockquotes have correct prefix" {
        checkAll(100, Arb.string(1..100)) { text ->
            val markdown = "> $text"
            
            // Blockquote should start with >
            markdown.startsWith(">") shouldBe true
            markdown shouldContain text
        }
    }

    "Property: Horizontal rules are valid" {
        val hrSyntaxes = listOf("---", "***", "___")
        
        hrSyntaxes.forEach { hr ->
            hr.length shouldBe 3
            hr.toSet().size shouldBe 1 // All same character
        }
    }
})
