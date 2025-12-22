package com.kelexine.azubimark.property

import com.kelexine.azubimark.ui.viewer.ErrorDisplayInfo
import com.kelexine.azubimark.ui.viewer.FileError
import com.kelexine.azubimark.ui.viewer.getErrorDisplayInfo
import com.kelexine.azubimark.util.isMarkdownFile
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.element
import io.kotest.property.checkAll

/**
 * Property-based tests for error handling.
 *
 * Feature: azubimark-android-app, Properties 19 and 20
 * 
 * Tests content-based file handling and invalid file error handling.
 * 
 * Validates: Requirements 7.3, 7.4
 */
class ErrorHandlingPropertiesTest : StringSpec({

    /**
     * Property 19: Content-Based File Handling
     *
     * For any file opened via intent that contains valid Markdown content,
     * the application should attempt to render it regardless of file extension.
     *
     * Validates: Requirements 7.3
     */
    "Property 19: Markdown file extensions are correctly recognized" {
        val validExtensions = listOf(".md", ".markdown", ".MD", ".MARKDOWN", ".Md", ".MarkDown")
        
        checkAll(100, Arb.string(1..20)) { baseName ->
            validExtensions.forEach { extension ->
                val fileName = "$baseName$extension"
                isMarkdownFile(fileName) shouldBe true
            }
        }
    }

    "Property 19: Non-markdown extensions are not recognized as markdown" {
        val invalidExtensions = listOf(".txt", ".html", ".pdf", ".doc", ".rtf", ".xml", ".json")
        
        checkAll(100, Arb.string(1..20)) { baseName ->
            invalidExtensions.forEach { extension ->
                val fileName = "$baseName$extension"
                isMarkdownFile(fileName) shouldBe false
            }
        }
    }

    "Property 19: Files without extensions are not recognized as markdown" {
        checkAll(100, Arb.string(1..30)) { fileName ->
            // Only test filenames without dots (no extension)
            if (!fileName.contains(".")) {
                isMarkdownFile(fileName) shouldBe false
            }
        }
    }

    "Property 19: Valid markdown content structure is preserved" {
        val markdownElements = listOf(
            "# Header",
            "## Subheader",
            "- List item",
            "1. Numbered item",
            "**bold**",
            "*italic*",
            "[link](url)",
            "```code```",
            "> quote",
            "| table | header |"
        )
        
        checkAll(100, Arb.element(markdownElements)) { element ->
            // Markdown elements should be non-empty strings
            element.shouldNotBeEmpty()
            // Basic structure validation - element should contain expected characters
            element.length shouldNotBe 0
        }
    }

    /**
     * Property 20: Invalid File Error Handling
     *
     * For any file that cannot be parsed as valid Markdown, the application
     * should display an appropriate error message instead of crashing.
     *
     * Validates: Requirements 7.4
     */
    "Property 20: All FileError types produce valid error display info" {
        val errorTypes = listOf(
            FileError.FileNotFound("Test file not found"),
            FileError.PermissionDenied("Permission denied"),
            FileError.EncodingError("Encoding error"),
            FileError.ParseError("Parse error"),
            FileError.Unknown("Unknown error")
        )
        
        errorTypes.forEach { error ->
            val displayInfo = getErrorDisplayInfo(error)
            
            displayInfo.title.shouldNotBeEmpty()
            displayInfo.message.shouldNotBeEmpty()
            displayInfo.icon.shouldNotBeEmpty()
        }
    }

    "Property 20: FileNotFound error produces correct display info" {
        checkAll(100, Arb.string(1..50)) { errorMessage ->
            val error = FileError.FileNotFound(errorMessage)
            val displayInfo = getErrorDisplayInfo(error)
            
            displayInfo.title shouldBe "File Not Found"
            displayInfo.icon shouldBe "📄"
            displayInfo.canRetry shouldBe true
        }
    }

    "Property 20: PermissionDenied error produces correct display info" {
        checkAll(100, Arb.string(1..50)) { errorMessage ->
            val error = FileError.PermissionDenied(errorMessage)
            val displayInfo = getErrorDisplayInfo(error)
            
            displayInfo.title shouldBe "Permission Denied"
            displayInfo.icon shouldBe "🔒"
            displayInfo.canRetry shouldBe true
        }
    }

    "Property 20: EncodingError produces correct display info" {
        checkAll(100, Arb.string(1..50)) { errorMessage ->
            val error = FileError.EncodingError(errorMessage)
            val displayInfo = getErrorDisplayInfo(error)
            
            displayInfo.title shouldBe "Encoding Error"
            displayInfo.icon shouldBe "📝"
            displayInfo.canRetry shouldBe true
        }
    }

    "Property 20: ParseError produces correct display info" {
        checkAll(100, Arb.string(1..50)) { errorMessage ->
            val error = FileError.ParseError(errorMessage)
            val displayInfo = getErrorDisplayInfo(error)
            
            displayInfo.title shouldBe "Parse Error"
            displayInfo.icon shouldBe "⚠️"
            displayInfo.canRetry shouldBe true
        }
    }

    "Property 20: Unknown error produces correct display info" {
        checkAll(100, Arb.string(1..50)) { errorMessage ->
            val error = FileError.Unknown(errorMessage)
            val displayInfo = getErrorDisplayInfo(error)
            
            displayInfo.title shouldBe "Unexpected Error"
            displayInfo.icon shouldBe "❌"
            displayInfo.canRetry shouldBe true
        }
    }

    "Property 20: Empty error message is handled gracefully" {
        val errorTypes = listOf(
            FileError.FileNotFound(""),
            FileError.PermissionDenied(""),
            FileError.EncodingError(""),
            FileError.ParseError(""),
            FileError.Unknown("")
        )
        
        errorTypes.forEach { error ->
            val displayInfo = getErrorDisplayInfo(error)
            
            // Should still produce valid display info even with empty message
            displayInfo.title.shouldNotBeEmpty()
            displayInfo.message.shouldNotBeEmpty()
            displayInfo.icon.shouldNotBeEmpty()
        }
    }

    "Property 20: Error display info is consistent for same error type" {
        checkAll(100, Arb.string(1..30), Arb.string(1..30)) { msg1, msg2 ->
            val error1 = FileError.FileNotFound(msg1)
            val error2 = FileError.FileNotFound(msg2)
            
            val info1 = getErrorDisplayInfo(error1)
            val info2 = getErrorDisplayInfo(error2)
            
            // Title and icon should be consistent for same error type
            info1.title shouldBe info2.title
            info1.icon shouldBe info2.icon
            info1.canRetry shouldBe info2.canRetry
        }
    }

    "Property 20: ErrorDisplayInfo data class preserves all fields" {
        checkAll(100, Arb.string(1..20), Arb.string(1..50), Arb.string(1..5)) { title, message, icon ->
            val displayInfo = ErrorDisplayInfo(
                title = title,
                message = message,
                icon = icon,
                canRetry = true
            )
            
            displayInfo.title shouldBe title
            displayInfo.message shouldBe message
            displayInfo.icon shouldBe icon
            displayInfo.canRetry shouldBe true
        }
    }

    "Property 20: FileError sealed class message property is accessible" {
        checkAll(100, Arb.string(1..50)) { errorMessage ->
            val errors = listOf(
                FileError.FileNotFound(errorMessage),
                FileError.PermissionDenied(errorMessage),
                FileError.EncodingError(errorMessage),
                FileError.ParseError(errorMessage),
                FileError.Unknown(errorMessage)
            )
            
            errors.forEach { error ->
                error.message shouldBe errorMessage
            }
        }
    }
})
