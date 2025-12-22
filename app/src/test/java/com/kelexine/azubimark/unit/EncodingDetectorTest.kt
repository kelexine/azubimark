package com.kelexine.azubimark.unit

import com.kelexine.azubimark.util.EncodingDetector
import com.kelexine.azubimark.util.EncodingException
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.assertions.throwables.shouldThrow
import java.io.ByteArrayInputStream
import java.nio.charset.Charset

/**
 * Unit tests for EncodingDetector edge cases.
 * 
 * Tests error conditions, recovery scenarios, and edge cases
 * for text encoding detection and decoding.
 */
class EncodingDetectorTest : DescribeSpec({

    describe("EncodingDetector") {
        
        describe("empty and minimal content handling") {
            
            it("should handle empty byte array") {
                val result = EncodingDetector.detectEncodingFromBytes(byteArrayOf())
                
                result.charset shouldBe Charsets.UTF_8
                result.hasBom shouldBe false
                result.bomLength shouldBe 0
            }
            
            it("should decode empty byte array") {
                val decoded = EncodingDetector.decodeWithAutoDetect(byteArrayOf())
                decoded shouldBe ""
            }
            
            it("should handle single byte content") {
                val bytes = byteArrayOf('A'.code.toByte())
                val decoded = EncodingDetector.decodeWithAutoDetect(bytes)
                decoded shouldBe "A"
            }
        }
        
        describe("BOM detection") {
            
            it("should detect UTF-8 BOM") {
                val utf8Bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
                val content = utf8Bom + "Hello".toByteArray(Charsets.UTF_8)
                
                val result = EncodingDetector.detectEncodingFromBytes(content)
                
                result.charset shouldBe Charsets.UTF_8
                result.hasBom shouldBe true
                result.bomLength shouldBe 3
            }
            
            it("should detect UTF-16 BE BOM") {
                val utf16BeBom = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
                val content = utf16BeBom + "Hi".toByteArray(Charsets.UTF_16BE)
                
                val result = EncodingDetector.detectEncodingFromBytes(content)
                
                result.charset shouldBe Charsets.UTF_16BE
                result.hasBom shouldBe true
                result.bomLength shouldBe 2
            }
            
            it("should detect UTF-16 LE BOM") {
                val utf16LeBom = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
                val content = utf16LeBom + "Hi".toByteArray(Charsets.UTF_16LE)
                
                val result = EncodingDetector.detectEncodingFromBytes(content)
                
                result.charset shouldBe Charsets.UTF_16LE
                result.hasBom shouldBe true
                result.bomLength shouldBe 2
            }
            
            it("should default to UTF-8 when no BOM present") {
                val content = "Hello World".toByteArray(Charsets.UTF_8)
                
                val result = EncodingDetector.detectEncodingFromBytes(content)
                
                result.charset shouldBe Charsets.UTF_8
                result.hasBom shouldBe false
                result.bomLength shouldBe 0
            }
        }
        
        describe("decoding with BOM skipping") {
            
            it("should skip UTF-8 BOM when decoding") {
                val utf8Bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
                val content = utf8Bom + "Hello".toByteArray(Charsets.UTF_8)
                
                val decoded = EncodingDetector.decodeWithAutoDetect(content)
                
                decoded shouldBe "Hello"
            }
            
            it("should skip UTF-16 BE BOM when decoding") {
                val utf16BeBom = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
                val textBytes = "Test".toByteArray(Charsets.UTF_16BE)
                val content = utf16BeBom + textBytes
                
                val decoded = EncodingDetector.decode(content, Charsets.UTF_16BE, skipBom = true)
                
                decoded shouldBe "Test"
            }
            
            it("should preserve BOM when skipBom is false") {
                val utf8Bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
                val content = utf8Bom + "Hello".toByteArray(Charsets.UTF_8)
                
                val decoded = EncodingDetector.decode(content, Charsets.UTF_8, skipBom = false)
                
                // BOM character should be present at the start
                decoded.length shouldBe 6 // BOM (1 char) + "Hello" (5 chars)
            }
        }

        
        describe("stream-based detection") {
            
            it("should detect charset from input stream") {
                val utf8Bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
                val content = utf8Bom + "Hello".toByteArray(Charsets.UTF_8)
                val stream = ByteArrayInputStream(content)
                
                val charset = EncodingDetector.detectCharset(stream)
                
                charset shouldBe Charsets.UTF_8
            }
            
            it("should reset stream after detection") {
                val content = "Hello World".toByteArray(Charsets.UTF_8)
                val stream = ByteArrayInputStream(content)
                
                EncodingDetector.detectCharset(stream)
                
                // Stream should be reset, so we can read from the beginning
                val readContent = stream.readBytes()
                String(readContent, Charsets.UTF_8) shouldBe "Hello World"
            }
        }
        
        describe("fallback decoding") {
            
            it("should use fallback for valid UTF-8 content") {
                val content = "Hello World".toByteArray(Charsets.UTF_8)
                
                val decoded = EncodingDetector.decodeWithFallback(content)
                
                decoded shouldBe "Hello World"
            }
            
            it("should handle content with special characters") {
                val content = "Héllo Wörld 你好".toByteArray(Charsets.UTF_8)
                
                val decoded = EncodingDetector.decodeWithFallback(content)
                
                decoded shouldBe "Héllo Wörld 你好"
            }
            
            it("should return non-null for any byte sequence using ISO-8859-1 fallback") {
                // Random bytes that might not be valid UTF-8
                val randomBytes = byteArrayOf(0x80.toByte(), 0x81.toByte(), 0x82.toByte())
                
                val decoded = EncodingDetector.decodeWithFallback(randomBytes)
                
                decoded shouldNotBe null
            }
        }
        
        describe("charset support checking") {
            
            it("should report UTF-8 as supported") {
                EncodingDetector.isSupportedCharset(Charsets.UTF_8) shouldBe true
            }
            
            it("should report UTF-16 variants as supported") {
                EncodingDetector.isSupportedCharset(Charsets.UTF_16) shouldBe true
                EncodingDetector.isSupportedCharset(Charsets.UTF_16BE) shouldBe true
                EncodingDetector.isSupportedCharset(Charsets.UTF_16LE) shouldBe true
            }
            
            it("should report ISO-8859-1 as not supported for detection") {
                EncodingDetector.isSupportedCharset(Charsets.ISO_8859_1) shouldBe false
            }
        }
        
        describe("large content handling") {
            
            it("should handle large UTF-8 content") {
                val largeContent = "A".repeat(100_000)
                val bytes = largeContent.toByteArray(Charsets.UTF_8)
                
                val decoded = EncodingDetector.decodeWithAutoDetect(bytes)
                
                decoded shouldBe largeContent
            }
            
            it("should handle content with many newlines") {
                val content = (1..1000).joinToString("\n") { "Line $it" }
                val bytes = content.toByteArray(Charsets.UTF_8)
                
                val decoded = EncodingDetector.decodeWithAutoDetect(bytes)
                
                decoded shouldBe content
            }
        }
        
        describe("unicode content") {
            
            it("should handle emoji content") {
                val content = "Hello 👋 World 🌍"
                val bytes = content.toByteArray(Charsets.UTF_8)
                
                val decoded = EncodingDetector.decodeWithAutoDetect(bytes)
                
                decoded shouldBe content
            }
            
            it("should handle mixed language content") {
                val content = "English 日本語 한국어 العربية"
                val bytes = content.toByteArray(Charsets.UTF_8)
                
                val decoded = EncodingDetector.decodeWithAutoDetect(bytes)
                
                decoded shouldBe content
            }
            
            it("should handle mathematical symbols") {
                val content = "∑ ∫ √ π ∞ ≠ ≤ ≥"
                val bytes = content.toByteArray(Charsets.UTF_8)
                
                val decoded = EncodingDetector.decodeWithAutoDetect(bytes)
                
                decoded shouldBe content
            }
        }
    }
})
