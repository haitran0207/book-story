package ua.acclorite.book_story.domain.service

interface TextToSpeechService {
    suspend fun initialize()
    suspend fun speak(text: String)
    suspend fun speakParagraph(text: String): Boolean
    fun pause()
    fun resume()
    fun stop()
    fun setSpeed(speed: Float)
    fun setPitch(pitch: Float)
    fun getAvailableVoices(): List<String>
}
