/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.library.model

import androidx.annotation.StringRes
import ua.acclorite.book_story.R

enum class AllBooksFilter(@StringRes val titleRes: Int) {
    ALL(R.string.filter_all),
    NOT_STARTED(R.string.filter_not_start),
    PROCESSING(R.string.filter_processing),
    COMPLETED(R.string.filter_completed)
}
