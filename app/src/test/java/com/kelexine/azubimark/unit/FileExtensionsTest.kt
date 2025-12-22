package com.kelexine.azubimark.unit

import com.kelexine.azubimark.util.isMarkdownFile
import com.kelexine.azubimark.util.validateMarkdownExtension
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for FileExtensions utility functions.
 * 
 * Tests edge cases for markdown file detection and validation.
 */
class FileExtensionsTest : DescribeSpec({

    describe("isMarkdownFile") {
        
        describe("valid markdown extensions") {
            
            it("should recognize .md extension") {
                isMarkdownFile("README.md") shouldBe true
                isMarkdownFile("document.md") shouldBe true
                isMarkdownFile("file.md") shouldBe true
            }
            
            it("should recognize .markdown extension") {
                isMarkdownFile("README.markdown") shouldBe true
                isMarkdownFile("document.markdown") shouldBe true
                isMarkdownFile("file.markdown") shouldBe true
            }
            
            it("should be case-insensitive for .md") {
                isMarkdownFile("file.MD") shouldBe true
                isMarkdownFile("file.Md") shouldBe true
                isMarkdownFile("file.mD") shouldBe true
            }
            
            it("should be case-insensitive for .markdown") {
                isMarkdownFile("file.MARKDOWN") shouldBe true
                isMarkdownFile("file.Markdown") shouldBe true
                isMarkdownFile("file.MarkDown") shouldBe true
            }
        }
        
        describe("invalid extensions") {
            
            it("should reject .txt files") {
                isMarkdownFile("file.txt") shouldBe false
            }
            
            it("should reject .html files") {
                isMarkdownFile("file.html") shouldBe false
            }
            
            it("should reject .mdx files") {
                isMarkdownFile("file.mdx") shouldBe false
            }
            
            it("should reject files without extension") {
                isMarkdownFile("README") shouldBe false
                isMarkdownFile("file") shouldBe false
            }
            
            it("should reject hidden files without markdown extension") {
                isMarkdownFile(".gitignore") shouldBe false
                isMarkdownFile(".hidden") shouldBe false
            }
        }
        
        describe("edge cases") {
            
            it("should handle empty string") {
                isMarkdownFile("") shouldBe false
            }
            
            it("should handle just the extension") {
                isMarkdownFile(".md") shouldBe true
                isMarkdownFile(".markdown") shouldBe true
            }
            
            it("should handle files with multiple dots") {
                isMarkdownFile("file.backup.md") shouldBe true
                isMarkdownFile("my.document.markdown") shouldBe true
                isMarkdownFile("file.md.txt") shouldBe false
            }
            
            it("should handle files with spaces") {
                isMarkdownFile("my file.md") shouldBe true
                isMarkdownFile("my document.markdown") shouldBe true
            }
            
            it("should handle files with special characters") {
                isMarkdownFile("file-name.md") shouldBe true
                isMarkdownFile("file_name.md") shouldBe true
                isMarkdownFile("file (1).md") shouldBe true
            }
            
            it("should handle unicode filenames") {
                isMarkdownFile("文档.md") shouldBe true
                isMarkdownFile("документ.markdown") shouldBe true
            }
            
            it("should not match md in the middle of filename") {
                isMarkdownFile("readme.md.bak") shouldBe false
                isMarkdownFile("markdown.txt") shouldBe false
            }
        }
    }
    
    describe("validateMarkdownExtension") {
        
        it("should behave identically to isMarkdownFile") {
            validateMarkdownExtension("file.md") shouldBe isMarkdownFile("file.md")
            validateMarkdownExtension("file.markdown") shouldBe isMarkdownFile("file.markdown")
            validateMarkdownExtension("file.txt") shouldBe isMarkdownFile("file.txt")
            validateMarkdownExtension("") shouldBe isMarkdownFile("")
        }
    }
})
