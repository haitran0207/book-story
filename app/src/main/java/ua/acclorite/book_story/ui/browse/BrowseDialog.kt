/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.browse

import androidx.compose.runtime.Composable
import ua.acclorite.book_story.core.Dialog
import ua.acclorite.book_story.presentation.browse.BrowseEvent
import ua.acclorite.book_story.presentation.browse.BrowseScreen
import ua.acclorite.book_story.presentation.browse.model.SelectableNullableBook

@Composable
fun BrowseDialog(
    dialog: Dialog?,
    loadingAddDialog: Boolean,
    isAddingBooks: Boolean = false,
    addingBooksProgress: Pair<Int, Int>? = null,
    selectedBooksAddDialog: List<SelectableNullableBook>,
    dismissAddDialog: (BrowseEvent.OnDismissAddDialog) -> Unit,
    actionAddDialog: (BrowseEvent.OnActionAddDialog) -> Unit,
    selectAddDialog: (BrowseEvent.OnSelectAddDialog) -> Unit
) {
    when (dialog) {
        BrowseScreen.ADD_DIALOG -> {
            BrowseAddDialog(
                loadingAddDialog = loadingAddDialog,
                isAddingBooks = isAddingBooks,
                addingBooksProgress = addingBooksProgress,
                selectedBooksAddDialog = selectedBooksAddDialog,
                dismissAddDialog = dismissAddDialog,
                actionAddDialog = actionAddDialog,
                selectAddDialog = selectAddDialog
            )
        }
    }
}