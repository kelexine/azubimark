package me.kelexine.azubimark

import android.content.Context
import android.util.LruCache
import kotlinx.coroutines.*
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages caching for markdown content, processed HTML, and file metadata
 * to improve app performance and reduce processing time
 */
class CacheManager private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: CacheManager? = null
        
        fun getInstance(context: Context): CacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CacheManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        private const val MAX_MEMORY_CACHE_SIZE = 20 // Max items in memory cache
        private const val MAX_CONTENT_SIZE = 5 * 1024 * 1024 // 5MB max content size
    }
    
    /**
     * Data classes for cache entries
     */
    data class CacheEntry(
        val content: String,
        val processedHtml: String? = null,
        val lastModified: Long,
        val fileSize: Long,
        val contentHash: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    data class FileMetadata(
        val path: String,
        val name: String,
        val size: Long,
        val lastModified: Long,
        val isMarkdown: Boolean,
        val contentPreview: String? = null
    )
    
    // Memory caches using LRU eviction
    private val contentCache = LruCache<String, CacheEntry>(MAX_MEMORY_CACHE_SIZE)
    private val metadataCache = LruCache<String, FileMetadata>(100)
    private val outlineCache = LruCache<String, List<DocumentOutline.HeadingItem>>(50)
    
    // Thread-safe maps for file system cache tracking
    private val cacheRegistry = ConcurrentHashMap<String, String>()
    private val cacheDir: File by lazy {
        File(context.cacheDir, "markdown_cache").apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Get cached markdown content
     */
    fun getCachedContent(filePath: String): CacheEntry? {
        return contentCache.get(filePath)
    }
    
    /**
     * Cache markdown content with processed HTML
     */
    fun cacheContent(
        filePath: String,
        content: String,
        processedHtml: String? = null,
        lastModified: Long,
        fileSize: Long
    ) {
        if (content.length > MAX_CONTENT_SIZE) {
            return // Don't cache very large files
        }
        
        val contentHash = generateContentHash(content)
        val cacheEntry = CacheEntry(
            content = content,
            processedHtml = processedHtml,
            lastModified = lastModified,
            fileSize = fileSize,
            contentHash = contentHash
        )
        
        contentCache.put(filePath, cacheEntry)
        
        // Also save to disk cache for persistence
        saveToDiskCache(filePath, cacheEntry)
    }
    
    /**
     * Check if cached content is still valid
     */
    fun isCacheValid(filePath: String, currentLastModified: Long, currentFileSize: Long): Boolean {
        val cached = getCachedContent(filePath) ?: return false
        
        return cached.lastModified == currentLastModified && 
               cached.fileSize == currentFileSize &&
               !isCacheExpired(cached)
    }
    
    /**
     * Cache file metadata
     */
    fun cacheFileMetadata(filePath: String, metadata: FileMetadata) {
        metadataCache.put(filePath, metadata)
    }
    
    /**
     * Get cached file metadata
     */
    fun getCachedFileMetadata(filePath: String): FileMetadata? {
        return metadataCache.get(filePath)
    }
    
    /**
     * Cache document outline for quick access
     */
    fun cacheOutline(filePath: String, outline: List<DocumentOutline.HeadingItem>) {
        outlineCache.put(filePath, outline)
    }
    
    /**
     * Get cached document outline
     */
    fun getCachedOutline(filePath: String): List<DocumentOutline.HeadingItem>? {
        return outlineCache.get(filePath)
    }
    
    /**
     * Preload content asynchronously for better performance
     */
    fun preloadContent(filePaths: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            filePaths.forEach { filePath ->
                if (contentCache.get(filePath) == null) {
                    try {
                        val file = File(filePath)
                        if (file.exists() && file.canRead() && file.length() < MAX_CONTENT_SIZE) {
                            val content = file.readText()
                            val metadata = FileMetadata(
                                path = filePath,
                                name = file.name,
                                size = file.length(),
                                lastModified = file.lastModified(),
                                isMarkdown = isMarkdownFile(file.name),
                                contentPreview = content.take(200)
                            )
                            
                            withContext(Dispatchers.Main) {
                                cacheContent(filePath, content, null, file.lastModified(), file.length())
                                cacheFileMetadata(filePath, metadata)
                            }
                        }
                    } catch (e: Exception) {
                        // Silently fail for preloading
                    }
                }
            }
        }
    }
    
    /**
     * Clear memory caches
     */
    fun clearMemoryCache() {
        contentCache.evictAll()
        metadataCache.evictAll()
        outlineCache.evictAll()
    }
    
    /**
     * Clear all caches including disk cache
     */
    fun clearAllCache() {
        clearMemoryCache()
        clearDiskCache()
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "contentCacheSize" to contentCache.size(),
            "metadataCacheSize" to metadataCache.size(),
            "outlineCacheSize" to outlineCache.size(),
            "diskCacheFiles" to (cacheDir.listFiles()?.size ?: 0),
            "diskCacheSize" to calculateDiskCacheSize()
        )
    }
    
    /**
     * Clean up expired cache entries
     */
    fun cleanupExpiredEntries() {
        CoroutineScope(Dispatchers.IO).launch {
            val snapshot = contentCache.snapshot()
            snapshot.forEach { (key, entry) ->
                if (isCacheExpired(entry)) {
                    contentCache.remove(key)
                    deleteDiskCacheFile(key)
                }
            }
        }
    }
    
    /**
     * Private helper methods
     */
    private fun generateContentHash(content: String): String {
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(content.toByteArray()).joinToString("") { 
            "%02x".format(it) 
        }
    }
    
    private fun isCacheExpired(entry: CacheEntry): Boolean {
        val maxAge = 24 * 60 * 60 * 1000L // 24 hours
        return System.currentTimeMillis() - entry.timestamp > maxAge
    }
    
    private fun isMarkdownFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in setOf("md", "markdown", "mdown", "mkd", "mdx")
    }
    
    private fun saveToDiskCache(filePath: String, entry: CacheEntry) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cacheFileName = generateCacheFileName(filePath)
                val cacheFile = File(cacheDir, cacheFileName)
                
                val cacheData = buildString {
                    appendLine("TIMESTAMP:${entry.timestamp}")
                    appendLine("LAST_MODIFIED:${entry.lastModified}")
                    appendLine("FILE_SIZE:${entry.fileSize}")
                    appendLine("CONTENT_HASH:${entry.contentHash}")
                    appendLine("---CONTENT---")
                    append(entry.content)
                    if (entry.processedHtml != null) {
                        appendLine("\n---PROCESSED_HTML---")
                        append(entry.processedHtml)
                    }
                }
                
                cacheFile.writeText(cacheData)
                cacheRegistry[filePath] = cacheFileName
            } catch (e: Exception) {
                // Silently fail disk cache operations
            }
        }
    }
    
    private fun loadFromDiskCache(filePath: String): CacheEntry? {
        return try {
            val cacheFileName = cacheRegistry[filePath] ?: generateCacheFileName(filePath)
            val cacheFile = File(cacheDir, cacheFileName)
            
            if (!cacheFile.exists()) return null
            
            val lines = cacheFile.readLines()
            var timestamp = 0L
            var lastModified = 0L
            var fileSize = 0L
            var contentHash = ""
            var contentStartIndex = -1
            var htmlStartIndex = -1
            
            for (i in lines.indices) {
                when {
                    lines[i].startsWith("TIMESTAMP:") -> timestamp = lines[i].substringAfter(":").toLongOrNull() ?: 0L
                    lines[i].startsWith("LAST_MODIFIED:") -> lastModified = lines[i].substringAfter(":").toLongOrNull() ?: 0L
                    lines[i].startsWith("FILE_SIZE:") -> fileSize = lines[i].substringAfter(":").toLongOrNull() ?: 0L
                    lines[i].startsWith("CONTENT_HASH:") -> contentHash = lines[i].substringAfter(":")
                    lines[i] == "---CONTENT---" -> contentStartIndex = i + 1
                    lines[i] == "---PROCESSED_HTML---" -> htmlStartIndex = i + 1
                }
            }
            
            if (contentStartIndex == -1) return null
            
            val content = if (htmlStartIndex > 0) {
                lines.subList(contentStartIndex, htmlStartIndex - 1).joinToString("\n")
            } else {
                lines.subList(contentStartIndex, lines.size).joinToString("\n")
            }
            
            val processedHtml = if (htmlStartIndex > 0) {
                lines.subList(htmlStartIndex, lines.size).joinToString("\n")
            } else null
            
            CacheEntry(
                content = content,
                processedHtml = processedHtml,
                lastModified = lastModified,
                fileSize = fileSize,
                contentHash = contentHash,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun generateCacheFileName(filePath: String): String {
        val hash = generateContentHash(filePath)
        return "cache_$hash.txt"
    }
    
    private fun clearDiskCache() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                cacheDir.listFiles()?.forEach { it.delete() }
                cacheRegistry.clear()
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
    
    private fun deleteDiskCacheFile(filePath: String) {
        val cacheFileName = cacheRegistry.remove(filePath)
        if (cacheFileName != null) {
            try {
                File(cacheDir, cacheFileName).delete()
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
    
    private fun calculateDiskCacheSize(): Long {
        return try {
            cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}