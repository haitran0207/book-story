/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.text

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import ua.acclorite.book_story.core.log.logE
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.data.model.file.CachedFile
import ua.acclorite.book_story.data.parser.document.DocumentParser
import ua.acclorite.book_story.data.parser.mobi.MobiParserHelper
import ua.acclorite.book_story.domain.model.reader.ReaderText
import javax.inject.Inject

private const val TAG = "MobiTextParser"

class MobiTextParser @Inject constructor(
    private val documentParser: DocumentParser
) : TextParser {

    override suspend fun parse(cachedFile: CachedFile): List<ReaderText> {
        logI(TAG, "Started MOBI parsing: ${cachedFile.name}.")

        return try {
            val rawFile = cachedFile.rawFile
            if (rawFile == null || !rawFile.exists() || !rawFile.canRead()) return emptyList()

            withContext(Dispatchers.IO) {
                val bookInfo = MobiParserHelper.parseMobiHeader(rawFile) ?: return@withContext emptyList()
                val htmlContent = MobiParserHelper.readTextContent(rawFile, bookInfo)
                if (htmlContent.isBlank()) return@withContext emptyList()

                val document = Jsoup.parse(htmlContent, "", Parser.htmlParser())
                val readerText = documentParser.parseDocument(document)

                yield()

                if (
                    readerText.isEmpty() ||
                    readerText.none { it is ReaderText.Text || it is ReaderText.Formula }
                ) {
                    logE(TAG, "Could not extract text from MOBI.")
                    return@withContext emptyList()
                }

                logI(TAG, "Successfully finished MOBI parsing.")
                readerText
            }
        } catch (e: Exception) {
            logE(TAG, "Could not parse MOBI text: ${e.message}")
            emptyList()
        }
    }
}
