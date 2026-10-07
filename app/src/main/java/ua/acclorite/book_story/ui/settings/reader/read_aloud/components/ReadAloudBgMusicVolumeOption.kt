package ua.acclorite.book_story.ui.settings.reader.read_aloud.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ua.acclorite.book_story.R
import ua.acclorite.book_story.ui.common.components.settings.SliderWithTitle
import ua.acclorite.book_story.ui.common.helpers.LocalSettings

@Composable
fun ReadAloudBgMusicVolumeOption() {
    val settings = LocalSettings.current

    SliderWithTitle(
        value = Pair(settings.readAloudBgMusicVolume.value, "%"),
        fromValue = 0,
        toValue = 100,
        title = stringResource(id = R.string.read_aloud_bg_music_volume),
        onValueChange = { volume ->
            settings.readAloudBgMusicVolume.update(volume)
        }
    )
}
