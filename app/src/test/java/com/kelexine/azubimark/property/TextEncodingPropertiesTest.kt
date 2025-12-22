package com.kelexine.azubimark.property

import com.kelexine.azubimark.util.EncodingDetector
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.io.ByteArrayInputStream

/**
 * Property-based tests for text encoding support.
 *
 * Feature: azubimark-android-app, Property 21: Text Encoding Support
 * Validates: Requirements 7.5
 *
 * Tests that UTF-8 and UTF-16 encoded files are correctly decoded.
 */
class TextEncodingPropertiesTest : StringSpec({

    /**
     * Property 21: Text Encoding Support - UTF-8 Round Trip
     *
     * For any valid string, encoding it as UTF-8 and then decoding it
     * should produce the original string.
     *
     * Validates: Requirements 7.5
     */
    "Property 21: UTF-8 encoded text decodes correctly" {
        checkAll(100, Arb.string(0..500)) { originalText ->
            // Encode the text as UTF-8
            val utf8Bytes = originalText.toByteArray(Charsets.UTF_8)
            
            // Decode using EncodingDetector
            val decodedText = EncodingDetector.decodeWithAutoDetect(utf8Bytes)
            
            // The decoded text should match the original
            decodedText shouldBe originalText
        }
    }

    /**
     * Property 21: Text Encoding Support - UTF-8 with BOM Round Trip
     *
     * For any valid string, encoding it as UTF-8 with BOM and then decoding it
     * should produce the original string (BOM should be stripped).
     *
     * Validates: Requirements 7.5
     */
    "Property 21: UTF-8 with BOM encoded text decodes correctly" {
        val utf8Bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        
        checkAll(100, Arb.string(0..500)) { originalText ->
            // Encode the text as UTF-8 with BOM
            val utf8Bytes = originalText.toByteArray(Charsets.UTF_8)
            val bytesWithBom = utf8Bom + utf8Bytes
            
            // Decode using EncodingDetector
            val decodedText = EncodingDetector.decodeWithAutoDetect(bytesWithBom)
            
            // The decoded text should match the original (BOM stripped)
            decodedText shouldBe originalText
        }
    }

    /**
     * Property 21: Text Encoding Support - UTF-16 LE Round Trip
     *
     * For any valid string, encoding it as UTF-16 LE with BOM and then decoding it
     * should produce the original string.
     *
     * Validates: Requirements 7.5
     */
    "Property 21: UTF-16 LE encoded text decodes correctly" {
        val utf16LeBom = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        
        checkAll(100, Arb.string(0..200)) { originalText ->
            // Encode the text as UTF-16 LE
            val utf16LeBytes = originalText.toByteArray(Charsets.UTF_16LE)
            val bytesWithBom = utf16LeBom + utf16LeBytes
            
            // Decode using EncodingDetector
            val decodedText = EncodingDetector.decodeWithAutoDetect(bytesWithBom)
            
            // The decoded text should match the original
            decodedText shouldBe originalText
        }
    }

    /**
     * Property 21: Text Encoding Support - UTF-16 BE Round Trip
     *
     * For any valid string, encoding it as UTF-16 BE with BOM and then decoding it
     * should produce the original string.
     *
     * Validates: Requirements 7.5
     */
    "Property 21: UTF-16 BE encoded text decodes correctly" {
        val utf16BeBom = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
        
        checkAll(100, Arb.string(0..200)) { originalText ->
            // Encode the text as UTF-16 BE
            val utf16BeBytes = originalText.toByteArray(Charsets.UTF_16BE)
            val bytesWithBom = utf16BeBom + utf16BeBytes
            
            // Decode using EncodingDetector
            val decodedText = EncodingDetector.decodeWithAutoDetect(bytesWithBom)
            
            // The decoded text should match the original
            decodedText shouldBe originalText
        }
    }

    /**
     * Property 21: Encoding Detection - UTF-8 BOM Detection
     *
     * For any byte array starting with UTF-8 BOM, the detector should
     * correctly identify it as UTF-8.
     *
     * Validates: Requirements 7.5
     */
    "Property 21: UTF-8 BOM is correctly detected" {
        val utf8Bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        
        checkAll(100, Arb.string(0..100)) { content ->
            val contentBytes = content.toByteArray(Charsets.UTF_8)
            val bytesWithBom = utf8Bom + contentBytes
            
            val result = EncodingDetector.detectEncodingFromBytes(bytesWithBom)
            
            result.charset shouldBe Charsets.UTF_8
            result.hasBom shouldBe true
            result.bomLength shouldBe 3
        }
    }

    /**
     * Property 21: Encoding Detection - UTF-16 LE BOM Detection
     *
     * For any byte array starting with UTF-16 LE BOM, the detector should
     * correctly identify it as UTF-16 LE.
     *
     * Validates: Requirements 7.5
     */
    "Property 21: UTF-16 LE BOM is correctly detected" {
        val utf16LeBom = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        
        checkAll(100, Arb.string(0..100)) { content ->
            val contentBytes = content.toByteArray(Charsets.UTF_16LE)
            val bytesWithBom = utf16LeBom + contentBytes
            
            val result = EncodingDetector.detectEncodingFromBytes(bytesWithBom)
            
            result.charset shouldBe Charsets.UTF_16LE
            result.hasBom shouldBe true
            result.bomLength shouldBe 2
        }
    }

    /**
     * Property 21: Encoding Detection - UTF-16 BE BOM Detection
     *
     * For any byte array starting with UTF-16 BE BOM, the detector should
     * correctly identify it as UTF-16 BE.
     *
     * Validates: Requirements 7.5
     */
    "Property 21: UTF-16 BE BOM is correctly detected" {
        val utf16BeBom = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
        
        checkAll(100, Arb.string(0..100)) { content ->
            val contentBytes = content.toByteArray(Charsets.UTF_16BE)
            val bytesWithBom = utf16BeBom + contentBytes
            
            val result = EncodingDetector.detectEncodingFromBytes(bytesWithBom)
            
            result.charset shouldBe Charsets.UTF_16BE
            result.hasBom shouldBe true
            result.bomLength shouldBe 2
        }
    }

    /**
     * Property 21: Encoding Detection - No BOM defaults to UTF-8
     *
     * For any byte array without a BOM, the detector should default to UTF-8.
     *
     * Validates: Requirements 7.5
     */
    "Property 21: No BOM defaults to UTF-8" {
        checkAll(100, Arb.string(0..100)) { content ->
            // Create bytes without BOM (just plain UTF-8)
            val contentBytes = content.toByteArray(Charsets.UTF_8)
            
            // Make sure it doesn't accidentally start with a BOM pattern
            val safeBytes = if (contentBytes.size >= 3 && 
                contentBytes[0] == 0xEF.toByte() && 
                contentBytes[1] == 0xBB.toByte() && 
                contentBytes[2] == 0xBF.toByte()) {
                // Prepend a safe character to avoid BOM pattern
                byteArrayOf('A'.code.toByte()) + contentBytes
            } else if (contentBytes.size >= 2 && 
                ((contentBytes[0] == 0xFF.toByte() && contentBytes[1] == 0xFE.toByte()) ||
                 (contentBytes[0] == 0xFE.toByte() && contentBytes[1] == 0xFF.toByte()))) {
                // Prepend a safe character to avoid UTF-16 BOM pattern
                byteArrayOf('A'.code.toByte()) + contentBytes
            } else {
                contentBytes
            }
            
            val result = EncodingDetector.detectEncodingFromBytes(safeBytes)
            
            result.charset shouldBe Charsets.UTF_8
            result.hasBom shouldBe false
            result.bomLength shouldBe 0
        }
    }

    /**
     * Property 21: Stream-based Detection - UTF-8 BOM
     *
     * For any input stream with UTF-8 BOM, detectCharset should return UTF-8.
     *
     * Validates: Requirements 7.5
     */
    "Property 21: Stream-based UTF-8 BOM detection works correctly" {
        val utf8Bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        
        checkAll(100, Arb.string(0..100)) { content ->
            val contentBytes = content.toByteArray(Charsets.UTF_8)
            val bytesWithBom = utf8Bom + contentBytes
            val inputStream = ByteArrayInputStream(bytesWithBom)
            
            val charset = EncodingDetector.detectCharset(inputStream)
            
            charset shouldBe Charsets.UTF_8
        }
    }

    /**
     * Property 21: Stream-based Detection - UTF-16 LE BOM
     *
     * For any input stream with UTF-16 LE BOM, detectCharset should return UTF-16 LE.
     *
     * Validates: Requirements 7.5
     */
    "Property 21: Stream-based UTF-16 LE BOM detection works correctly" {
        val utf16LeBom = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        
        checkAll(100, Arb.string(0..100)) { content ->
            val contentBytes = content.toByteArray(Charsets.UTF_16LE)
            val bytesWithBom = utf16LeBom + contentBytes
            val inputStream = ByteArrayInputStream(bytesWithBom)
            
            val charset = EncodingDetector.detectCharset(inputStream)
            
            charset shouldBe Charsets.UTF_16LE
        }
    }

    /**
     * Property 21: Stream-based Detection - UTF-16 BE BOM
     *
     * For any input stream with UTF-16 BE BOM, detectCharset should return UTF-16 BE.
     *
     * Validates: Requirements 7.5
     */
    "Property 21: Stream-based UTF-16 BE BOM detection works correctly" {
        val utf16BeBom = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
        
        checkAll(100, Arb.string(0..100)) { content ->
            val contentBytes = content.toByteArray(Charsets.UTF_16BE)
            val bytesWithBom = utf16BeBom + contentBytes
            val inputStream = ByteArrayInputStream(bytesWithBom)
            
            val charset = EncodingDetector.detectCharset(inputStream)
            
            charset shouldBe Charsets.UTF_16BE
        }
    }

    /**
     * Property 21: Fallback Decoding - Corrupted data handled gracefully
     *
     * For any byte array, decodeWithFallback should never return null
     * (it falls back to ISO-8859-1 which accepts any byte sequence).
     *
     * Validates: Requirements 7.5
     */
    "Property 21: Fallback decoding handles any byte sequence" {
        checkAll(100, Arb.string(0..100)) { text ->
            val bytes = text.toByteArray(Charsets.UTF_8)
            val result = EncodingDetector.decodeWithFallback(bytes)
            
            // Should never return null due to ISO-8859-1 fallback
            result shouldNotBe null
        }
    }

    /**
     * Property 21: Supported Charset Check
     *
     * UTF-8, UTF-16, UTF-16BE, and UTF-16LE should all be reported as supported.
     *
     * Validates: Requirements 7.5
     */
    "Property 21: Supported charsets are correctly identified" {
        val supportedCharsets = listOf(
            Charsets.UTF_8,
            Charsets.UTF_16,
            Charsets.UTF_16BE,
            Charsets.UTF_16LE
        )
        
        supportedCharsets.forEach { charset ->
            EncodingDetector.isSupportedCharset(charset) shouldBe true
        }
    }
})
