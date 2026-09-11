package ua.acclorite.book_story.data.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ua.acclorite.book_story.domain.service.TextToSpeechService
import java.text.Normalizer
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

class TextToSpeechServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeechService, TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    @Volatile
    private var isInitialized = false
    private var initDeferred: CompletableDeferred<Boolean>? = null
    private val mutex = Mutex()

    @Volatile
    private var currentUtteranceDeferred: CompletableDeferred<Boolean>? = null

    private var currentSpeed: Float = 1.75f
    private var currentPitch: Float = 1.0f

    private var defaultLocale: Locale = Locale.getDefault()
    private var currentAppliedLocale: Locale? = null
    private var lastDetectedLanguageWasVietnamese = false

    override suspend fun initialize() {
        if (isInitialized && tts != null) return

        val deferred = mutex.withLock {
            if (isInitialized && tts != null) return
            val existing = initDeferred
            if (existing != null) {
                existing
            } else {
                val newDeferred = CompletableDeferred<Boolean>()
                initDeferred = newDeferred

                try {
                    tts = TextToSpeech(context.applicationContext, this)
                } catch (e: Exception) {
                    Log.e("TTS", "Failed to create TextToSpeech instance", e)
                    newDeferred.complete(false)
                }
                newDeferred
            }
        }

        deferred.await()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.let { engine ->
                try {
                    val sysLocale = Locale.getDefault()
                    val result = engine.setLanguage(sysLocale)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w("TTS", "Default locale $sysLocale not supported, falling back to US")
                        engine.setLanguage(Locale.US)
                        defaultLocale = Locale.US
                    } else {
                        defaultLocale = sysLocale
                    }
                    currentAppliedLocale = defaultLocale
                } catch (e: Exception) {
                    Log.e("TTS", "Error setting language on TTS init", e)
                    try {
                        engine.setLanguage(Locale.US)
                        defaultLocale = Locale.US
                        currentAppliedLocale = Locale.US
                    } catch (_: Exception) {}
                }

                try {
                    engine.setSpeechRate(currentSpeed)
                    engine.setPitch(currentPitch)
                } catch (e: Exception) {
                    Log.e("TTS", "Error setting speech rate/pitch on init", e)
                }

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d("TTS", "Utterance started: $utteranceId")
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d("TTS", "Utterance completed: $utteranceId")
                        currentUtteranceDeferred?.complete(true)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.e("TTS", "Utterance error: $utteranceId")
                        currentUtteranceDeferred?.complete(false)
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.e("TTS", "Utterance error with code $errorCode: $utteranceId")
                        currentUtteranceDeferred?.complete(false)
                    }
                })
            }
            initDeferred?.complete(true)
        } else {
            Log.e("TTS", "TextToSpeech onInit failed with status: $status")
            isInitialized = false
            initDeferred?.complete(false)
        }
        initDeferred = null
    }

    override suspend fun speak(text: String) {
        speakParagraph(text)
    }

    override suspend fun speakParagraph(text: String): Boolean {
        if (text.isBlank()) return true

        if (!isInitialized || tts == null) {
            initialize()
        }

        val engine = tts
        if (!isInitialized || engine == null) {
            Log.e("TTS", "Cannot speak: TextToSpeech is not initialized")
            return false
        }

        val chunks = chunkText(text, 1500)
        for (chunk in chunks) {
            val deferred = CompletableDeferred<Boolean>()
            currentUtteranceDeferred = deferred
            val utteranceId = "PARAGRAPH_${System.currentTimeMillis()}_${UUID.randomUUID()}"

            try {
                val targetLocale = determineTargetLocale(chunk)
                applyLanguage(engine, targetLocale)

                val chunkToSpeak = if (targetLocale == VIETNAMESE_LOCALE || targetLocale == VIETNAMESE_FALLBACK_LOCALE) {
                    applyVietnamesePronunciationReplacements(chunk)
                } else {
                    chunk
                }

                engine.setSpeechRate(currentSpeed)
                engine.setPitch(currentPitch)
                val result = engine.speak(chunkToSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                if (result != TextToSpeech.SUCCESS) {
                    Log.e("TTS", "engine.speak() failed with code $result")
                    deferred.complete(false)
                    return false
                }
            } catch (e: Exception) {
                Log.e("TTS", "Error executing speak() in speakParagraph", e)
                deferred.complete(false)
                return false
            }

            val completed = try {
                deferred.await()
            } catch (e: CancellationException) {
                try {
                    engine.stop()
                } catch (_: Exception) {}
                throw e
            }

            if (!completed) {
                return false
            }
        }

        return true
    }

    override fun pause() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("TTS", "Error in pause()", e)
        }
        currentUtteranceDeferred?.complete(false)
        currentUtteranceDeferred = null
    }

    override fun resume() {
        // Handled by restarting the paragraph loop in ReaderModel
    }

    override fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("TTS", "Error in stop()", e)
        }
        currentUtteranceDeferred?.complete(false)
        currentUtteranceDeferred = null
        lastDetectedLanguageWasVietnamese = false
    }

    override fun setSpeed(speed: Float) {
        currentSpeed = speed.coerceIn(0.25f, 3.0f)
        try {
            tts?.setSpeechRate(currentSpeed)
        } catch (e: Exception) {
            Log.e("TTS", "Error updating speech rate", e)
        }
    }

    override fun setPitch(pitch: Float) {
        currentPitch = pitch.coerceIn(0.25f, 2.0f)
        try {
            tts?.setPitch(currentPitch)
        } catch (e: Exception) {
            Log.e("TTS", "Error updating pitch", e)
        }
    }

    override fun getAvailableVoices(): List<String> {
        return try {
            tts?.voices?.map { it.name } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun chunkText(text: String, maxChunkSize: Int = 1500): List<String> {
        if (text.length <= maxChunkSize) return listOf(text)

        val chunks = mutableListOf<String>()
        val paragraphs = text.split("\n")
        var currentChunk = StringBuilder()

        for (paragraph in paragraphs) {
            if (currentChunk.length + paragraph.length + 1 <= maxChunkSize) {
                if (currentChunk.isNotEmpty()) currentChunk.append("\n")
                currentChunk.append(paragraph)
            } else {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString())
                    currentChunk = StringBuilder()
                }
                if (paragraph.length <= maxChunkSize) {
                    currentChunk.append(paragraph)
                } else {
                    val sentences = paragraph.split(Regex("(?<=[.!?])\\s+"))
                    for (sentence in sentences) {
                        if (currentChunk.length + sentence.length + 1 <= maxChunkSize) {
                            if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                            currentChunk.append(sentence)
                        } else {
                            if (currentChunk.isNotEmpty()) {
                                chunks.add(currentChunk.toString())
                                currentChunk = StringBuilder()
                            }
                            if (sentence.length <= maxChunkSize) {
                                currentChunk.append(sentence)
                            } else {
                                val words = sentence.split(" ")
                                for (word in words) {
                                    if (currentChunk.length + word.length + 1 <= maxChunkSize) {
                                        if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                                        currentChunk.append(word)
                                    } else {
                                        if (currentChunk.isNotEmpty()) {
                                            chunks.add(currentChunk.toString())
                                            currentChunk = StringBuilder()
                                        }
                                        currentChunk.append(word)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }
        return chunks
    }

    private fun applyLanguage(engine: TextToSpeech, targetLocale: Locale) {
        if (currentAppliedLocale == targetLocale) return
        try {
            val result = engine.setLanguage(targetLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("TTS", "Language $targetLocale not supported (code $result)")
                if (targetLocale == VIETNAMESE_LOCALE) {
                    val viResult = engine.setLanguage(VIETNAMESE_FALLBACK_LOCALE)
                    if (viResult != TextToSpeech.LANG_MISSING_DATA && viResult != TextToSpeech.LANG_NOT_SUPPORTED) {
                        currentAppliedLocale = VIETNAMESE_FALLBACK_LOCALE
                    }
                }
            } else {
                currentAppliedLocale = targetLocale
            }
        } catch (e: Exception) {
            Log.e("TTS", "Error setting language to $targetLocale", e)
        }
    }

    private fun determineTargetLocale(text: String): Locale {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFC)

        // 1. Direct check for Vietnamese-specific characters and diacritics
        if (VIETNAMESE_CHARACTERS_REGEX.containsMatchIn(normalized) || COMBINING_MARKS_REGEX.containsMatchIn(text)) {
            lastDetectedLanguageWasVietnamese = true
            return VIETNAMESE_LOCALE
        }

        // 2. Check if the text is clearly English (contains common English words)
        if (ENGLISH_WORDS_REGEX.containsMatchIn(text)) {
            lastDetectedLanguageWasVietnamese = false
            return defaultLocale
        }

        // 3. For short text without accents (e.g. numbers "123", "...", untoned names "Nam Cao", "Phan 1"):
        // If the context is Vietnamese, keep Vietnamese. Otherwise use default.
        if (lastDetectedLanguageWasVietnamese) {
            return VIETNAMESE_LOCALE
        }

        return defaultLocale
    }

    private fun applyVietnamesePronunciationReplacements(text: String): String {
        return PRONUNCIATION_REGEX.replace(text) { match ->
            val key = match.value.lowercase()
            VIETNAMESE_PRONUNCIATION_MAP[key] ?: match.value
        }
    }

    companion object {
        private val VIETNAMESE_LOCALE = Locale.forLanguageTag("vi-VN")
        private val VIETNAMESE_FALLBACK_LOCALE = Locale.forLanguageTag("vi")

        private val VIETNAMESE_CHARACTERS_REGEX = Regex(
            "[àáảãạăắằẳẵặâấầẩẫậđèéẻẽẹêếềểễệìíỉĩịòóỏõọôốồổỗộơớờởỡợùúủũụưứừửữựỳýỷỹỵ" +
            "ÀÁẢÃẠĂẮẰẲẴẶÂẤẦẨẪẬĐÈÉẺẼẸÊẾỀỂỄỆÌÍỈĨỊÒÓỎÕỌÔỐỒỔỖỘƠỚỜỞỠỢÙÚỦŨỤƯỨỪỬỮỰỲÝỶỸỴ]"
        )

        private val COMBINING_MARKS_REGEX = Regex("[\\u0300-\\u036F]")

        private val ENGLISH_WORDS_REGEX = Regex(
            "\\b(the|and|is|it|you|that|he|was|for|on|are|as|with|his|they|at|be|this|have|from|or|one|had|by|but|not|what|all|were|we|when|your|can|said|there|each|which|she|how|their|will|chapter|table|contents|foreword|preface|introduction|epilogue)\\b",
            RegexOption.IGNORE_CASE
        )

        private val VIETNAMESE_PRONUNCIATION_MAP: Map<String, String> = mapOf(
            // Logic & Programming keywords
            "if" to "íp",
            "else" to "eo-sờ",
            "for" to "pho",
            "while" to "oai",
            "switch" to "suyt",
            "case" to "kết-sờ",
            "break" to "brếch",
            "continue" to "cơn-ti-niu",
            "return" to "ri-tơn",
            "function" to "phăng-xừn",
            "class" to "clát",
            "object" to "óp-dếch",
            "code" to "cốt",
            "coding" to "cốt-đinh",
            "bug" to "bắc",
            "debug" to "đi-bắc",
            "error" to "e-rờ",
            "warning" to "oan-ninh",
            "try" to "trai",
            "catch" to "két",
            "finally" to "phai-nơ-li",
            "throw" to "thơ-râu",
            "null" to "nun",
            "true" to "tru",
            "false" to "phôn-sờ",
            "boolean" to "bu-li-ừn",
            "string" to "sờ-tring",
            "int" to "in-tờ",
            "integer" to "in-ti-giơ",
            "float" to "phơ-lốt",
            "double" to "đáp-bồ",
            "array" to "ơ-rây",
            "list" to "lít",
            "map" to "mép",
            "loop" to "lúp",
            "var" to "va",
            "val" to "van",
            "let" to "lét",
            "const" to "con-sờ",

            // Office & Spreadsheet (Excel, Word, PowerPoint)
            "excel" to "ếch-seo",
            "word" to "uốt",
            "powerpoint" to "pao-o-poy-ừn",
            "office" to "ọp-phít",
            "sheet" to "sít",
            "cell" to "seo",
            "row" to "râu",
            "column" to "co-lầm",
            "table" to "tây-bồ",
            "chart" to "chạt",
            "filter" to "phin-tơ",
            "sort" to "sọt",
            "sum" to "xum",
            "sumif" to "xum-íp",
            "countif" to "cao-íp",
            "vlookup" to "vê-lúc-cắp",
            "hlookup" to "hát-lúc-cắp",
            "index" to "in-đếch",
            "match" to "mát",
            "average" to "a-vơ-rít",
            "formula" to "pho-miu-la",
            "macro" to "mác-crô",
            "vba" to "vê-bê-a",
            "pivot" to "pi-vót",
            "pivottable" to "pi-vót tây-bồ",

            // Tech, Data & Web
            "database" to "đây-tơ-bây",
            "data" to "đây-ta",
            "server" to "sơ-vơ",
            "client" to "clai-ừn",
            "api" to "a-pi-ai",
            "app" to "áp",
            "apps" to "áp-sờ",
            "application" to "áp-pli-cây-xừn",
            "web" to "uép",
            "website" to "uép-sai",
            "internet" to "in-tơ-nét",
            "wifi" to "oai-phai",
            "bluetooth" to "bơ-lu-tút",
            "link" to "linh",
            "click" to "clích",
            "file" to "phai-ồ",
            "folder" to "phôn-đơ",
            "download" to "đao-lốt",
            "upload" to "ắp-lốt",
            "install" to "in-x-to",
            "setup" to "sét-ắp",
            "setting" to "sét-tinh",
            "settings" to "sét-tinh-sờ",
            "update" to "ắp-đét",
            "upgrade" to "ắp-grét",
            "reset" to "ri-xét",
            "restart" to "ri-x-tạt",
            "log" to "lóc",
            "login" to "lóc-in",
            "logout" to "lóc-ao",
            "cache" to "két",
            "cookie" to "cúc-ki",
            "token" to "tô-kừn",
            "key" to "ki",
            "user" to "diu-zơ",
            "admin" to "át-min",
            "password" to "pát-uốt",
            "input" to "in-pút",
            "output" to "ao-pút",
            "query" to "que-ri",
            "sql" to "ét-quy-en",
            "mysql" to "mai-ét-quy-en",
            "nosql" to "nô-ét-quy-en",
            "github" to "gít-hắp",
            "git" to "gít",
            "commit" to "com-mít",
            "push" to "pút",
            "pull" to "pu",
            "merge" to "mợt",
            "branch" to "bran-chờ",
            "repository" to "ri-pô-zi-to-ri",
            "repo" to "re-pô",

            // Programming languages & Platforms
            "python" to "pai-thần",
            "java" to "gia-va",
            "javascript" to "gia-va-scrip",
            "typescript" to "típ-scrip",
            "kotlin" to "cốt-lin",
            "php" to "pê-hắt-pê",
            "html" to "hắt-tê-em-nờ",
            "css" to "xê-ét-ét",
            "windows" to "win-đô",
            "android" to "an-đroi",
            "ios" to "ai-ô-ét",
            "linux" to "li-núc",
            "mac" to "mác",
            "macos" to "mác-ô-ét",

            // Common Tech Brands
            "google" to "gu-gồ",
            "microsoft" to "mai-crô-xóp",
            "apple" to "áp-pồ",
            "facebook" to "phây-sơ-búc",
            "youtube" to "diu-túp",
            "zalo" to "za-lô",
            "tiktok" to "tíc-tóc",
            "ai" to "a-ai",
            "cpu" to "xê-pê-u",
            "gpu" to "gờ-pê-u",
            "usb" to "u-ét-bê",
            "email" to "i-meo",
            "gmail" to "gờ-meo",
            "chat" to "chát",
            "bot" to "bót",
            "online" to "on-lai",
            "offline" to "ọp-lai",
            "test" to "tét",
            "testing" to "tét-tinh",
            "developer" to "đe-vơ-lóp-pơ",
            "dev" to "đép",
            "designer" to "đi-zai-nơ",
            "manager" to "ma-na-gơ",
            "team" to "tim",
            "project" to "prô-dếch",
            "task" to "tát"
        )

        private val PRONUNCIATION_REGEX = Regex(
            "\\b(" + VIETNAMESE_PRONUNCIATION_MAP.keys
                .sortedByDescending { it.length }
                .joinToString("|") { Regex.escape(it) } + ")\\b",
            RegexOption.IGNORE_CASE
        )
    }
}

