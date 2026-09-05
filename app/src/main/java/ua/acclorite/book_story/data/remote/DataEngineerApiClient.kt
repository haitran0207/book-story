/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.acclorite.book_story.core.log.logE
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.data.remote.model.AuthResponseDto
import ua.acclorite.book_story.data.remote.model.AuthenticationDto
import ua.acclorite.book_story.data.remote.model.DownloadBookRequest
import ua.acclorite.book_story.data.remote.model.GetAllBooksRequest
import ua.acclorite.book_story.data.remote.model.LoginRequest
import ua.acclorite.book_story.data.remote.model.RegisterRequest
import ua.acclorite.book_story.data.remote.model.RemoteBookDto
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DataEngineerApiClient"

@Singleton
class DataEngineerApiClient @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private fun normalizeUrl(baseUrl: String, endpoint: String): String {
        val cleanBase = baseUrl.trim().trimEnd('/')
        val cleanEndpoint = endpoint.trim().trimStart('/')
        return "$cleanBase/$cleanEndpoint"
    }

    suspend fun pingServer(baseUrl: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val urlStr = normalizeUrl(baseUrl, "auth-api/login")
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.outputStream.use { os ->
                os.write("{}".toByteArray())
            }
            val responseCode = connection.responseCode
            logI(TAG, "Ping $urlStr returned code: $responseCode")
            responseCode in 200..499
        }
    }

    suspend fun login(
        baseUrl: String,
        req: LoginRequest
    ): Result<AuthenticationDto> = withContext(Dispatchers.IO) {
        runCatching {
            val urlStr = normalizeUrl(baseUrl, "auth-api/login")
            logI(TAG, "Logging in to $urlStr for ${req.usernameOrEmail}")

            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true

            val bodyJson = json.encodeToString(req)
            connection.outputStream.use { os ->
                os.write(bodyJson.toByteArray())
            }

            val responseCode = connection.responseCode
            val stream: InputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: throw Exception("HTTP error $responseCode")
            }

            val responseBody = stream.bufferedReader().use { it.readText() }
            logI(TAG, "Login response [$responseCode]: $responseBody")

            if (responseCode in 200..299) {
                val parsed = json.decodeFromString<AuthResponseDto<AuthenticationDto>>(responseBody)
                parsed.data ?: throw Exception(parsed.message ?: "Authentication failed (null data)")
            } else {
                val errorMsg = runCatching {
                    val parsedErr = json.decodeFromString<AuthResponseDto<AuthenticationDto>>(responseBody)
                    parsedErr.message
                }.getOrNull() ?: "Login failed ($responseCode)"
                throw Exception(errorMsg)
            }
        }
    }

    suspend fun register(
        baseUrl: String,
        req: RegisterRequest
    ): Result<AuthenticationDto?> = withContext(Dispatchers.IO) {
        runCatching {
            val urlStr = normalizeUrl(baseUrl, "auth-api/register")
            logI(TAG, "Registering to $urlStr for ${req.username}")

            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true

            val bodyJson = json.encodeToString(req)
            connection.outputStream.use { os ->
                os.write(bodyJson.toByteArray())
            }

            val responseCode = connection.responseCode
            val stream: InputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: throw Exception("HTTP error $responseCode")
            }

            val responseBody = stream.bufferedReader().use { it.readText() }
            logI(TAG, "Register response [$responseCode]: $responseBody")

            if (responseCode in 200..299) {
                val parsed = json.decodeFromString<AuthResponseDto<AuthenticationDto>>(responseBody)
                parsed.data
            } else {
                val errorMsg = runCatching {
                    val parsedErr = json.decodeFromString<AuthResponseDto<AuthenticationDto>>(responseBody)
                    parsedErr.message
                }.getOrNull() ?: "Registration failed ($responseCode)"
                throw Exception(errorMsg)
            }
        }
    }

    suspend fun getAllBooks(
        baseUrl: String,
        token: String?,
        userId: Long?
    ): Result<List<RemoteBookDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val urlStr = normalizeUrl(baseUrl, "book-management/get-all-books")
            logI(TAG, "Fetching all books from $urlStr")

            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 12000
            connection.readTimeout = 12000
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) {
                val cleanToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
                connection.setRequestProperty("Authorization", cleanToken)
            }
            connection.doOutput = true

            val bodyJson = json.encodeToString(GetAllBooksRequest(userId = userId))
            connection.outputStream.use { os ->
                os.write(bodyJson.toByteArray())
            }

            val responseCode = connection.responseCode
            val stream: InputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: throw Exception("HTTP error $responseCode")
            }

            val responseBody = stream.bufferedReader().use { it.readText() }
            logI(TAG, "Get all books response [$responseCode]: ${responseBody.take(200)}...")

            if (responseCode in 200..299) {
                json.decodeFromString<List<RemoteBookDto>>(responseBody)
            } else {
                throw Exception("Failed to load books from server ($responseCode)")
            }
        }
    }

    suspend fun downloadBook(
        baseUrl: String,
        token: String?,
        bookId: Long,
        destinationFile: File,
        onProgress: ((Float) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val urlStr = normalizeUrl(baseUrl, "book-management/download-book")
            logI(TAG, "Downloading book $bookId from $urlStr to ${destinationFile.absolutePath}")

            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 60000
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            if (!token.isNullOrBlank()) {
                val cleanToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
                connection.setRequestProperty("Authorization", cleanToken)
            }
            connection.doOutput = true

            val bodyJson = json.encodeToString(DownloadBookRequest(bookId = bookId))
            connection.outputStream.use { os ->
                os.write(bodyJson.toByteArray())
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                logE(TAG, "Download error $responseCode: $err")
                throw Exception("Failed to download book (HTTP $responseCode)")
            }

            val contentLength = connection.contentLengthLong
            val inputStream = connection.inputStream

            destinationFile.parentFile?.mkdirs()
            val outputStream: OutputStream = FileOutputStream(destinationFile)

            var bytesRead = 0L
            val buffer = ByteArray(8192)
            var read: Int

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read
                        if (contentLength > 0) {
                            val progress = bytesRead.toFloat() / contentLength.toFloat()
                            onProgress?.invoke(progress)
                        }
                    }
                }
            }

            logI(TAG, "Download completed: ${destinationFile.length()} bytes saved.")
            destinationFile
        }
    }
}
