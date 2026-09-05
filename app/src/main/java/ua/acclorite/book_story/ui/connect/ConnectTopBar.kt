/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.connect

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ua.acclorite.book_story.R
import ua.acclorite.book_story.presentation.connect.ConnectEvent
import ua.acclorite.book_story.ui.common.components.common.IconButton
import ua.acclorite.book_story.ui.common.components.common.StyledText
import ua.acclorite.book_story.ui.common.components.top_bar.TopAppBar
import ua.acclorite.book_story.ui.common.components.top_bar.TopAppBarData
import ua.acclorite.book_story.ui.navigator.NavigatorIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectTopBar(
    isLoggedIn: Boolean,
    isRefreshing: Boolean,
    onEvent: (ConnectEvent) -> Unit
) {
    TopAppBar(
        scrollBehavior = null,
        isTopBarScrolled = false,
        shownTopBar = 0,
        topBars = listOf(
            TopAppBarData(
                contentID = 0,
                contentNavigationIcon = {},
                contentTitle = {
                    StyledText(stringResource(id = R.string.connect_title))
                },
                contentActions = {
                    if (isLoggedIn) {
                        IconButton(
                            icon = Icons.Outlined.Refresh,
                            contentDescription = R.string.connect_remote_catalog,
                            disableOnClick = false,
                            enabled = !isRefreshing
                        ) {
                            onEvent(ConnectEvent.OnFetchRemoteBooks)
                        }
                    }
                    NavigatorIconButton()
                }
            )
        )
    )
}
