/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ua.acclorite.book_story.core.log.logE
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.data.cache.dto.BookCacheContainerDto
import ua.acclorite.book_story.data.cache.dto.ReaderTextItemDto
import ua.acclorite.book_story.data.cache.dto.SpanDto
import ua.acclorite.book_story.data.settings.SettingsManager
import ua.acclorite.book_story.domain.model.reader.ReaderText
import ua.acclorite.book_story.domain.model.settings.CacheLocation
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BookTextCacheManager"
private const val CACHE_DIR_NAME = "book_text_cache"
private const val IMAGES_DIR_NAME = "images"

@Singleton
class BookTextCacheManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // L1 Cache: In-memory LRU Cache (max 3 parsed books in memory)
    private val inMemoryCache = LruCache<Int, List<ReaderText>>(3)

    fun isSdCardAvailable(): Boolean {
        return try {
            val externalDirs = context.externalCacheDirs
            externalDirs.size > 1 && externalDirs[1] != null && externalDirs[1].canWrite()
        } catch (e: Exception) {
            false
        }
    }

    fun getCacheDir(location: CacheLocation? = null): File {
        val targetLocation = location ?: settingsManager.cacheLocation.lastValue
        if (targetLocation == CacheLocation.SD_CARD) {
            val customPath = settingsManager.customCachePath.lastValue
            if (customPath.isNotBlank()) {
                val customDir = File(customPath, CACHE_DIR_NAME)
                try {
                    if ((customDir.exists() || customDir.mkdirs()) && customDir.canWrite()) {
                        return customDir
                    }
                } catch (e: Exception) {
                    logE(TAG, "Could not access custom cache directory [$customPath]: ${e.message}")
                }
            }
            if (isSdCardAvailable()) {
                val sdDir = File(context.externalCacheDirs[1], CACHE_DIR_NAME)
                if ((sdDir.exists() || sdDir.mkdirs()) && sdDir.canWrite()) {
                    return sdDir
                }
            }
        }
        return File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    private fun getBookCacheFile(bookId: Int): File {
        return File(getCacheDir(), "book_$bookId.json.gz")
    }

    private fun getBookImagesDir(bookId: Int): File {
        return File(getCacheDir(), "$IMAGES_DIR_NAME/$bookId").apply {
            if (!exists()) mkdirs()
        }
    }

    suspend fun moveCache(
        targetLocation: CacheLocation,
        customPath: String? = null,
        customUri: String? = null
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val currentLocation = settingsManager.cacheLocation.lastValue
                val sourceDir = getCacheDir(currentLocation)

                val destDir = if (customPath != null) {
                    File(customPath, CACHE_DIR_NAME).apply { if (!exists()) mkdirs() }
                } else {
                    getCacheDir(targetLocation)
                }

                if (sourceDir.absolutePath == destDir.absolutePath) {
                    if (customPath != null) settingsManager.customCachePath.update(customPath)
                    if (customUri != null) settingsManager.customCacheUri.update(customUri)
                    settingsManager.cacheLocation.update(targetLocation)
                    return@withContext true
                }

                logI(TAG, "Moving cache from ${sourceDir.absolutePath} to ${destDir.absolutePath}")

                if (sourceDir.exists()) {
                    sourceDir.copyRecursively(destDir, overwrite = true)
                    sourceDir.deleteRecursively()
                }

                inMemoryCache.evictAll()
                if (customPath != null) settingsManager.customCachePath.update(customPath)
                if (customUri != null) settingsManager.customCacheUri.update(customUri)
                settingsManager.cacheLocation.update(targetLocation)
                logI(TAG, "Successfully moved cache to $targetLocation (${destDir.absolutePath})")
                true
            } catch (e: Exception) {
                logE(TAG, "Failed to move cache: ${e.message}")
                false
            }
        }
    }

    /**
     * Gets cached [List<ReaderText>] for a book.
     * Returns null if cache is missing, corrupted, or stale ([lastModified] mismatch).
     */
    suspend fun get(bookId: Int, lastModified: Long): List<ReaderText>? {
        // 1. Check L1 Memory Cache
        inMemoryCache.get(bookId)?.let {
            logI(TAG, "L1 Cache hit for book [$bookId].")
            return it
        }

        // 2. Check L2 Disk Cache
        return withContext(Dispatchers.IO) {
            try {
                val file = getBookCacheFile(bookId)
                if (!file.exists() || !file.canRead()) return@withContext null

                val jsonContent = GZIPInputStream(FileInputStream(file)).bufferedReader().use {
                    it.readText()
                }

                val container = json.decodeFromString<BookCacheContainerDto>(jsonContent)

                // Cache freshness validation
                if (container.lastModified != lastModified && lastModified > 0L) {
                    logI(TAG, "L2 Cache stale for book [$bookId] (modified timestamp mismatch). Evicting.")
                    evict(bookId)
                    return@withContext null
                }

                val imagesDir = getBookImagesDir(bookId)
                val readerTextList = container.items.mapNotNull { item ->
                    item.toDomainModel(imagesDir)
                }

                if (readerTextList.isEmpty()) {
                    return@withContext null
                }

                logI(TAG, "L2 Cache hit for book [$bookId] (${readerTextList.size} items loaded).")
                inMemoryCache.put(bookId, readerTextList)
                readerTextList
            } catch (e: Exception) {
                logE(TAG, "Failed to read L2 disk cache for book [$bookId]: ${e.message}")
                evict(bookId)
                null
            }
        }
    }

    /**
     * Stores [List<ReaderText>] into L1 and L2 cache.
     */
    suspend fun put(bookId: Int, lastModified: Long, textList: List<ReaderText>) {
        if (textList.isEmpty()) return

        // 1. Save to L1 Memory Cache
        inMemoryCache.put(bookId, textList)

        // 2. Save to L2 Disk Cache
        withContext(Dispatchers.IO) {
            try {
                val imagesDir = getBookImagesDir(bookId)
                var imageCounter = 0

                val dtos = textList.map { item ->
                    item.toDto(imagesDir, imageCounter++)
                }

                val container = BookCacheContainerDto(
                    bookId = bookId,
                    lastModified = lastModified,
                    items = dtos
                )

                val jsonContent = json.encodeToString(BookCacheContainerDto.serializer(), container)
                val file = getBookCacheFile(bookId)

                GZIPOutputStream(FileOutputStream(file)).bufferedWriter().use {
                    it.write(jsonContent)
                }

                logI(TAG, "Successfully saved L2 disk cache for book [$bookId].")
            } catch (e: Exception) {
                logE(TAG, "Failed to write L2 disk cache for book [$bookId]: ${e.message}")
            }
        }
    }

    /**
     * Evicts cache for [bookId] from memory and disk.
     */
    suspend fun evict(bookId: Int) {
        inMemoryCache.remove(bookId)
        withContext(Dispatchers.IO) {
            try {
                val file = getBookCacheFile(bookId)
                if (file.exists()) file.delete()

                val imagesDir = getBookImagesDir(bookId)
                if (imagesDir.exists()) imagesDir.deleteRecursively()

                logI(TAG, "Evicted cache for book [$bookId].")
            } catch (e: Exception) {
                logE(TAG, "Failed to evict cache for book [$bookId]: ${e.message}")
            }
        }
    }

    /**
     * Clears all cached books.
     */
    suspend fun clearAll() {
        inMemoryCache.evictAll()
        withContext(Dispatchers.IO) {
            try {
                getCacheDir().deleteRecursively()
                logI(TAG, "Cleared all book text caches.")
            } catch (e: Exception) {
                logE(TAG, "Failed to clear all caches: ${e.message}")
            }
        }
    }

    // --- Domain <-> DTO Mapping ---

    private fun ReaderText.toDto(imagesDir: File, index: Int): ReaderTextItemDto {
        return when (this) {
            is ReaderText.Chapter -> ReaderTextItemDto.ChapterDto(
                id = id.toString(),
                title = title,
                nested = nested
            )

            is ReaderText.Text -> line.toDto()

            is ReaderText.Separator -> ReaderTextItemDto.SeparatorDto

            is ReaderText.Image -> {
                val fileName = "img_$index.png"
                saveImageBitmap(imageBitmap, File(imagesDir, fileName))
                ReaderTextItemDto.ImageDto(
                    imageFileName = fileName,
                    description = description
                )
            }

            is ReaderText.Formula -> {
                val fileName = "formula_$index.png"
                saveImageBitmap(imageBitmap, File(imagesDir, fileName))
                ReaderTextItemDto.FormulaDto(
                    imageFileName = fileName,
                    latex = latex
                )
            }
        }
    }

    private fun ReaderTextItemDto.toDomainModel(imagesDir: File): ReaderText? {
        return when (this) {
            is ReaderTextItemDto.ChapterDto -> ReaderText.Chapter(
                id = try { UUID.fromString(id) } catch (e: Exception) { UUID.randomUUID() },
                title = title,
                nested = nested
            )

            is ReaderTextItemDto.TextDto -> ReaderText.Text(
                line = toAnnotatedString()
            )

            is ReaderTextItemDto.SeparatorDto -> ReaderText.Separator

            is ReaderTextItemDto.ImageDto -> {
                val imageFile = File(imagesDir, imageFileName)
                val bitmap = loadImageBitmap(imageFile) ?: return null
                ReaderText.Image(
                    imageBitmap = bitmap,
                    description = description
                )
            }

            is ReaderTextItemDto.FormulaDto -> {
                val imageFile = File(imagesDir, imageFileName)
                val bitmap = loadImageBitmap(imageFile) ?: return null
                ReaderText.Formula(
                    imageBitmap = bitmap,
                    latex = latex
                )
            }
        }
    }

    private fun AnnotatedString.toDto(): ReaderTextItemDto.TextDto {
        val spans = mutableListOf<SpanDto>()
        spanStyles.forEach { range ->
            val style = range.item
            val isBold = style.fontWeight != null && style.fontWeight!! >= FontWeight.Bold
            val isItalic = style.fontStyle == FontStyle.Italic
            val isUnderline = style.textDecoration == TextDecoration.Underline
            if (isBold || isItalic || isUnderline) {
                spans.add(
                    SpanDto(
                        start = range.start,
                        end = range.end,
                        isBold = isBold,
                        isItalic = isItalic,
                        isUnderline = isUnderline
                    )
                )
            }
        }
        getLinkAnnotations(0, length).forEach { range ->
            val link = range.item
            if (link is LinkAnnotation.Url) {
                spans.add(
                    SpanDto(
                        start = range.start,
                        end = range.end,
                        linkUrl = link.url
                    )
                )
            }
        }
        return ReaderTextItemDto.TextDto(text = text, spans = spans)
    }

    private fun ReaderTextItemDto.TextDto.toAnnotatedString(): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            spans.forEach { span ->
                val style = SpanStyle(
                    fontWeight = if (span.isBold) FontWeight.Medium else null,
                    fontStyle = if (span.isItalic) FontStyle.Italic else null,
                    textDecoration = if (span.isUnderline) TextDecoration.Underline else null
                )
                val safeStart = span.start.coerceIn(0, text.length)
                val safeEnd = span.end.coerceIn(0, text.length)
                if (safeStart < safeEnd) {
                    addStyle(style, safeStart, safeEnd)
                    if (!span.linkUrl.isNullOrEmpty()) {
                        addLink(
                            LinkAnnotation.Url(
                                url = span.linkUrl,
                                styles = TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.Underline))
                            ),
                            safeStart,
                            safeEnd
                        )
                    }
                }
            }
        }
    }

    private fun saveImageBitmap(imageBitmap: ImageBitmap, targetFile: File) {
        try {
            val bitmap = imageBitmap.asAndroidBitmap()
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            logE(TAG, "Failed to save image bitmap to file [${targetFile.name}]: ${e.message}")
        }
    }

    private fun loadImageBitmap(sourceFile: File): ImageBitmap? {
        if (!sourceFile.exists() || !sourceFile.canRead()) return null
        return try {
            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            logE(TAG, "Failed to load image bitmap from file [${sourceFile.name}]: ${e.message}")
            null
        }
    }
}
