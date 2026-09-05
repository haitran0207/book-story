/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.connect

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.R
import ua.acclorite.book_story.core.log.logE
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.data.local.preferences.ConnectPreferences
import ua.acclorite.book_story.data.remote.DataEngineerApiClient
import ua.acclorite.book_story.data.remote.model.LoginRequest
import ua.acclorite.book_story.data.remote.model.RegisterRequest
import ua.acclorite.book_story.domain.model.file.File as DomainFile
import ua.acclorite.book_story.domain.use_case.book.AddBookUseCase
import ua.acclorite.book_story.domain.use_case.file_system.GetBookFromFileUseCase
import ua.acclorite.book_story.presentation.library.LibraryScreen
import java.io.File
import javax.inject.Inject

private const val TAG = "ConnectModel"

@HiltViewModel
class ConnectModel @Inject constructor(
    private val application: Application,
    private val apiClient: DataEngineerApiClient,
    private val connectPreferences: ConnectPreferences,
    private val getBookFromFileUseCase: GetBookFromFileUseCase,
    private val addBookUseCase: AddBookUseCase
) : ViewModel() {

    private val mutex = Mutex()

    private val _state = MutableStateFlow(ConnectState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ConnectEffect>()
    val effects = _effects.asSharedFlow()

    init {
        val serverUrl = connectPreferences.getServerUrl()
        val isLoggedIn = connectPreferences.isLoggedIn()
        val username = connectPreferences.getUsername()
        val email = connectPreferences.getEmail()
        val fullName = connectPreferences.getFullName()
        val userId = connectPreferences.getUserId()

        _state.update {
            it.copy(
                serverUrl = serverUrl,
                isLoggedIn = isLoggedIn,
                username = username,
                email = email,
                fullName = fullName,
                userId = userId
            )
        }

        if (isLoggedIn) {
            viewModelScope.launch {
                fetchRemoteBooks()
            }
        }
    }

    fun onEvent(event: ConnectEvent) {
        viewModelScope.launch {
            mutex.withLock {
                when (event) {
                    is ConnectEvent.OnChangeAuthTab -> {
                        _state.update { it.copy(authTab = event.tab) }
                    }

                    is ConnectEvent.OnLoginUsernameChange -> {
                        _state.update { it.copy(loginUsernameOrEmail = event.value) }
                    }

                    is ConnectEvent.OnLoginPasswordChange -> {
                        _state.update { it.copy(loginPassword = event.value) }
                    }

                    is ConnectEvent.OnRegisterUsernameChange -> {
                        _state.update { it.copy(registerUsername = event.value) }
                    }

                    is ConnectEvent.OnRegisterEmailChange -> {
                        _state.update { it.copy(registerEmail = event.value) }
                    }

                    is ConnectEvent.OnRegisterPasswordChange -> {
                        _state.update { it.copy(registerPassword = event.value) }
                    }

                    is ConnectEvent.OnRegisterFirstNameChange -> {
                        _state.update { it.copy(registerFirstName = event.value) }
                    }

                    is ConnectEvent.OnRegisterLastNameChange -> {
                        _state.update { it.copy(registerLastName = event.value) }
                    }

                    is ConnectEvent.OnSubmitLogin -> {
                        performLogin()
                    }

                    is ConnectEvent.OnSubmitRegister -> {
                        performRegister()
                    }

                    is ConnectEvent.OnDisconnect -> {
                        disconnectAccount()
                    }

                    is ConnectEvent.OnFetchRemoteBooks -> {
                        fetchRemoteBooks()
                    }

                    is ConnectEvent.OnSearchQueryChange -> {
                        _state.update { it.copy(searchQuery = event.query) }
                    }

                    is ConnectEvent.OnDownloadBook -> {
                        downloadBook(event.book)
                    }
                }
            }
        }
    }

    private suspend fun performLogin() {
        val current = _state.value
        val usernameOrEmail = current.loginUsernameOrEmail.trim()
        val password = current.loginPassword.trim()

        if (usernameOrEmail.isBlank() || password.isBlank()) {
            _effects.emit(ConnectEffect.ShowToast(application.getString(R.string.connect_error_empty_credentials)))
            return
        }

        _state.update { it.copy(isLoading = true) }

        val result = apiClient.login(
            baseUrl = current.serverUrl,
            req = LoginRequest(usernameOrEmail = usernameOrEmail, password = password)
        )

        result.fold(
            onSuccess = { authDto ->
                val token = authDto.token ?: ""
                val user = authDto.user
                val userId = user?.userId
                val username = user?.username ?: usernameOrEmail
                val email = user?.email
                val firstName = user?.firstName
                val lastName = user?.lastName

                connectPreferences.saveSession(
                    token = token,
                    userId = userId,
                    username = username,
                    email = email,
                    firstName = firstName,
                    lastName = lastName
                )

                _state.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        username = username,
                        email = email,
                        fullName = connectPreferences.getFullName(),
                        userId = userId,
                        loginPassword = "",
                        serverStatus = ServerStatus.CONNECTED,
                        serverStatusMessage = "Connected to ${it.serverUrl}"
                    )
                }

                _effects.emit(ConnectEffect.ShowToast(application.getString(R.string.connect_login_success)))
                fetchRemoteBooks()
            },
            onFailure = { err ->
                logE(TAG, "Login failed: ${err.message}")
                _state.update { it.copy(isLoading = false) }
                _effects.emit(ConnectEffect.ShowToast("Login failed: ${err.message}"))
            }
        )
    }

    private suspend fun performRegister() {
        val current = _state.value
        val username = current.registerUsername.trim()
        val email = current.registerEmail.trim()
        val password = current.registerPassword.trim()
        val firstName = current.registerFirstName.trim()
        val lastName = current.registerLastName.trim()

        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _effects.emit(ConnectEffect.ShowToast("Please enter username, email and password."))
            return
        }

        _state.update { it.copy(isLoading = true) }

        val result = apiClient.register(
            baseUrl = current.serverUrl,
            req = RegisterRequest(
                username = username,
                email = email,
                password = password,
                firstName = firstName.ifBlank { null },
                lastName = lastName.ifBlank { null },
                teamCode = "BOOKAPPCUS"
            )
        )

        result.fold(
            onSuccess = {
                _state.update {
                    it.copy(
                        isLoading = false,
                        authTab = AuthTab.LOGIN,
                        loginUsernameOrEmail = username,
                        registerPassword = ""
                    )
                }
                _effects.emit(ConnectEffect.ShowToast(application.getString(R.string.connect_register_success)))
            },
            onFailure = { err ->
                logE(TAG, "Register failed: ${err.message}")
                _state.update { it.copy(isLoading = false) }
                _effects.emit(ConnectEffect.ShowToast("Registration error: ${err.message}"))
            }
        )
    }

    private fun disconnectAccount() {
        connectPreferences.clearSession()
        _state.update {
            it.copy(
                isLoggedIn = false,
                username = null,
                email = null,
                fullName = null,
                userId = null,
                remoteBooks = emptyList(),
                downloadingBookIds = emptySet()
            )
        }
        viewModelScope.launch {
            _effects.emit(ConnectEffect.ShowToast(application.getString(R.string.connect_logout_success)))
        }
    }

    private suspend fun fetchRemoteBooks() {
        val serverUrl = _state.value.serverUrl
        val token = connectPreferences.getAuthToken()
        val userId = connectPreferences.getUserId()

        _state.update { it.copy(isRefreshingBooks = true) }

        val result = apiClient.getAllBooks(
            baseUrl = serverUrl,
            token = token,
            userId = userId
        )

        result.fold(
            onSuccess = { books ->
                logI(TAG, "Loaded ${books.size} books from server")
                val booksDir = File(application.filesDir, "downloaded_books")
                val existingFileNames = booksDir.listFiles()?.map { it.name.lowercase() }?.toSet() ?: emptySet()
                val alreadyDownloaded = books.filter { b ->
                    val safeName = (b.fileName?.takeIf { it.isNotBlank() }
                        ?: (b.title.replace(Regex("[^a-zA-Z0-9_\\-\\.]"), "_") + ".epub"))
                        .let { if (it.endsWith(".epub", ignoreCase = true)) it else "$it.epub" }
                        .lowercase()
                    existingFileNames.contains(safeName)
                }.map { it.title }.toSet()

                _state.update {
                    it.copy(
                        remoteBooks = books,
                        downloadedBookTitles = it.downloadedBookTitles + alreadyDownloaded,
                        isRefreshingBooks = false
                    )
                }
            },
            onFailure = { err ->
                logE(TAG, "Failed to load remote books: ${err.message}")
                _state.update { it.copy(isRefreshingBooks = false) }
                _effects.emit(ConnectEffect.ShowToast("Failed to fetch books: ${err.message}"))
            }
        )
    }

    private suspend fun downloadBook(book: ua.acclorite.book_story.data.remote.model.RemoteBookDto) {
        val serverUrl = _state.value.serverUrl
        val token = connectPreferences.getAuthToken()

        if (_state.value.downloadingBookIds.contains(book.bookId)) {
            return
        }

        _state.update {
            it.copy(downloadingBookIds = it.downloadingBookIds + book.bookId)
        }

        withContext(Dispatchers.IO) {
            val safeFileName = (book.fileName?.takeIf { it.isNotBlank() }
                ?: (book.title.replace(Regex("[^a-zA-Z0-9_\\-\\.]"), "_") + ".epub"))
                .let { if (it.endsWith(".epub", ignoreCase = true)) it else "$it.epub" }

            val booksDir = File(application.filesDir, "downloaded_books").apply { mkdirs() }
            val destFile = File(booksDir, safeFileName)

            val downloadResult = apiClient.downloadBook(
                baseUrl = serverUrl,
                token = token,
                bookId = book.bookId,
                destinationFile = destFile
            )

            downloadResult.fold(
                onSuccess = { savedFile ->
                    logI(TAG, "Saved book file: ${savedFile.absolutePath}, importing into library...")

                    val domainFile = DomainFile(
                        name = savedFile.name,
                        uri = Uri.fromFile(savedFile).toString(),
                        path = savedFile.absolutePath,
                        size = savedFile.length(),
                        lastModified = savedFile.lastModified(),
                        isDirectory = false
                    )

                    val parsed = getBookFromFileUseCase(domainFile)
                    if (parsed != null) {
                        addBookUseCase(parsed.first, parsed.second)
                        LibraryScreen.refreshListChannel.trySend(0)
                        LibraryScreen.scrollToPageCompositionChannel.trySend(0)

                        _state.update {
                            it.copy(
                                downloadingBookIds = it.downloadingBookIds - book.bookId,
                                downloadedBookTitles = it.downloadedBookTitles + book.title
                            )
                        }

                        val successMsg = application.getString(
                            R.string.connect_book_downloaded_success,
                            book.title
                        )
                        _effects.emit(ConnectEffect.ShowToast(successMsg))
                    } else {
                        logE(TAG, "Could not parse downloaded EPUB: ${savedFile.name}")
                        _state.update {
                            it.copy(downloadingBookIds = it.downloadingBookIds - book.bookId)
                        }
                        _effects.emit(ConnectEffect.ShowToast("Error: Could not parse EPUB file structure"))
                    }
                },
                onFailure = { err ->
                    logE(TAG, "Download error for book ${book.bookId}: ${err.message}")
                    _state.update {
                        it.copy(downloadingBookIds = it.downloadingBookIds - book.bookId)
                    }
                    _effects.emit(ConnectEffect.ShowToast("Download failed: ${err.message}"))
                }
            )
        }
    }
}
