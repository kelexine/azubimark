package com.kelexine.azubimark.util

import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CodingErrorAction
import java.nio.charset.MalformedInputException
import java.nio.charset.UnmappableCharacterException

/**
 * Result of encoding detection containing the detected charset and whether a BOM was found.
 *
 * @property charset The detected character encoding
 * @property hasBom Whether a Byte Order Mark was detected
 * @property bomLength The length of the BOM in bytes (0 if no BOM)
 */
data class EncodingResult(
    val charset: Charset,
    val hasBom: Boolean,
    val bomLength: Int
)

/**
 * Exception thrown when text encoding detection or decoding fails.
 *
 * @property message Description of the encoding error
 * @property cause The underlying exception that caused this error
 */
class EncodingException(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause)

/**
 * Utility for detecting and handling text encodings.
 *
 * Supports UTF-8, UTF-16 (both LE and BE variants) with automatic BOM detection.
 * Falls back to UTF-8 when encoding cannot be determined from BOM.
 *
 * Requirements: 7.5 - Handle files with different text encodings (UTF-8, UTF-16) correctly
 */
object EncodingDetector {

    // BOM (Byte Order Mark) constants
    private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    private val UTF16_BE_BOM = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
    private val UTF16_LE_BOM = byteArrayOf(0xFF.toByte(), 0xFE.toByte())

    /**
     * Detect charset from input stream by examining the Byte Order Mark (BOM).
     *
     * Supports detection of:
     * - UTF-8 (with or without BOM)
     * - UTF-16 Big Endian (with BOM)
     * - UTF-16 Little Endian (with BOM)
     *
     * Falls back to UTF-8 if no BOM is detected.
     *
     * @param inputStream The input stream to examine (must support mark/reset)
     * @return The detected Charset
     * @throws EncodingException if the stream doesn't support mark/reset
     */
    fun detectCharset(inputStream: InputStream): Charset {
        return detectEncoding(inputStream).charset
    }

    /**
     * Detect encoding from input stream with detailed result including BOM information.
     *
     * @param inputStream The input stream to examine (must support mark/reset)
     * @return EncodingResult containing charset, BOM presence, and BOM length
     * @throws EncodingException if the stream doesn't support mark/reset
     */
    fun detectEncoding(inputStream: InputStream): EncodingResult {
        if (!inputStream.markSupported()) {
            throw EncodingException("Input stream does not support mark/reset operations")
        }

        val bom = ByteArray(4)
        inputStream.mark(4)
        val bytesRead = try {
            inputStream.read(bom)
        } catch (e: Exception) {
            inputStream.reset()
            throw EncodingException("Failed to read BOM bytes", e)
        }

        try {
            inputStream.reset()
        } catch (e: Exception) {
            throw EncodingException("Failed to reset stream after BOM detection", e)
        }

        // Check for UTF-8 BOM (3 bytes: EF BB BF)
        if (bytesRead >= 3 && matchesBom(bom, UTF8_BOM)) {
            return EncodingResult(Charsets.UTF_8, hasBom = true, bomLength = 3)
        }

        // Check for UTF-16 BE BOM (2 bytes: FE FF)
        if (bytesRead >= 2 && matchesBom(bom, UTF16_BE_BOM)) {
            return EncodingResult(Charsets.UTF_16BE, hasBom = true, bomLength = 2)
        }

        // Check for UTF-16 LE BOM (2 bytes: FF FE)
        if (bytesRead >= 2 && matchesBom(bom, UTF16_LE_BOM)) {
            return EncodingResult(Charsets.UTF_16LE, hasBom = true, bomLength = 2)
        }

        // No BOM detected - default to UTF-8
        return EncodingResult(Charsets.UTF_8, hasBom = false, bomLength = 0)
    }

    /**
     * Decode bytes to string using the specified charset with proper error handling.
     *
     * @param bytes The byte array to decode
     * @param charset The charset to use for decoding
     * @param skipBom Whether to skip the BOM if present
     * @return The decoded string
     * @throws EncodingException if decoding fails due to malformed or unmappable characters
     */
    fun decode(bytes: ByteArray, charset: Charset, skipBom: Boolean = true): String {
        val decoder = createStrictDecoder(charset)
        
        val startOffset = if (skipBom) {
            getBomLength(bytes, charset)
        } else {
            0
        }

        val bytesToDecode = if (startOffset > 0) {
            bytes.copyOfRange(startOffset, bytes.size)
        } else {
            bytes
        }

        return try {
            val charBuffer = decoder.decode(java.nio.ByteBuffer.wrap(bytesToDecode))
            charBuffer.toString()
        } catch (e: MalformedInputException) {
            throw EncodingException("Malformed input: bytes cannot be decoded as ${charset.name()}", e)
        } catch (e: UnmappableCharacterException) {
            throw EncodingException("Unmappable character found when decoding as ${charset.name()}", e)
        } catch (e: Exception) {
            throw EncodingException("Failed to decode bytes as ${charset.name()}", e)
        }
    }

    /**
     * Decode bytes to string with automatic encoding detection.
     *
     * @param bytes The byte array to decode
     * @return The decoded string
     * @throws EncodingException if decoding fails
     */
    fun decodeWithAutoDetect(bytes: ByteArray): String {
        val encodingResult = detectEncodingFromBytes(bytes)
        return decode(bytes, encodingResult.charset, skipBom = true)
    }

    /**
     * Detect encoding from a byte array by examining the BOM.
     *
     * @param bytes The byte array to examine
     * @return EncodingResult containing charset and BOM information
     */
    fun detectEncodingFromBytes(bytes: ByteArray): EncodingResult {
        // Check for UTF-8 BOM
        if (bytes.size >= 3 && matchesBom(bytes, UTF8_BOM)) {
            return EncodingResult(Charsets.UTF_8, hasBom = true, bomLength = 3)
        }

        // Check for UTF-16 BE BOM
        if (bytes.size >= 2 && matchesBom(bytes, UTF16_BE_BOM)) {
            return EncodingResult(Charsets.UTF_16BE, hasBom = true, bomLength = 2)
        }

        // Check for UTF-16 LE BOM
        if (bytes.size >= 2 && matchesBom(bytes, UTF16_LE_BOM)) {
            return EncodingResult(Charsets.UTF_16LE, hasBom = true, bomLength = 2)
        }

        // Default to UTF-8
        return EncodingResult(Charsets.UTF_8, hasBom = false, bomLength = 0)
    }

    /**
     * Try to decode bytes with fallback to UTF-8 if the detected encoding fails.
     *
     * @param bytes The byte array to decode
     * @return The decoded string, or null if all decoding attempts fail
     */
    fun decodeWithFallback(bytes: ByteArray): String? {
        // First, try auto-detection
        return try {
            decodeWithAutoDetect(bytes)
        } catch (e: EncodingException) {
            // If auto-detect fails, try UTF-8 as fallback
            try {
                decode(bytes, Charsets.UTF_8, skipBom = false)
            } catch (e2: EncodingException) {
                // If UTF-8 fails, try ISO-8859-1 as last resort (accepts any byte sequence)
                try {
                    String(bytes, Charsets.ISO_8859_1)
                } catch (e3: Exception) {
                    null
                }
            }
        }
    }

    /**
     * Check if the given bytes match the expected BOM pattern.
     */
    private fun matchesBom(bytes: ByteArray, bom: ByteArray): Boolean {
        if (bytes.size < bom.size) return false
        for (i in bom.indices) {
            if (bytes[i] != bom[i]) return false
        }
        return true
    }

    /**
     * Get the BOM length for the given charset if present in the bytes.
     */
    private fun getBomLength(bytes: ByteArray, charset: Charset): Int {
        return when {
            charset == Charsets.UTF_8 && bytes.size >= 3 && matchesBom(bytes, UTF8_BOM) -> 3
            charset == Charsets.UTF_16BE && bytes.size >= 2 && matchesBom(bytes, UTF16_BE_BOM) -> 2
            charset == Charsets.UTF_16LE && bytes.size >= 2 && matchesBom(bytes, UTF16_LE_BOM) -> 2
            charset == Charsets.UTF_16 && bytes.size >= 2 -> {
                when {
                    matchesBom(bytes, UTF16_BE_BOM) -> 2
                    matchesBom(bytes, UTF16_LE_BOM) -> 2
                    else -> 0
                }
            }
            else -> 0
        }
    }

    /**
     * Create a strict charset decoder that reports errors instead of replacing characters.
     */
    private fun createStrictDecoder(charset: Charset): CharsetDecoder {
        return charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
    }

    /**
     * Check if the given charset is supported for encoding detection.
     */
    fun isSupportedCharset(charset: Charset): Boolean {
        return charset == Charsets.UTF_8 ||
                charset == Charsets.UTF_16 ||
                charset == Charsets.UTF_16BE ||
                charset == Charsets.UTF_16LE
    }
}
