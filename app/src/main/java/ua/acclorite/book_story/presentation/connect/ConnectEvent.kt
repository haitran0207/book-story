/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.connect

import androidx.compose.runtime.Immutable
import ua.acclorite.book_story.data.remote.model.RemoteBookDto

@Immutable
sealed class ConnectEvent {
    data class OnChangeAuthTab(val tab: AuthTab) : ConnectEvent()
    data class OnLoginUsernameChange(val value: String) : ConnectEvent()
    data class OnLoginPasswordChange(val value: String) : ConnectEvent()
    data class OnRegisterUsernameChange(val value: String) : ConnectEvent()
    data class OnRegisterEmailChange(val value: String) : ConnectEvent()
    data class OnRegisterPasswordChange(val value: String) : ConnectEvent()
    data class OnRegisterFirstNameChange(val value: String) : ConnectEvent()
    data class OnRegisterLastNameChange(val value: String) : ConnectEvent()
    
    data object OnSubmitLogin : ConnectEvent()
    data object OnSubmitRegister : ConnectEvent()
    data object OnDisconnect : ConnectEvent()
    data object OnTestConnection : ConnectEvent()
    data object OnFetchRemoteBooks : ConnectEvent()
    data class OnSearchQueryChange(val query: String) : ConnectEvent()
    data class OnDownloadBook(val book: RemoteBookDto) : ConnectEvent()
    
    data class OnShowServerDialog(val show: Boolean) : ConnectEvent()
    data class OnTempServerUrlChange(val url: String) : ConnectEvent()
    data object OnSaveServerUrl : ConnectEvent()
}
