/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.model.reader

import androidx.compose.runtime.Immutable

@Immutable
data class ParseChunk(
    val items: List<ReaderText>,
    val isFirstChunk: Boolean,
    val isLastChunk: Boolean,
    val totalChaptersEstimated: Int = 0,
    val currentChapterParsed: Int = 0
)
