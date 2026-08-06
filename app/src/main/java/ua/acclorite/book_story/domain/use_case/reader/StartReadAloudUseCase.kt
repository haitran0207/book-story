package ua.acclorite.book_story.domain.use_case.reader

import ua.acclorite.book_story.domain.service.TextToSpeechService
import javax.inject.Inject

class StartReadAloudUseCase @Inject constructor(
    private val ttsService: TextToSpeechService
) {
    suspend operator fun invoke(text: String) {
        ttsService.speak(text)
    }
}
