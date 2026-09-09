/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.R
import ua.acclorite.book_story.core.log.logE
import ua.acclorite.book_story.core.ui.UIText
import ua.acclorite.book_story.data.model.file.CachedFile
import ua.acclorite.book_story.data.parser.mobi.MobiParserHelper
import ua.acclorite.book_story.domain.model.library.Book
import javax.inject.Inject

private const val TAG = "MobiFileParser"

class MobiFileParser @Inject constructor() : FileParser {

    override suspend fun parse(cachedFile: CachedFile): Book? {
        return try {
            val rawFile = cachedFile.rawFile
            if (rawFile == null || !rawFile.exists() || !rawFile.canRead()) return null

            withContext(Dispatchers.IO) {
                val bookInfo = MobiParserHelper.parseMobiHeader(rawFile) ?: return@withContext null

                val title = bookInfo.title.ifBlank {
                    cachedFile.name.substringBeforeLast(".").trim()
                }

                val author = if (!bookInfo.author.isNullOrBlank()) {
                    UIText.StringValue(bookInfo.author)
                } else {
                    UIText.StringResource(R.string.unknown_author)
                }

                Book(
                    title = title,
                    author = author,
                    description = bookInfo.description,
                    scrollIndex = 0,
                    scrollOffset = 0,
                    progress = 0f,
                    filePath = cachedFile.path,
                    lastOpened = null,
                    categories = emptyList(),
                    tags = bookInfo.tags,
                    coverImage = null
                )
            }
        } catch (e: Exception) {
            logE(TAG, "Could not parse MOBI file: ${e.message}")
            null
        }
    }
}
