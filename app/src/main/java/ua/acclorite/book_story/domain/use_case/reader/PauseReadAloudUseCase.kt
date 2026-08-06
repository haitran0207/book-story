package ua.acclorite.book_story.domain.use_case.reader

import ua.acclorite.book_story.domain.service.TextToSpeechService
import javax.inject.Inject

class PauseReadAloudUseCase @Inject constructor(
    private val ttsService: TextToSpeechService
) {
    operator fun invoke() {
        ttsService.pause()
    }
}
