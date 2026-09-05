/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.connect

import androidx.compose.runtime.Immutable
import ua.acclorite.book_story.BuildConfig
import ua.acclorite.book_story.data.remote.model.RemoteBookDto

enum class AuthTab {
    LOGIN,
    REGISTER
}

enum class ServerStatus {
    CONNECTED,
    DISCONNECTED,
    TESTING
}

@Immutable
data class ConnectState(
    val serverUrl: String = BuildConfig.BACKEND_BASE_URL,
    val isLoggedIn: Boolean = false,
    val username: String? = null,
    val email: String? = null,
    val fullName: String? = null,
    val userId: Long? = null,

    // Auth Form State
    val authTab: AuthTab = AuthTab.LOGIN,
    val loginUsernameOrEmail: String = "",
    val loginPassword: String = "",
    val registerUsername: String = "",
    val registerEmail: String = "",
    val registerPassword: String = "",
    val registerFirstName: String = "",
    val registerLastName: String = "",

    // UI Status
    val isLoading: Boolean = false,
    val serverStatus: ServerStatus? = null,
    val serverStatusMessage: String? = null,

    // Books Catalog
    val remoteBooks: List<RemoteBookDto> = emptyList(),
    val downloadingBookIds: Set<Long> = emptySet(),
    val downloadedBookTitles: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isRefreshingBooks: Boolean = false
)
