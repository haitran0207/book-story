package ua.acclorite.book_story.ui.settings.reader.read_aloud.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ua.acclorite.book_story.R
import ua.acclorite.book_story.ui.common.components.settings.SwitchWithTitle
import ua.acclorite.book_story.ui.common.helpers.LocalSettings

@Composable
fun ReadAloudBgMusicOption() {
    val settings = LocalSettings.current

    SwitchWithTitle(
        selected = settings.readAloudBgMusic.value,
        title = stringResource(id = R.string.read_aloud_bg_music),
        description = stringResource(id = R.string.read_aloud_bg_music_desc),
        onClick = {
            settings.readAloudBgMusic.update(!settings.readAloudBgMusic.lastValue)
        }
    )
}
