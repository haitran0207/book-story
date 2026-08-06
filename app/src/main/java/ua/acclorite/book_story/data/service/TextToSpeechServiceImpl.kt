package ua.acclorite.book_story.data.service

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import ua.acclorite.book_story.domain.service.TextToSpeechService
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

class TextToSpeechServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeechService, TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var initContinuation: kotlin.coroutines.Continuation<Unit>? = null

    override suspend fun initialize() {
        if (isInitialized) return
        suspendCancellableCoroutine { continuation ->
            initContinuation = continuation
            tts = TextToSpeech(context, this)
            continuation.invokeOnCancellation {
                tts?.shutdown()
                tts = null
                isInitialized = false
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale.getDefault()
            initContinuation?.resume(Unit)
        } else {
            initContinuation?.resume(Unit)
        }
        initContinuation = null
    }

    override suspend fun speak(text: String) {
        if (!isInitialized) initialize()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
    }

    override fun pause() {
        tts?.stop()
    }

    override fun resume() {
        // Resuming from the same position requires custom chunking logic, which is managed in ViewModel typically.
    }

    override fun stop() {
        tts?.stop()
    }

    override fun setSpeed(speed: Float) {
        tts?.setSpeechRate(speed)
    }

    override fun setPitch(pitch: Float) {
        tts?.setPitch(pitch)
    }

    override fun getAvailableVoices(): List<String> {
        return tts?.voices?.map { it.name } ?: emptyList()
    }
}
