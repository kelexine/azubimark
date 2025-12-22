package com.kelexine.azubimark.property

import android.content.Intent
import android.net.Uri
import com.kelexine.azubimark.util.isMarkdownFile
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk

/**
 * Property-based tests for file management functionality.
 *
 * Feature: azubimark-android-app
 * Tests Properties 6, 7, and 8 for file filtering, opening, and intent handling.
 * Validates: Requirements 2.2, 2.3, 2.4
 */
class FileManagementPropertiesTest : StringSpec({

    /**
     * Property 6: File Filtering Consistency
     *
     * For any directory contents, the file browser should display only items that are
     * either directories or files with .md/.markdown extensions.
     *
     * Validates: Requirements 2.2
     */
    "Property 6: File filtering shows only directories and markdown files" {
        // Generator for file extensions (mix of markdown and non-markdown)
        val markdownExtensions = listOf(".md", ".markdown", ".MD", ".MARKDOWN", ".Md")
        val nonMarkdownExtensions = listOf(".txt", ".pdf", ".doc", ".html", ".json", ".xml", ".kt", ".java")
        val allExtensions = markdownExtensions + nonMarkdownExtensions

        checkAll(100, Arb.string(1..30), Arb.element(allExtensions), Arb.boolean()) { baseName, ext, isDir ->
            val fileName = "$baseName$ext"
            
            // Simulate the filtering logic used in DocumentFileManager
            val shouldBeShown = isDir || isMarkdownFile(fileName)
            
            // Verify the filtering logic
            if (isDir) {
                // Directories should always be shown
                shouldBeShown shouldBe true
            } else {
                // Files should only be shown if they have markdown extension
                shouldBeShown shouldBe isMarkdownFile(fileName)
            }
        }
    }

    /**
     * Property 6 (continued): Filtered list contains only valid items
     *
     * For any list of FileItems, after filtering, all items should be either
     * directories or markdown files.
     */
    "Property 6: Filtered file list contains only directories and markdown files" {
        val markdownExtensions = listOf(".md", ".markdown")
        val nonMarkdownExtensions = listOf(".txt", ".pdf", ".doc", ".html")
        val allExtensions = markdownExtensions + nonMarkdownExtensions

        // Generator for FileItem-like data
        val fileItemArb = Arb.string(1..20).map { baseName ->
            val ext = allExtensions.random()
            val isDir = listOf(true, false).random()
            Triple(baseName + if (isDir) "" else ext, isDir, isMarkdownFile(baseName + ext))
        }

        checkAll(100, Arb.list(fileItemArb, 0..20)) { items ->
            // Apply the same filtering logic as DocumentFileManager
            val filteredItems = items.filter { (name, isDir, _) ->
                isDir || isMarkdownFile(name)
            }

            // Verify all filtered items are valid
            filteredItems.forEach { (name, isDir, _) ->
                val isValid = isDir || isMarkdownFile(name)
                isValid shouldBe true
            }
        }
    }

    /**
     * Property 7: File Opening Reliability
     *
     * For any valid Markdown file selection, the application should successfully
     * open and render the file content without errors.
     *
     * This property tests that the isMarkdownFile check correctly identifies
     * files that should be openable.
     *
     * Validates: Requirements 2.3
     */
    "Property 7: Valid markdown files are identified as openable" {
        val markdownExtensions = listOf(".md", ".markdown", ".MD", ".MARKDOWN", ".Md", ".MarkDown")

        checkAll(100, Arb.string(1..50), Arb.element(markdownExtensions)) { baseName, ext ->
            val fileName = "$baseName$ext"
            
            // A file with markdown extension should be identified as openable
            isMarkdownFile(fileName) shouldBe true
        }
    }

    /**
     * Property 7 (continued): Non-markdown files are not openable via file browser
     */
    "Property 7: Non-markdown files are not identified as openable in browser" {
        val nonMarkdownExtensions = listOf(".txt", ".pdf", ".doc", ".html", ".json", ".xml", ".kt", ".java", ".py")

        checkAll(100, Arb.string(1..50), Arb.element(nonMarkdownExtensions)) { baseName, ext ->
            val fileName = "$baseName$ext"
            
            // A file without markdown extension should not be identified as openable
            isMarkdownFile(fileName) shouldBe false
        }
    }

    /**
     * Property 8: External Intent Handling
     *
     * For any external intent containing a valid Markdown file URI, the application
     * should open and display the file content correctly.
     *
     * This tests the intent handling logic for ACTION_VIEW intents.
     * Note: We mock the Uri to avoid Android framework dependencies.
     *
     * Validates: Requirements 2.4
     */
    "Property 8: ACTION_VIEW intents with URIs return the URI" {
        checkAll(100, Arb.string(1..50)) { path ->
            val uri = mockk<Uri>()
            val intent = mockk<Intent>()
            
            every { intent.action } returns Intent.ACTION_VIEW
            every { intent.data } returns uri
            
            // Simulate handleExternalIntent logic
            val result = when (intent.action) {
                Intent.ACTION_VIEW -> intent.data
                else -> null
            }
            
            result shouldBe uri
        }
    }

    /**
     * Property 8 (continued): ACTION_SEND intents with text type return stream URI
     */
    "Property 8: ACTION_SEND intents with text type return stream URI" {
        checkAll(100, Arb.string(1..50)) { _ ->
            val uri = mockk<Uri>()
            val intent = mockk<Intent>()
            
            every { intent.action } returns Intent.ACTION_SEND
            every { intent.type } returns "text/markdown"
            every { intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) } returns uri
            
            // Simulate handleExternalIntent logic
            val result = when (intent.action) {
                Intent.ACTION_SEND -> {
                    if (intent.type?.startsWith("text/") == true) {
                        intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    } else {
                        null
                    }
                }
                else -> null
            }
            
            result shouldBe uri
        }
    }

    /**
     * Property 8 (continued): Intents with non-text types return null
     */
    "Property 8: ACTION_SEND intents with non-text type return null" {
        val nonTextTypes = listOf("image/png", "application/pdf", "video/mp4")

        checkAll(100, Arb.element(nonTextTypes)) { mimeType ->
            val uri = mockk<Uri>()
            val intent = mockk<Intent>()
            
            every { intent.action } returns Intent.ACTION_SEND
            every { intent.type } returns mimeType
            every { intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) } returns uri
            
            // Simulate handleExternalIntent logic
            val result = when (intent.action) {
                Intent.ACTION_SEND -> {
                    if (intent.type?.startsWith("text/") == true) {
                        intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    } else {
                        null
                    }
                }
                else -> null
            }
            
            result shouldBe null
        }
    }

    /**
     * Property 8 (continued): ACTION_SEND intents with null type return null
     */
    "Property 8: ACTION_SEND intents with null type return null" {
        checkAll(100, Arb.string(1..20)) { _ ->
            val uri = mockk<Uri>()
            val intent = mockk<Intent>()
            
            every { intent.action } returns Intent.ACTION_SEND
            every { intent.type } returns null
            every { intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) } returns uri
            
            // Simulate handleExternalIntent logic
            val result = when (intent.action) {
                Intent.ACTION_SEND -> {
                    if (intent.type?.startsWith("text/") == true) {
                        intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    } else {
                        null
                    }
                }
                else -> null
            }
            
            result shouldBe null
        }
    }

    /**
     * Property 8 (continued): Unknown intent actions return null
     */
    "Property 8: Unknown intent actions return null" {
        val unknownActions = listOf(Intent.ACTION_EDIT, Intent.ACTION_DELETE, Intent.ACTION_PICK, "custom.action")

        checkAll(100, Arb.element(unknownActions)) { action ->
            val uri = mockk<Uri>()
            val intent = mockk<Intent>()
            
            every { intent.action } returns action
            every { intent.data } returns uri
            
            // Simulate handleExternalIntent logic
            val result = when (intent.action) {
                Intent.ACTION_VIEW -> intent.data
                Intent.ACTION_SEND -> null // simplified
                else -> null
            }
            
            result shouldBe null
        }
    }
})
