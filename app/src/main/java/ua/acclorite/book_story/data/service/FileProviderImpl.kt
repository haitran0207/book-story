/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.service

import android.app.Application
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.getAbsolutePath
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.data.model.file.CachedFile
import ua.acclorite.book_story.data.model.file.CachedFileCompat
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.domain.service.FileProvider
import javax.inject.Inject

private const val TAG = "FileProvider"

class FileProviderImpl @Inject constructor(
    private val application: Application
) : FileProvider {

    override fun getFileFromBook(book: Book): Result<CachedFile> = runCatching {
        // 1. Direct POSIX File check (fastest when accessible directly)
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

        // 2. Direct SAF document resolution using the book's current filePath (O(1), < 5ms)
        val directDoc = resolveDocFileFromPath(book.filePath)
        if (directDoc != null) {
            return@runCatching toCachedFile(directDoc, book.filePath)
        }

        // 3. Fast self-healing resolution across mounted storages and persisted URI permissions
        val healedFile = resolveHealedFile(book)
        if (healedFile != null) {
            return@runCatching healedFile
        }

        throw NoSuchElementException("Could not find file from book: ${book.filePath}")
    }

    private fun resolveDocFileFromPath(path: String): DocumentFile? {
        if (path.isBlank()) return null
        return try {
            val doc = DocumentFileCompat.fromFullPath(application, path)
            if (doc != null && doc.exists() && doc.canRead()) doc else null
        } catch (e: Exception) {
            null
        }
    }

    private fun toCachedFile(docFile: DocumentFile, fallbackPath: String): CachedFile {
        val resolvedPath = docFile.getAbsolutePath(application).ifBlank { fallbackPath }
        return CachedFileCompat.fromUri(
            application,
            docFile.uri,
            CachedFileCompat.build(
                name = docFile.name ?: java.io.File(resolvedPath).name,
                path = resolvedPath,
                size = docFile.length(),
                lastModified = docFile.lastModified(),
                isDirectory = docFile.isDirectory
            )
        )
    }

    private fun resolveHealedFile(book: Book): CachedFile? {
        val originalFile = java.io.File(book.filePath)
        val fileName = originalFile.name
        val parentDirName = originalFile.parentFile?.name.orEmpty()

        val persistedPermissions = application.contentResolver.persistedUriPermissions
        val storageIds = try {
            DocumentFileCompat.getStorageIds(application)
        } catch (e: Exception) {
            emptyList()
        }

        // 3a. Candidate path checks against current mounted storages and SAF roots
        val candidatePaths = mutableListOf<String>()

        for (permission in persistedPermissions) {
            val rootDoc = try {
                DocumentFileCompat.fromUri(application, permission.uri)
            } catch (e: Exception) {
                null
            } ?: continue

            val rootPath = rootDoc.getAbsolutePath(application)
            if (rootPath.isNotBlank()) {
                if (parentDirName.isNotBlank()) {
                    candidatePaths.add("$rootPath/$parentDirName/$fileName")
                }
                candidatePaths.add("$rootPath/$fileName")
            }
        }

        for (storageId in storageIds) {
            val volumePrefix = if (storageId.equals("primary", ignoreCase = true)) {
                "/storage/emulated/0"
            } else {
                "/storage/$storageId"
            }
            if (parentDirName.isNotBlank()) {
                candidatePaths.add("$volumePrefix/0. Book/$parentDirName/$fileName")
                candidatePaths.add("$volumePrefix/Book/$parentDirName/$fileName")
                candidatePaths.add("$volumePrefix/$parentDirName/$fileName")
            }
            candidatePaths.add("$volumePrefix/0. Book/$fileName")
            candidatePaths.add("$volumePrefix/Book/$fileName")
            candidatePaths.add("$volumePrefix/$fileName")
        }

        for (candidatePath in candidatePaths.distinct()) {
            val candidateDoc = resolveDocFileFromPath(candidatePath)
            if (candidateDoc != null) {
                logI(TAG, "Healed file for [${book.title}] at [$candidatePath]")
                return toCachedFile(candidateDoc, candidatePath)
            }
            val rawCandidate = java.io.File(candidatePath)
            if (rawCandidate.exists() && rawCandidate.canRead()) {
                logI(TAG, "Healed file for [${book.title}] via raw File at [$candidatePath]")
                return CachedFileCompat.fromUri(
                    application,
                    android.net.Uri.fromFile(rawCandidate),
                    CachedFileCompat.build(
                        name = rawCandidate.name,
                        path = rawCandidate.absolutePath,
                        size = rawCandidate.length(),
                        lastModified = rawCandidate.lastModified(),
                        isDirectory = rawCandidate.isDirectory
                    )
                )
            }
        }

        // 3b. Direct directory navigation inside rootDoc
        for (permission in persistedPermissions) {
            val rootDoc = try {
                DocumentFileCompat.fromUri(application, permission.uri)
            } catch (e: Exception) {
                null
            } ?: continue

            if (parentDirName.isNotBlank()) {
                val targetDoc = rootDoc.findFile(parentDirName)?.findFile(fileName)
                if (targetDoc != null && targetDoc.exists() && targetDoc.canRead()) {
                    return toCachedFile(targetDoc, targetDoc.getAbsolutePath(application))
                }
            }
            val directInRoot = rootDoc.findFile(fileName)
            if (directInRoot != null && directInRoot.exists() && directInRoot.canRead()) {
                return toCachedFile(directInRoot, directInRoot.getAbsolutePath(application))
            }

            // 3c. Immediate subfolder check (depth 1 only, NO deep recursive walk)
            try {
                val subfolders = rootDoc.listFiles().filter { it.isDirectory }
                for (subfolder in subfolders) {
                    val childDoc = subfolder.findFile(fileName)
                    if (childDoc != null && childDoc.exists() && childDoc.canRead()) {
                        return toCachedFile(childDoc, childDoc.getAbsolutePath(application))
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        return null
    }

    override fun getStorageFiles(): Result<List<CachedFile>> = runCatching {
        val safStorages = application.contentResolver.persistedUriPermissions.mapNotNull { permission ->
            try {
                val storage = CachedFileCompat.fromUri(
                    application,
                    permission.uri
                )
                if (!storage.isDirectory) return@mapNotNull null
                storage
            } catch (e: Exception) {
                null
            }
        }

        if (safStorages.isNotEmpty()) {
            return@runCatching safStorages
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

        standardDirs.filter { storage ->
            standardDirs.none { other ->
                other.path != storage.path && storage.path.startsWith(
                    other.path,
                    ignoreCase = true
                )
            }
        }.distinctBy { it.path.lowercase() }
    }
}