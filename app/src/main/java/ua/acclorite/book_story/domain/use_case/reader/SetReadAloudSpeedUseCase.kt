package ua.acclorite.book_story.domain.use_case.reader

import ua.acclorite.book_story.domain.service.TextToSpeechService
import javax.inject.Inject

class SetReadAloudSpeedUseCase @Inject constructor(
    private val ttsService: TextToSpeechService
) {
    operator fun invoke(speed: Float) {
        ttsService.setSpeed(speed)
    }
}
