/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.book_info

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.R
import ua.acclorite.book_story.core.helpers.calculateProgress
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.presentation.book_info.BookInfoEvent
import ua.acclorite.book_story.ui.common.components.common.StyledText

@Composable
fun BookInfoLayoutButton(
    book: Book,
    navigateToReader: (BookInfoEvent.OnNavigateToReader) -> Unit,
    toggleMarkComplete: (BookInfoEvent.OnToggleMarkComplete) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            onClick = {
                navigateToReader(BookInfoEvent.OnNavigateToReader)
            }
        ) {
            StyledText(
                text = if (book.progress == 0f) stringResource(id = R.string.start_reading)
                else stringResource(
                    id = R.string.continue_reading_query,
                    "${book.progress.calculateProgress(1)}%"
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            onClick = {
                toggleMarkComplete(BookInfoEvent.OnToggleMarkComplete)
            }
        ) {
            Icon(
                imageVector = if (book.progress >= 1f) Icons.Outlined.Close else Icons.Outlined.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            StyledText(
                text = if (book.progress >= 1f) stringResource(id = R.string.mark_as_incomplete)
                else stringResource(id = R.string.mark_as_completed)
            )
        }
    }
}