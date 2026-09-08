/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.local.dto

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import ua.acclorite.book_story.data.converter.CategoryConverter
import ua.acclorite.book_story.data.converter.TagsConverter

@Entity
@TypeConverters(CategoryConverter::class, TagsConverter::class)
data class BookEntity(
    @PrimaryKey(true) val id: Int = 0,
    val title: String,
    val author: String,
    val description: String?,
    val filePath: String,
    val scrollIndex: Int,
    val scrollOffset: Int,
    val progress: Float,
    val image: String? = null,
    @ColumnInfo(defaultValue = "[]") val categories: List<Int>,
    @ColumnInfo(defaultValue = "[]") val tags: List<String> = emptyList()
)