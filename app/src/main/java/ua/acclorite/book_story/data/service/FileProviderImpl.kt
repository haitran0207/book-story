/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.service

import android.app.Application
import ua.acclorite.book_story.data.model.file.CachedFile
import ua.acclorite.book_story.data.model.file.CachedFileCompat
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.domain.service.FileProvider
import javax.inject.Inject

class FileProviderImpl @Inject constructor(
    private val application: Application
) : FileProvider {

    override fun getFileFromBook(book: Book): Result<CachedFile> = runCatching {
        val directFile = java.io.File(book.filePath)
        if (directFile.exists() && directFile.canRead()) {
            return@runCatching CachedFileCompat.fromUri(
                application,
                android.net.Uri.fromFile(directFile),
                CachedFileCompat.build(
                    name = directFile.name,
                    path = directFile.absolutePath,
                    size = directFile.length(),
                    lastModified = directFile.lastModified(),
                    isDirectory = directFile.isDirectory
                )
            )
        }

        application.contentResolver.persistedUriPermissions.forEach { storage ->
            val storageFile = CachedFileCompat.fromUri(
                application,
                storage.uri
            )

            if (!storageFile.isDirectory) return@forEach
            if (!book.filePath.startsWith(storageFile.path, ignoreCase = true)) return@forEach

            storageFile.walk().forEach { file ->
                if (book.filePath.equals(file.path, ignoreCase = true)) {
                    return@runCatching file
                }
            }
        }

        throw NoSuchElementException("Could not find file from book.")
    }

    override fun getStorageFiles(): Result<List<CachedFile>> = runCatching {
        val safStorages = application.contentResolver.persistedUriPermissions.mapNotNull { permission ->
            val storage = CachedFileCompat.fromUri(
                application,
                permission.uri
            )
            if (!storage.isDirectory) return@mapNotNull null
            storage
        }

        val standardDirs = try {
            val externalStorage = android.os.Environment.getExternalStorageDirectory()
            listOfNotNull(
                externalStorage,
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS),
                java.io.File(externalStorage, "Books"),
                java.io.File(externalStorage, "eBooks"),
                java.io.File(externalStorage, "Kindle"),
                java.io.File(externalStorage, "Calibre")
            ).filter { it.exists() && it.isDirectory }.map { dir ->
                CachedFileCompat.fromUri(
                    application,
                    android.net.Uri.fromFile(dir),
                    CachedFileCompat.build(
                        name = dir.name,
                        path = dir.absolutePath,
                        size = dir.length(),
                        lastModified = dir.lastModified(),
                        isDirectory = true
                    )
                )
            }
        } catch (e: Exception) {
            emptyList()
        }

        val allStorages = safStorages + standardDirs
        allStorages.filter { storage ->
            allStorages.none { other ->
                other.path != storage.path && storage.path.startsWith(
                    other.path,
                    ignoreCase = true
                )
            }
        }.distinctBy { it.path.lowercase() }
    }
}