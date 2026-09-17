/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.text

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.core.log.logE
import ua.acclorite.book_story.data.model.file.CachedFile
import ua.acclorite.book_story.domain.model.reader.ParseChunk
import ua.acclorite.book_story.domain.model.reader.ReaderText
import javax.inject.Inject

private const val TAG = "TextParser"

class TextParserImpl @Inject constructor(
    // Markdown parser (Markdown)
    private val txtTextParser: TxtTextParser,
    private val pdfTextParser: PdfTextParser,

    // Document parser (HTML+Markdown)
    private val epubTextParser: EpubTextParser,
    private val htmlTextParser: HtmlTextParser,
    private val xmlTextParser: XmlTextParser,
    private val mobiTextParser: MobiTextParser
) : TextParser {

    override fun parseProgressive(cachedFile: CachedFile): Flow<ParseChunk> {
        if (!cachedFile.canAccess()) {
            logE(TAG, "File does not exist or no read access is granted.")
            return flowOf(ParseChunk(emptyList(), isFirstChunk = true, isLastChunk = true))
        }

        val fileFormat = ".${cachedFile.name.substringAfterLast(".")}".lowercase().trim()
        return when (fileFormat) {
            ".epub" -> epubTextParser.parseProgressive(cachedFile)
            ".txt" -> txtTextParser.parseProgressive(cachedFile)
            else -> super.parseProgressive(cachedFile)
        }
    }

    override suspend fun parse(cachedFile: CachedFile): List<ReaderText> {
        if (!cachedFile.canAccess()) {
            logE(TAG, "File does not exist or no read access is granted.")
            return emptyList()
        }

        val fileFormat = ".${cachedFile.name.substringAfterLast(".")}".lowercase().trim()
        return withContext(Dispatchers.IO) {
            when (fileFormat) {
                ".pdf" -> {
                    pdfTextParser.parse(cachedFile)
                }

                ".epub" -> {
                    epubTextParser.parse(cachedFile)
                }

                ".mobi", ".azw", ".azw3", ".prc" -> {
                    mobiTextParser.parse(cachedFile)
                }

                ".txt" -> {
                    txtTextParser.parse(cachedFile)
                }

                ".fb2" -> {
                    xmlTextParser.parse(cachedFile)
                }

                ".html" -> {
                    htmlTextParser.parse(cachedFile)
                }

                ".htm" -> {
                    htmlTextParser.parse(cachedFile)
                }

                ".md" -> {
                    htmlTextParser.parse(cachedFile)
                }

                else -> {
                    logE(TAG, "Wrong file format, could not find supported extension.")
                    emptyList()
                }
            }
        }
    }
}