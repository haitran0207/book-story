/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.cache.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BookCacheContainerDto(
    @SerialName("book_id") val bookId: Int,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("items") val items: List<ReaderTextItemDto>
)

@Serializable
sealed class ReaderTextItemDto {

    @Serializable
    @SerialName("chapter")
    data class ChapterDto(
        @SerialName("id") val id: String,
        @SerialName("title") val title: String,
        @SerialName("nested") val nested: Boolean
    ) : ReaderTextItemDto()

    @Serializable
    @SerialName("text")
    data class TextDto(
        @SerialName("text") val text: String,
        @SerialName("spans") val spans: List<SpanDto> = emptyList()
    ) : ReaderTextItemDto()

    @Serializable
    @SerialName("separator")
    data object SeparatorDto : ReaderTextItemDto()

    @Serializable
    @SerialName("image")
    data class ImageDto(
        @SerialName("image_path") val imageFileName: String,
        @SerialName("description") val description: String? = null
    ) : ReaderTextItemDto()

    @Serializable
    @SerialName("formula")
    data class FormulaDto(
        @SerialName("image_path") val imageFileName: String,
        @SerialName("latex") val latex: String = ""
    ) : ReaderTextItemDto()
}

@Serializable
data class SpanDto(
    @SerialName("start") val start: Int,
    @SerialName("end") val end: Int,
    @SerialName("is_bold") val isBold: Boolean = false,
    @SerialName("is_italic") val isItalic: Boolean = false,
    @SerialName("is_underline") val isUnderline: Boolean = false,
    @SerialName("link_url") val linkUrl: String? = null
)
