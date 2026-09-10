/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.model.file

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.runtime.Immutable
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.getAbsolutePath
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Cached File.
 * Faster than [androidx.documentfile.provider.DocumentFile].
 * Saves all it's variables after initialized.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
@Immutable
class CachedFile(
    private val context: Context,
    val uri: Uri,
    private val builder: CachedFileBuilder? = null
) {
    @Immutable
    private data class QueryParams(
        val name: String,
        val size: Long,
        val lastModified: Long,
        val isDirectory: Boolean
    )

    private val queryParams by lazy {
        getFileQueryParams()
    }
    val path: String by lazy { builder?.path ?: getFilePath() }
    val rawFile: File? by lazy { storeInCache() }

    val name: String get() = builder?.name ?: queryParams.name
    val size: Long get() = builder?.size ?: queryParams.size
    val lastModified: Long get() = builder?.lastModified ?: queryParams.lastModified
    val isDirectory: Boolean get() = builder?.isDirectory ?: queryParams.isDirectory

    fun canAccess(): Boolean {
        if (builder != null) return true
        val directPath = if (uri.scheme == "file" && uri.path != null) uri.path else builder?.path ?: path
        if (!directPath.isNullOrBlank()) {
            val file = File(directPath)
            if (file.exists() && file.canRead()) {
                return true
            }
        }
        return try {
            val docFile = DocumentFileCompat.fromUri(context, uri)
            docFile != null && docFile.canRead()
        } catch (e: Exception) {
            false
        }
    }

    fun openInputStream(): InputStream? {
        if (uri.scheme == "file" && uri.path != null) {
            try {
                val file = File(uri.path!!)
                if (file.exists() && file.canRead()) {
                    return java.io.FileInputStream(file)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val directPath = builder?.path ?: path
        if (directPath.isNotBlank()) {
            try {
                val file = File(directPath)
                if (file.exists() && file.canRead()) {
                    return java.io.FileInputStream(file)
                }
            } catch (e: Exception) {
                // fall through to contentResolver
            }
        }
        return try {
            context.contentResolver.openInputStream(uri)
                ?: throw Exception("Failed to open InputStream for URI: $uri")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun listFiles(forEach: ((CachedFile) -> Unit)? = null): List<CachedFile> {
        if (!isDirectory) return emptyList()

        val directPath = if (uri.scheme == "file" && uri.path != null) uri.path else builder?.path ?: path
        if (!directPath.isNullOrBlank()) {
            val dir = File(directPath)
            if (dir.isDirectory && dir.canRead()) {
                val files = dir.listFiles()
                if (files != null) {
                    val cachedFiles = mutableListOf<CachedFile>()
                    files.forEach { file ->
                        if (file.name.equals("Android", ignoreCase = true) || file.name.startsWith(".")) {
                            return@forEach
                        }
                        val queryFile = CachedFileCompat.fromUri(
                            context = context,
                            uri = Uri.fromFile(file),
                            builder = CachedFileCompat.build(
                                name = file.name,
                                path = file.absolutePath,
                                size = file.length(),
                                lastModified = file.lastModified(),
                                isDirectory = file.isDirectory
                            )
                        )
                        forEach?.invoke(queryFile)
                        cachedFiles.add(queryFile)
                    }
                    return cachedFiles
                }
            }
        }

        return try {
            val docFile = DocumentFileCompat.fromUri(context, uri) ?: return emptyList()
            if (!docFile.isDirectory) return emptyList()

            val children = docFile.listFiles()
            val cachedFiles = mutableListOf<CachedFile>()
            for (child in children) {
                val childName = child.name ?: continue
                if (childName.equals("Android", ignoreCase = true) || childName.startsWith(".")) {
                    continue
                }
                val childAbsPath = child.getAbsolutePath(context).ifBlank { "$path/$childName" }
                val queryFile = CachedFileCompat.fromUri(
                    context = context,
                    uri = child.uri,
                    builder = CachedFileCompat.build(
                        name = childName,
                        path = childAbsPath,
                        size = child.length(),
                        lastModified = child.lastModified(),
                        isDirectory = child.isDirectory
                    )
                )
                forEach?.invoke(queryFile)
                cachedFiles.add(queryFile)
            }
            cachedFiles
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun walk(
        includeDirectories: Boolean = false,
        forEach: ((CachedFile) -> Unit)? = null
    ): List<CachedFile> {
        val cachedFiles = mutableListOf<CachedFile>()

        // 1. If direct java.io.File access is available, use fast File.walk()
        val directPath = if (uri.scheme == "file" && uri.path != null) uri.path else builder?.path ?: path
        if (!directPath.isNullOrBlank()) {
            val dir = File(directPath)
            if (dir.isDirectory && dir.canRead()) {
                try {
                    dir.walkTopDown()
                        .onEnter { folder ->
                            !folder.name.equals("Android", ignoreCase = true) && !folder.name.startsWith(".")
                        }
                        .forEach { file ->
                            if (file.absolutePath == dir.absolutePath) return@forEach
                            if (file.name.startsWith(".")) return@forEach

                            val isDir = file.isDirectory
                            if (!isDir || includeDirectories) {
                                val cached = CachedFileCompat.fromUri(
                                    context = context,
                                    uri = Uri.fromFile(file),
                                    builder = CachedFileCompat.build(
                                        name = file.name,
                                        path = file.absolutePath,
                                        size = file.length(),
                                        lastModified = file.lastModified(),
                                        isDirectory = isDir
                                    )
                                )
                                forEach?.invoke(cached)
                                cachedFiles.add(cached)
                            }
                        }
                    return cachedFiles
                } catch (e: Exception) {
                    e.printStackTrace()
                    cachedFiles.clear()
                }
            }
        }

        // 2. Otherwise recursively traverse using listFiles() (which uses DocumentFileCompat)
        listFiles { child ->
            when (child.isDirectory) {
                false -> {
                    forEach?.invoke(child)
                    cachedFiles.add(child)
                }
                true -> {
                    if (includeDirectories) cachedFiles.add(child)
                    cachedFiles.addAll(child.walk(includeDirectories, forEach))
                }
            }
        }

        return cachedFiles
    }

    /**
     * Copies the file to [context].cacheDir and provides [File].
     *
     * @return null if the file is directory or failed
     */
    private fun storeInCache(): File? {
        if (isDirectory) return null
        val directPath = if (uri.scheme == "file" && uri.path != null) uri.path else builder?.path ?: path
        if (!directPath.isNullOrBlank()) {
            val f = File(directPath)
            if (f.exists() && f.canRead()) {
                return f
            }
        }
        val extension = name.substringAfterLast('.', "")
        val cacheFile = File(
            context.cacheDir,
            if (extension.isNotEmpty()) "${UUID.randomUUID()}.$extension" else UUID.randomUUID().toString()
        )

        try {
            openInputStream()?.use { input ->
                BufferedOutputStream(FileOutputStream(cacheFile)).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Failed to open InputStream.")
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        return cacheFile
    }

    private fun getFileQueryParams(): QueryParams {
        if (uri.scheme == "file" || (builder?.name != null && builder.size != null)) {
            val f = if (uri.scheme == "file" && uri.path != null) File(uri.path!!) else builder?.path?.let { File(it) }
            return QueryParams(
                name = builder?.name ?: f?.name ?: "unknown_${UUID.randomUUID()}",
                size = builder?.size ?: f?.length() ?: 0L,
                lastModified = builder?.lastModified ?: f?.lastModified() ?: 0L,
                isDirectory = builder?.isDirectory ?: f?.isDirectory ?: false
            )
        }

        try {
            val docFile = DocumentFileCompat.fromUri(context, uri)
            if (docFile != null) {
                return QueryParams(
                    name = builder?.name ?: docFile.name ?: "unknown_${UUID.randomUUID()}",
                    size = builder?.size ?: docFile.length(),
                    lastModified = builder?.lastModified ?: docFile.lastModified(),
                    isDirectory = builder?.isDirectory ?: docFile.isDirectory
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return QueryParams(
            name = builder?.name ?: "unknown_${UUID.randomUUID()}",
            size = builder?.size ?: 0L,
            lastModified = builder?.lastModified ?: 0L,
            isDirectory = builder?.isDirectory ?: false
        )
    }

    private fun getFilePath(): String {
        if (uri.scheme == "file" && uri.path != null) {
            return uri.path!!.trimEnd('/')
        }
        if (builder?.path != null) {
            return builder.path.trimEnd('/')
        }
        return try {
            val tempFile = DocumentFileCompat.fromUri(context, uri)
            tempFile?.getAbsolutePath(context)?.trimEnd('/') ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}