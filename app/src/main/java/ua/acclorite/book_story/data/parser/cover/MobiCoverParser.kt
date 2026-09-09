/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.cover

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.core.CoverImage
import ua.acclorite.book_story.core.log.logE
import ua.acclorite.book_story.data.model.file.CachedFile
import ua.acclorite.book_story.data.parser.mobi.MobiParserHelper
import javax.inject.Inject

private const val TAG = "MobiCoverParser"

class MobiCoverParser @Inject constructor() : CoverParser {

    override suspend fun parse(cachedFile: CachedFile): CoverImage? {
        return try {
            val rawFile = cachedFile.rawFile
            if (rawFile == null || !rawFile.exists() || !rawFile.canRead()) return null

            withContext(Dispatchers.IO) {
                val bookInfo = MobiParserHelper.parseMobiHeader(rawFile) ?: return@withContext null
                MobiParserHelper.readCoverImage(rawFile, bookInfo)
            }
        } catch (e: Exception) {
            logE(TAG, "Could not parse MOBI cover: ${e.message}")
            null
        }
    }
}
