/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.document

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.yield
import org.jsoup.nodes.Document
import ua.acclorite.book_story.core.helpers.clearAllMarkdown
import ua.acclorite.book_story.core.helpers.clearMarkdown
import ua.acclorite.book_story.core.helpers.containsVisibleText
import ua.acclorite.book_story.domain.model.reader.ReaderText
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.inject.Inject

class DocumentParser @Inject constructor(
    private val markdownParser: MarkdownParser
) {
    /**
     * Parses document to get it's text.
     * Fixes issues such as manual line breaking in <p>.
     * Applies Markdown to the text: Bold(**), Italic(_), Section separator(---), and Links(a > href).
     *
     * @return Parsed text line by line with Markdown(all lines are not blank).
     */
    suspend fun parseDocument(
        document: Document,
        zipFile: ZipFile? = null,
        imageEntries: List<ZipEntry>? = null,
        includeChapter: Boolean = true
    ): List<ReaderText> {
        yield()

        val readerText = mutableListOf<ReaderText>()
        var chapterAdded = false

        // Process MathML, matrices and LaTeX math tags before extracting text
        val formulasMap = ua.acclorite.book_story.data.parser.math.MathParser.processMathInDocument(document)

        document.selectFirst("body")
            .run { this ?: document.body() }
            .apply {
                // Remove manual line breaks from all <p>, <a>
                select("p").forEach { element ->
                    yield()
                    element.html(element.html().replace(Regex("\\n+"), " "))
                    element.append("\n")
                }
                select("a").forEach { element ->
                    yield()
                    element.html(element.html().replace(Regex("\\n+"), ""))
                }

                // Remove <head>'s title
                select("title").remove()

                // Markdown
                select("hr").append("\n---\n")
                select("b").append("**").prepend("**")
                select("h1").append("**").prepend("**")
                select("h2").append("**").prepend("**")
                select("h3").append("**").prepend("**")
                select("strong").append("**").prepend("**")
                select("em").append("_").prepend("_")
                select("a").forEach { element ->
                    var link = element.attr("href")
                    if (!link.startsWith("http") || element.wholeText().isBlank()) return@forEach

                    if (link.startsWith("http://")) {
                        link = link.replaceFirst("http://", "https://")
                    }

                    element.prepend("[")
                    element.append("]($link)")
                }

                // Image (<img>)
                select("img").forEach { element ->
                    val src = element.attr("src")
                        .trim()
                        .substringAfterLast(File.separator)
                        .lowercase()
                        .let { src -> URLDecoder.decode(src, StandardCharsets.UTF_8.name()) }
                        .takeIf {
                            it.containsVisibleText() && imageEntries?.any { image ->
                                it == image.name.substringAfterLast(File.separator).lowercase()
                            } == true
                        } ?: return@forEach

                    val alt = element.attr("alt").trim().takeIf {
                        it.clearMarkdown().containsVisibleText()
                    } ?: "Image"

                    element.append("\n[[$src|$alt]]\n")
                }

                // Image (<image>)
                select("image").forEach { element ->
                    val src = element.attr("xlink:href")
                        .trim()
                        .substringAfterLast(File.separator)
                        .lowercase()
                        .let { src -> URLDecoder.decode(src, StandardCharsets.UTF_8.name()) }
                        .takeIf {
                            it.containsVisibleText() && imageEntries?.any { image ->
                                it == image.name.substringAfterLast(File.separator).lowercase()
                            } == true
                        } ?: return@forEach

                    val alt = "Image"

                    element.append("\n[[$src|$alt]]\n")
                }
            }.wholeText().lines().forEach { rawLine ->
                yield()

                val line = rawLine.trim()
                if (!line.containsVisibleText()) return@forEach

                val imageRegex = Regex("""\[\[(.*?)\|(.*?)]]""")
                val mathRegex = Regex("""\[\[MATH_BLOCK\|(.*?)\]\]""")

                // 1. Separators
                if (line == "---" || line == "***") {
                    readerText.add(ReaderText.Separator)
                    return@forEach
                }

                // 2. Standalone Math Block
                if (mathRegex.matches(line)) {
                    val id = line.removeSurrounding("[[MATH_BLOCK|", "]]").trim()
                    val formula = formulasMap[id]
                    if (formula != null) {
                        readerText.add(formula)
                        return@forEach
                    }
                }

                // 3. Standalone Image
                if (imageRegex.matches(line)) {
                    val trimmedLine = line.removeSurrounding("[[", "]]")
                    val src = trimmedLine.substringBefore("|")
                    val alt = "_${trimmedLine.substringAfter("|")}_"

                    val image = try {
                        val imageEntry = imageEntries?.find { img ->
                            src == img.name.substringAfterLast(File.separator).lowercase()
                        }
                        if (imageEntry != null) zipFile?.getImage(imageEntry)?.asImageBitmap() else null
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }

                    if (image != null) {
                        image.prepareToDraw()
                        readerText.add(ReaderText.Image(imageBitmap = image))
                        readerText.add(ReaderText.Text(markdownParser.parse(alt)))
                        return@forEach
                    }
                }

                // 4. Line with embedded math block or image placeholders
                if (line.contains("[[MATH_BLOCK|") || line.contains("[[")) {
                    // Split line into text, math tokens, and image tokens
                    val tokenRegex = Regex("""(\[\[MATH_BLOCK\|.*?\]\]|\[\[.*?\|.*?\]\])""")
                    var lastIndex = 0
                    val matches = tokenRegex.findAll(line).toList()

                    if (matches.isNotEmpty()) {
                        for (match in matches) {
                            val textBefore = line.substring(lastIndex, match.range.first).trim()
                            if (textBefore.clearMarkdown().containsVisibleText()) {
                                readerText.add(ReaderText.Text(markdownParser.parse(formatMarkdown(textBefore))))
                            }

                            val token = match.value
                            if (mathRegex.matches(token)) {
                                val id = token.removeSurrounding("[[MATH_BLOCK|", "]]").trim()
                                formulasMap[id]?.let { readerText.add(it) }
                            } else if (imageRegex.matches(token)) {
                                val trimmedLine = token.removeSurrounding("[[", "]]")
                                val src = trimmedLine.substringBefore("|")
                                val alt = "_${trimmedLine.substringAfter("|")}_"

                                val image = try {
                                    val imageEntry = imageEntries?.find { img ->
                                        src == img.name.substringAfterLast(File.separator).lowercase()
                                    }
                                    if (imageEntry != null) zipFile?.getImage(imageEntry)?.asImageBitmap() else null
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    null
                                }

                                if (image != null) {
                                    image.prepareToDraw()
                                    readerText.add(ReaderText.Image(imageBitmap = image))
                                    readerText.add(ReaderText.Text(markdownParser.parse(alt)))
                                }
                            }
                            lastIndex = match.range.last + 1
                        }

                        val textAfter = line.substring(lastIndex).trim()
                        if (textAfter.clearMarkdown().containsVisibleText()) {
                            readerText.add(ReaderText.Text(markdownParser.parse(formatMarkdown(textAfter))))
                        }
                        return@forEach
                    }
                }

                // 5. Standard text line
                val formattedLine = formatMarkdown(line)
                if (
                    !chapterAdded &&
                    formattedLine.clearAllMarkdown().containsVisibleText() &&
                    includeChapter
                ) {
                    readerText.add(
                        0, ReaderText.Chapter(
                            title = formattedLine.clearAllMarkdown(),
                            nested = false
                        )
                    )
                    chapterAdded = true
                } else if (formattedLine.clearMarkdown().containsVisibleText()) {
                    readerText.add(
                        ReaderText.Text(
                            line = markdownParser.parse(formattedLine)
                        )
                    )
                }
            }

        yield()

        if (
            readerText.none { it is ReaderText.Text || it is ReaderText.Formula || it is ReaderText.Image } ||
            (includeChapter && readerText.filterIsInstance<ReaderText.Chapter>().isEmpty())
        ) {
            return emptyList()
        }

        return readerText
    }

    private fun formatMarkdown(line: String): String {
        return line.replace(
            Regex("""\*\*\*\s*(.*?)\s*\*\*\*"""), "_**$1**_"
        ).replace(
            Regex("""\*\*\s*(.*?)\s*\*\*"""), "**$1**"
        ).replace(
            Regex("""_\s*(.*?)\s*_"""), "_$1_"
        ).trim()
    }

    /**
     * Getting bitmap from [ZipFile] with compression
     * that depends on the [imageEntry] size.
     */
    private fun ZipFile.getImage(imageEntry: ZipEntry): Bitmap? {
        fun getBitmapFromInputStream(compressionLevel: Int = 1): Bitmap? {
            return getInputStream(imageEntry).use { inputStream ->
                BitmapFactory.decodeStream(
                    inputStream,
                    null,
                    BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.RGB_565
                        inSampleSize = compressionLevel
                    }
                )
            }
        }


        val uncompressedBitmap = getBitmapFromInputStream() ?: return null
        return uncompressedBitmap
    }
}