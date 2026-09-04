package ua.acclorite.book_story.domain.reader.model

data class ReadAloudState(
    val isPlaying: Boolean = false,
    val speed: Float = 1.75f,
    val pitch: Float = 1.0f,
    val voice: String? = null,
    val currentReadingIndex: Int? = null
)
