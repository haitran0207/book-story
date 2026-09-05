/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.connect

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.parcelize.Parcelize
import ua.acclorite.book_story.presentation.navigator.Screen
import ua.acclorite.book_story.ui.connect.ConnectContent
import ua.acclorite.book_story.ui.connect.ConnectEffects

@Parcelize
object ConnectScreen : Screen, Parcelable {

    @Composable
    override fun Content() {
        val screenModel = hiltViewModel<ConnectModel>()
        val state = screenModel.state.collectAsStateWithLifecycle()

        ConnectEffects(
            effects = screenModel.effects
        )

        ConnectContent(
            state = state.value,
            onEvent = screenModel::onEvent
        )
    }
}
