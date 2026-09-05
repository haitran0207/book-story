/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.connect

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import ua.acclorite.book_story.presentation.connect.ConnectEffect

@Composable
fun ConnectEffects(
    effects: SharedFlow<ConnectEffect>
) {
    val context = LocalContext.current

    LaunchedEffect(effects) {
        effects.collectLatest { effect ->
            when (effect) {
                is ConnectEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
