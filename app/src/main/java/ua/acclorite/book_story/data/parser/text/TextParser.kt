/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.text

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ua.acclorite.book_story.data.model.file.CachedFile
import ua.acclorite.book_story.domain.model.reader.ParseChunk
import ua.acclorite.book_story.domain.model.reader.ReaderText

interface TextParser {
    suspend fun parse(cachedFile: CachedFile): List<ReaderText>

    fun parseProgressive(cachedFile: CachedFile): Flow<ParseChunk> {
        return flow {
            val text = parse(cachedFile)
            emit(
                ParseChunk(
                    items = text,
                    isFirstChunk = true,
                    isLastChunk = true,
                    totalChaptersEstimated = 1,
                    currentChapterParsed = 1
                )
            )
        }
    }
}