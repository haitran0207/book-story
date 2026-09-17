/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.text

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import ua.acclorite.book_story.core.helpers.clearAllMarkdown
import ua.acclorite.book_story.core.log.logE
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.data.model.file.CachedFile
import ua.acclorite.book_story.data.parser.document.MarkdownParser
import ua.acclorite.book_story.domain.model.reader.ParseChunk
import ua.acclorite.book_story.domain.model.reader.ReaderText
import javax.inject.Inject

private const val TAG = "TxtTextParser"

class TxtTextParser @Inject constructor(
    private val markdownParser: MarkdownParser
) : TextParser {

    override fun parseProgressive(cachedFile: CachedFile): Flow<ParseChunk> = flow {
        logI(TAG, "Started progressive TXT parsing: ${cachedFile.name}.")
        var chapterAdded = false
        var isFirstChunkEmitted = false
        val initialLineCount = 500
        val batchLineCount = 1000

        cachedFile.openInputStream()?.bufferedReader()?.use { reader ->
            val currentBatch = mutableListOf<ReaderText>()
            var lineCounter = 0

            var line = reader.readLine()
            while (line != null) {
                if (line.isNotBlank()) {
                    when (line) {
                        "***", "---" -> currentBatch.add(ReaderText.Separator)
                        else -> {
                            if (!chapterAdded && line.clearAllMarkdown().isNotBlank()) {
                                currentBatch.add(
                                    0,
                                    ReaderText.Chapter(
                                        title = line.clearAllMarkdown(),
                                        nested = false
                                    )
                                )
                                chapterAdded = true
                            } else {
                                currentBatch.add(
                                    ReaderText.Text(
                                        line = markdownParser.parse(line)
                                    )
                                )
                            }
                        }
                    }
                    lineCounter++

                    val threshold = if (!isFirstChunkEmitted) initialLineCount else batchLineCount
                    if (lineCounter >= threshold) {
                        emit(
                            ParseChunk(
                                items = currentBatch.toList(),
                                isFirstChunk = !isFirstChunkEmitted,
                                isLastChunk = false
                            )
                        )
                        isFirstChunkEmitted = true
                        currentBatch.clear()
                        lineCounter = 0
                    }
                }
                line = reader.readLine()
            }

            emit(
                ParseChunk(
                    items = currentBatch.toList(),
                    isFirstChunk = !isFirstChunkEmitted,
                    isLastChunk = true
                )
            )
        } ?: run {
            emit(ParseChunk(emptyList(), isFirstChunk = true, isLastChunk = true))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun parse(cachedFile: CachedFile): List<ReaderText> {
        logI(TAG, "Started TXT parsing: ${cachedFile.name}.")

        return try {
            val readerText = mutableListOf<ReaderText>()
            var chapterAdded = false

            withContext(Dispatchers.IO) {
                cachedFile.openInputStream()?.bufferedReader()?.use { reader ->
                    reader.forEachLine { line ->
                        if (line.isNotBlank()) {
                            when (line) {
                                "***", "---" -> readerText.add(
                                    ReaderText.Separator
                                )

                                else -> {
                                    if (!chapterAdded && line.clearAllMarkdown().isNotBlank()) {
                                        readerText.add(
                                            0, ReaderText.Chapter(
                                                title = line.clearAllMarkdown(),
                                                nested = false
                                            )
                                        )
                                        chapterAdded = true
                                    } else readerText.add(
                                        ReaderText.Text(
                                            line = markdownParser.parse(line)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            yield()

            if (
                readerText.filterIsInstance<ReaderText.Text>().isEmpty() ||
                readerText.filterIsInstance<ReaderText.Chapter>().isEmpty()
            ) {
                logE(TAG, "Could not extract text from TXT.")
                return emptyList()
            }

            logI(TAG, "Successfully finished TXT parsing.")
            readerText
        } catch (e: Exception) {
            logE(TAG, "Could not parse text with message: ${e.message}.")
            emptyList()
        }
    }
}