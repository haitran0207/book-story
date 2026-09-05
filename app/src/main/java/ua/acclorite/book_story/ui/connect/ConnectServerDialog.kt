/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.connect

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.R
import ua.acclorite.book_story.presentation.connect.ConnectEvent
import ua.acclorite.book_story.presentation.connect.ConnectState
import ua.acclorite.book_story.presentation.connect.ServerStatus
import ua.acclorite.book_story.ui.common.components.common.StyledText
import ua.acclorite.book_story.ui.common.components.dialog.Dialog

@Composable
fun ConnectServerDialog(
    state: ConnectState,
    onEvent: (ConnectEvent) -> Unit
) {
    if (!state.showServerDialog) return

    Dialog(
        icon = Icons.Outlined.Dns,
        title = stringResource(id = R.string.connect_server_settings),
        description = stringResource(id = R.string.connect_subtitle),
        actionEnabled = state.tempServerUrlInput.isNotBlank(),
        disableOnClick = false,
        onDismiss = { onEvent(ConnectEvent.OnShowServerDialog(false)) },
        onAction = { onEvent(ConnectEvent.OnSaveServerUrl) },
        secondaryAction = "Test",
        onSecondaryAction = { onEvent(ConnectEvent.OnTestConnection) },
        withContent = true,
        items = {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    OutlinedTextField(
                        value = state.tempServerUrlInput,
                        onValueChange = { onEvent(ConnectEvent.OnTempServerUrlChange(it)) },
                        label = { StyledText(stringResource(id = R.string.connect_server_url)) },
                        placeholder = { StyledText(stringResource(id = R.string.connect_server_url_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.serverStatus != null || state.serverStatusMessage != null) {
                        val statusColor = when (state.serverStatus) {
                            ServerStatus.CONNECTED -> Color(0xFF00C853)
                            ServerStatus.DISCONNECTED -> MaterialTheme.colorScheme.error
                            ServerStatus.TESTING -> MaterialTheme.colorScheme.primary
                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        StyledText(
                            text = state.serverStatusMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = statusColor),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    )
}
