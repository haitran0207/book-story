/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.repository

import android.app.Application
import android.content.ContentUris
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.core.CoverImage
import ua.acclorite.book_story.core.data.ExtensionsData
import ua.acclorite.book_story.data.local.room.BookDatabase
import ua.acclorite.book_story.data.mapper.file.FileMapper
import ua.acclorite.book_story.data.model.file.CachedFile
import ua.acclorite.book_story.data.model.file.CachedFileCompat
import ua.acclorite.book_story.data.parser.cover.CoverParser
import ua.acclorite.book_story.data.parser.file.FileParser
import ua.acclorite.book_story.domain.model.file.File
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.domain.repository.FileSystemRepository
import ua.acclorite.book_story.domain.service.FileProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileSystemRepositoryImpl @Inject constructor(
    private val application: Application,
    private val database: BookDatabase,
    private val fileMapper: FileMapper,
    private val fileParser: FileParser,
    private val coverParser: CoverParser,
    private val fileProvider: FileProvider
) : FileSystemRepository {

    override suspend fun searchFiles(query: String): Result<List<File>> {
        return withContext(Dispatchers.IO) {
            fileProvider.getStorageFiles().mapCatching { storages ->
                val existingFiles = database.bookDao.searchBooks("").map { it.filePath }

                val filesFromStorage = storages.map { storage ->
                    storage.getFilesFromStorage(
                        query = query,
                        existingFiles = existingFiles
                    )
                }.flatten()

                val mediaStoreFiles = queryMediaStoreFiles(query, existingFiles)

                (filesFromStorage + mediaStoreFiles).distinctBy { it.path.lowercase() }
            }
        }
    }

    private fun queryMediaStoreFiles(
        query: String,
        existingFiles: List<String>
    ): List<File> {
        val files = mutableListOf<File>()
        try {
            val uri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED
            )

            val selectionParts = ExtensionsData.fileExtensions.map {
                "${MediaStore.Files.FileColumns.DATA} LIKE ?"
            }
            val selection = selectionParts.joinToString(" OR ")
            val selectionArgs = ExtensionsData.fileExtensions.map { "%$it" }.toTypedArray()

            application.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val path = cursor.getString(dataIndex) ?: continue
                    val name = cursor.getString(nameIndex) ?: path.substringAfterLast("/")
                    val size = cursor.getLong(sizeIndex)
                    val lastModified = cursor.getLong(dateIndex) * 1000L
                    val fileUri = ContentUris.withAppendedId(uri, id)

                    val cached = CachedFileCompat.fromUri(
                        context = application,
                        uri = fileUri,
                        builder = CachedFileCompat.build(
                            name = name,
                            path = path,
                            size = size,
                            lastModified = lastModified,
                            isDirectory = false
                        )
                    )

                    if (cached.isValid(query, existingFiles)) {
                        files.add(fileMapper.toFile(cached))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return files
    }

    private fun CachedFile.isValid(
        query: String,
        existingFiles: List<String>
    ): Boolean {
        if (
            ExtensionsData.fileExtensions.none {
                name.endsWith(
                    it,
                    ignoreCase = true
                )
            }
        ) return false
        if (query.isNotBlank() && !name.contains(query.trim(), ignoreCase = true)) return false
        if (existingFiles.any { it.equals(path, ignoreCase = true) }) return false
        return true
    }

    private fun CachedFile.getFilesFromStorage(
        query: String,
        existingFiles: List<String>
    ): List<File> {
        val files = mutableListOf<File>()
        walk { cachedFile ->
            if (cachedFile.isValid(query, existingFiles)) {
                files.add(fileMapper.toFile(cachedFile))
            }
        }
        return files
    }

    override suspend fun getBookFromFile(file: File): Result<Pair<Book, CoverImage?>> =
        runCatching {
            withContext(Dispatchers.IO) {
                val cachedFile = fileMapper.toCachedFile(file)

                val book = fileParser.parse(
                    cachedFile = cachedFile
                ) ?: throw Exception("Could not parse ${file.name}.")
                val coverImage = coverParser.parse(cachedFile = cachedFile)

                return@withContext book to coverImage
            }
        }
}