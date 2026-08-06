package ua.acclorite.book_story.ui.settings.reader.read_aloud

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import ua.acclorite.book_story.ui.common.components.settings.SettingsCategory
import ua.acclorite.book_story.presentation.settings.SettingsEvent
import ua.acclorite.book_story.presentation.settings.SettingsState

@Composable
fun ReadAloudSubcategory(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit
) {
    SettingsCategory(title = "Read Aloud") {
        // These are placeholders for actual Read Aloud settings (speed, pitch, voice)
        // that you would integrate with your DataStore settings.
    }
}
