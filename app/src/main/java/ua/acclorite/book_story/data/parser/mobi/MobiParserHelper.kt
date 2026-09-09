/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.mobi

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import ua.acclorite.book_story.core.log.logE
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

data class MobiBookInfo(
    val title: String,
    val author: String?,
    val description: String?,
    val tags: List<String>,
    val coverRecordIndex: Int?,
    val firstImageRecordIndex: Int?,
    val recordOffsets: IntArray,
    val textRecordCount: Int,
    val compression: Int,
    val textEncoding: Charset,
    val extraRecordDataFlags: Int
)

object MobiParserHelper {
    private const val TAG = "MobiParserHelper"

    fun parseMobiHeader(file: File): MobiBookInfo? {
        if (!file.exists() || !file.canRead() || file.length() < 78) return null

        try {
            RandomAccessFile(file, "r").use { raf ->
                val fileSize = raf.length().toInt()

                // Read PDB Header
                raf.seek(76)
                val numRecords = raf.readUnsignedShort()
                if (numRecords <= 0 || file.length() < 78 + numRecords * 8) return null

                val recordOffsets = IntArray(numRecords + 1)
                for (i in 0 until numRecords) {
                    raf.seek(78L + i * 8L)
                    recordOffsets[i] = raf.readInt()
                }
                recordOffsets[numRecords] = fileSize

                if (recordOffsets[0] < 0 || recordOffsets[0] >= fileSize) return null
                val record0Offset = recordOffsets[0].toLong()
                val record0Length = (recordOffsets[1] - recordOffsets[0]).coerceAtLeast(0)
                if (record0Length < 16) return null

                val record0Bytes = ByteArray(record0Length)
                raf.seek(record0Offset)
                raf.readFully(record0Bytes)

                // 1. PalmDOC Header (16 bytes)
                val compression = getUShort(record0Bytes, 0)
                val textLength = getInt(record0Bytes, 4)
                val recordCount = getUShort(record0Bytes, 8)
                val encryptionType = getUShort(record0Bytes, 12)

                // If encrypted, we cannot decompress DRM files
                if (encryptionType == 2) {
                    logE(TAG, "MOBI file has DRM encryption.")
                }

                // 2. MOBI Header (starts at offset 16)
                var mobiHeaderLength = 0
                var textEncoding: Charset = StandardCharsets.UTF_8
                var fullNameOffset = 0
                var fullNameLength = 0
                var exthFlags = 0
                var firstImageRecordIndex: Int? = null
                var extraRecordDataFlags = 0
                var titleFromMobi: String? = null

                if (record0Length >= 16 + 24) {
                    val mobiId = String(record0Bytes, 16, 4, StandardCharsets.US_ASCII)
                    if (mobiId == "MOBI" || mobiId == "TEXT") {
                        mobiHeaderLength = getInt(record0Bytes, 20)

                        if (record0Length >= 16 + 16) {
                            val encodingCode = getInt(record0Bytes, 28)
                            textEncoding = if (encodingCode == 1252) {
                                Charset.forName("windows-1252")
                            } else {
                                StandardCharsets.UTF_8
                            }
                        }

                        if (record0Length >= 16 + 76) {
                            fullNameOffset = getInt(record0Bytes, 16 + 68) // Offset 84 from Record 0
                            fullNameLength = getInt(record0Bytes, 16 + 72) // Offset 88 from Record 0
                        }

                        if (record0Length >= 16 + 116) {
                            exthFlags = getInt(record0Bytes, 16 + 112) // Offset 128 from Record 0
                        }

                        if (record0Length >= 16 + 112) {
                            val firstNonBook = getInt(record0Bytes, 16 + 108) // Offset 0x6C in MOBI header
                            if (firstNonBook in 0 until numRecords) {
                                firstImageRecordIndex = firstNonBook
                            }
                        }

                        if (record0Length >= 16 + 230 && mobiHeaderLength >= 230) {
                            extraRecordDataFlags = getUShort(record0Bytes, 16 + 226)
                        }

                        if (fullNameOffset > 0 && fullNameLength > 0 && fullNameOffset + fullNameLength <= record0Bytes.size) {
                            titleFromMobi = String(record0Bytes, fullNameOffset, fullNameLength, textEncoding).trim()
                        }
                    }
                }

                // 3. EXTH Header
                var exthTitle: String? = null
                var author: String? = null
                var description: String? = null
                val tags = mutableListOf<String>()
                var coverRecordOffset: Int? = null

                val hasExth = (exthFlags and 0x40) != 0
                val exthOffset = 16 + mobiHeaderLength
                if (hasExth && record0Length >= exthOffset + 12) {
                    val exthId = String(record0Bytes, exthOffset, 4, StandardCharsets.US_ASCII)
                    if (exthId == "EXTH") {
                        val exthHeaderLength = getInt(record0Bytes, exthOffset + 4)
                        val exthCount = getInt(record0Bytes, exthOffset + 8)

                        var cur = exthOffset + 12
                        for (r in 0 until exthCount) {
                            if (cur + 8 > record0Bytes.size) break
                            val recordType = getInt(record0Bytes, cur)
                            val recordLength = getInt(record0Bytes, cur + 4)
                            if (recordLength < 8 || cur + recordLength > record0Bytes.size) break

                            val data = record0Bytes.copyOfRange(cur + 8, cur + recordLength)
                            cur += recordLength

                            when (recordType) {
                                100 -> { // Author
                                    val authStr = String(data, textEncoding).trim()
                                    if (authStr.isNotBlank() && author == null) {
                                        author = authStr
                                    }
                                }
                                103 -> { // Description
                                    val descStr = String(data, textEncoding).trim()
                                    if (descStr.isNotBlank() && description == null) {
                                        description = descStr
                                    }
                                }
                                105 -> { // Subject / Tag
                                    val tagStr = String(data, textEncoding).trim()
                                    if (tagStr.isNotBlank()) {
                                        tagStr.split(',', ';').forEach { t ->
                                            val clean = t.trim()
                                            if (clean.isNotBlank()) tags.add(clean)
                                        }
                                    }
                                }
                                201 -> { // Cover offset
                                    if (data.size >= 4) {
                                        coverRecordOffset = ByteBuffer.wrap(data).int
                                    }
                                }
                                503 -> { // Updated Title
                                    val tStr = String(data, textEncoding).trim()
                                    if (tStr.isNotBlank() && exthTitle == null) {
                                        exthTitle = tStr
                                    }
                                }
                            }
                        }
                    }
                }

                // Title resolution fallback
                val finalTitle = exthTitle?.ifBlank { null }
                    ?: titleFromMobi?.ifBlank { null }
                    ?: file.nameWithoutExtension.trim()

                val coverRecordIndex = if (coverRecordOffset != null && firstImageRecordIndex != null) {
                    val idx = firstImageRecordIndex + coverRecordOffset
                    if (idx in 0 until numRecords) idx else null
                } else if (firstImageRecordIndex != null && firstImageRecordIndex in 0 until numRecords) {
                    firstImageRecordIndex
                } else null

                return MobiBookInfo(
                    title = finalTitle,
                    author = author,
                    description = description,
                    tags = tags.distinctBy { it.lowercase() },
                    coverRecordIndex = coverRecordIndex,
                    firstImageRecordIndex = firstImageRecordIndex,
                    recordOffsets = recordOffsets,
                    textRecordCount = recordCount,
                    compression = compression,
                    textEncoding = textEncoding,
                    extraRecordDataFlags = extraRecordDataFlags
                )
            }
        } catch (e: Exception) {
            logE(TAG, "Failed to parse MOBI header: ${e.message}")
            return null
        }
    }

    fun readCoverImage(file: File, bookInfo: MobiBookInfo): Bitmap? {
        val coverIdx = bookInfo.coverRecordIndex ?: return null
        if (coverIdx >= bookInfo.recordOffsets.size - 1) return null

        return try {
            RandomAccessFile(file, "r").use { raf ->
                val start = bookInfo.recordOffsets[coverIdx].toLong()
                val len = (bookInfo.recordOffsets[coverIdx + 1] - bookInfo.recordOffsets[coverIdx]).coerceAtLeast(0)
                if (len <= 0) return null

                val bytes = ByteArray(len)
                raf.seek(start)
                raf.readFully(bytes)

                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (e: Exception) {
            logE(TAG, "Failed to read MOBI cover image: ${e.message}")
            null
        }
    }

    fun readTextContent(file: File, bookInfo: MobiBookInfo): String {
        val count = bookInfo.textRecordCount
        if (count <= 0) return ""

        return try {
            val stringBuilder = StringBuilder()

            RandomAccessFile(file, "r").use { raf ->
                for (r in 1..count) {
                    if (r >= bookInfo.recordOffsets.size - 1) break
                    val start = bookInfo.recordOffsets[r].toLong()
                    val len = (bookInfo.recordOffsets[r + 1] - bookInfo.recordOffsets[r]).coerceAtLeast(0)
                    if (len <= 0) continue

                    val rawData = ByteArray(len)
                    raf.seek(start)
                    raf.readFully(rawData)

                    val decompressed = when (bookInfo.compression) {
                        2 -> decompressRecord(rawData, bookInfo.extraRecordDataFlags)
                        1 -> rawData
                        else -> rawData
                    }

                    val text = String(decompressed, bookInfo.textEncoding)
                    stringBuilder.append(text)
                }
            }

            stringBuilder.toString()
        } catch (e: Exception) {
            logE(TAG, "Failed to read MOBI text content: ${e.message}")
            ""
        }
    }

    private fun decompressRecord(data: ByteArray, extraFlags: Int): ByteArray {
        var extraBytes = 0
        if (extraFlags != 0) {
            var flags = extraFlags ushr 1
            while (flags > 0) {
                if ((flags and 1) != 0 && data.size - extraBytes > 0) {
                    val trailingSize = getTrailingEntrySize(data, data.size - extraBytes)
                    extraBytes += trailingSize
                }
                flags = flags ushr 1
            }
            if ((extraFlags and 1) != 0 && data.size - extraBytes > 0) {
                val lastByte = data[data.size - 1 - extraBytes].toInt() and 0x03
                extraBytes += lastByte + 1
            }
        }
        val actualLength = (data.size - extraBytes).coerceIn(0, data.size)
        return decompressPalmDoc(data, actualLength)
    }

    private fun getTrailingEntrySize(data: ByteArray, offset: Int): Int {
        var size = 0
        val start = (offset - 4).coerceAtLeast(0)
        for (i in start until offset) {
            val b = data[i].toInt() and 0xFF
            if ((b and 0x80) != 0) {
                size = 0
            }
            size = (size shl 7) or (b and 0x7F)
        }
        return size
    }

    private fun decompressPalmDoc(data: ByteArray, length: Int): ByteArray {
        val out = ByteArrayOutputStream()
        var i = 0
        while (i < length) {
            val b = data[i].toInt() and 0xFF
            i++
            when {
                b == 0 -> {
                    out.write(0)
                }
                b in 1..8 -> {
                    val count = minOf(b, length - i)
                    out.write(data, i, count)
                    i += count
                }
                b in 9..0x7F -> {
                    out.write(b)
                }
                b in 0x80..0xBF -> {
                    if (i < length) {
                        val nextB = data[i].toInt() and 0xFF
                        i++
                        val distance = ((b and 0x3F) shl 3) or (nextB ushr 5)
                        val len = (nextB and 0x07) + 3
                        val buf = out.toByteArray()
                        val start = buf.size - distance
                        for (j in 0 until len) {
                            val readPos = start + j
                            if (readPos in buf.indices) {
                                out.write(buf[readPos].toInt())
                            } else {
                                out.write(' '.code)
                            }
                        }
                    }
                }
                b >= 0xC0 -> {
                    out.write(' '.code)
                    out.write(b xor 0x80)
                }
            }
        }
        return out.toByteArray()
    }

    private fun getInt(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)
    }

    private fun getUShort(bytes: ByteArray, offset: Int): Int {
        if (offset + 2 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0xFF) shl 8) or
                (bytes[offset + 1].toInt() and 0xFF)
    }
}
