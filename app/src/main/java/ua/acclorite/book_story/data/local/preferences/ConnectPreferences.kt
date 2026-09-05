/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.local.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("connect_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_FIRST_NAME = "first_name"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"

        // Default to local development server IP
        const val DEFAULT_SERVER_URL = "http://172.20.2.76:8082/data-api"
    }

    private val _serverUrlFlow = MutableStateFlow(getServerUrl())
    val serverUrlFlow: StateFlow<String> = _serverUrlFlow.asStateFlow()

    private val _isLoggedInFlow = MutableStateFlow(isLoggedIn())
    val isLoggedInFlow: StateFlow<Boolean> = _isLoggedInFlow.asStateFlow()

    fun getServerUrl(): String {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    fun setServerUrl(url: String) {
        val clean = url.trim().trimEnd('/')
        prefs.edit().putString(KEY_SERVER_URL, clean).apply()
        _serverUrlFlow.value = clean
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun getUserId(): Long? {
        val id = prefs.getLong(KEY_USER_ID, -1L)
        return if (id != -1L) id else null
    }

    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    fun getEmail(): String? {
        return prefs.getString(KEY_EMAIL, null)
    }

    fun getFullName(): String {
        val first = prefs.getString(KEY_FIRST_NAME, "") ?: ""
        val last = prefs.getString(KEY_LAST_NAME, "") ?: ""
        val combined = "$first $last".trim()
        return combined.ifEmpty { getUsername() ?: "User" }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && !getAuthToken().isNullOrBlank()
    }

    fun saveSession(
        token: String,
        userId: Long?,
        username: String?,
        email: String?,
        firstName: String?,
        lastName: String?
    ) {
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .putLong(KEY_USER_ID, userId ?: -1L)
            .putString(KEY_USERNAME, username)
            .putString(KEY_EMAIL, email)
            .putString(KEY_FIRST_NAME, firstName)
            .putString(KEY_LAST_NAME, lastName)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
        _isLoggedInFlow.value = true
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_EMAIL)
            .remove(KEY_FIRST_NAME)
            .remove(KEY_LAST_NAME)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
        _isLoggedInFlow.value = false
    }
}
