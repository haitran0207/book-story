/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val usernameOrEmail: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val teamCode: String = "BOOKAPPCUS"
)

@Serializable
data class UserInfoDto(
    val userId: Long? = null,
    val username: String? = null,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)

@Serializable
data class AuthenticationDto(
    val token: String? = null,
    val tokenType: String? = null,
    val expiresIn: Long? = null,
    val user: UserInfoDto? = null
)

@Serializable
data class AuthResponseDto<T>(
    val status: String? = null,
    val message: String? = null,
    val data: T? = null,
    val statusCode: Int? = null
)

@Serializable
data class GetAllBooksRequest(
    val userId: Long? = null
)

@Serializable
data class DownloadBookRequest(
    val bookId: Long
)

@Serializable
data class RemoteBookDto(
    val bookId: Long,
    val title: String,
    val author: String? = null,
    val publisher: String? = null,
    val publishedDate: String? = null,
    val language: String? = null,
    val isbn: String? = null,
    val description: String? = null,
    val coverImage: String? = null,
    val totalChapters: Int? = null,
    val fileName: String? = null,
    val filePath: String? = null
)
