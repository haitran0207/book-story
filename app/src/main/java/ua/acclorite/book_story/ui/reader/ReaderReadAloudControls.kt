package ua.acclorite.book_story.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.reader.model.ReadAloudState
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import ua.acclorite.book_story.ui.common.components.dialog.Dialog
import ua.acclorite.book_story.ui.common.components.settings.SliderWithTitle
import ua.acclorite.book_story.ui.common.components.settings.SwitchWithTitle
import ua.acclorite.book_story.ui.common.helpers.LocalSettings
import kotlin.math.roundToInt

@Composable
fun ReaderReadAloudControls(
    state: ReadAloudState,
    onEvent: (ReaderEvent) -> Unit
) {
    val settings = LocalSettings.current
    var showBgMusicDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play / Pause, Prev, Next & Stop Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { onEvent(ReaderEvent.OnPreviousReadAloudParagraph) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Paragraph",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledIconButton(
                onClick = {
                    if (state.isPlaying) {
                        onEvent(ReaderEvent.OnPauseReadAloud(isExplicitUserAction = true))
                    } else {
                        if (state.currentReadingIndex != null) {
                            onEvent(ReaderEvent.OnResumeReadAloud)
                        } else {
                            onEvent(ReaderEvent.OnStartReadAloud)
                        }
                    }
                },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (state.isPlaying) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = if (state.isPlaying) "Pause" else "Play"
                )
            }

            IconButton(
                onClick = { onEvent(ReaderEvent.OnNextReadAloudParagraph) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Paragraph",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { onEvent(ReaderEvent.OnStopReadAloud) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { showBgMusicDialog = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (settings.readAloudBgMusic.value) Icons.Default.MusicNote else Icons.Default.MusicOff,
                    contentDescription = stringResource(id = R.string.read_aloud_bg_music),
                    tint = if (settings.readAloudBgMusic.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Speed Controls: [-] [ 1.0x ] [+]
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = {
                    val newSpeed = ((state.speed - 0.25f) * 100).roundToInt() / 100f
                    val clamped = newSpeed.coerceIn(0.25f, 3.0f)
                    onEvent(ReaderEvent.OnChangeReadAloudSpeed(clamped))
                },
                modifier = Modifier.size(36.dp),
                enabled = state.speed > 0.25f
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease speed",
                    tint = if (state.speed > 0.25f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            // Clickable speed badge that also cycles common speeds on tap
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                        val currentIndex = speeds.indexOfFirst { kotlin.math.abs(it - state.speed) < 0.05f }
                        val nextSpeed = if (currentIndex in 0 until speeds.size - 1) {
                            speeds[currentIndex + 1]
                        } else {
                            speeds[0]
                        }
                        onEvent(ReaderEvent.OnChangeReadAloudSpeed(nextSpeed))
                    },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                val speedFormatted = if (state.speed % 1.0f == 0.0f) {
                    "${state.speed.toInt()}x"
                } else {
                    "${((state.speed * 100).roundToInt() / 100f)}x"
                }
                Text(
                    text = speedFormatted,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            IconButton(
                onClick = {
                    val newSpeed = ((state.speed + 0.25f) * 100).roundToInt() / 100f
                    val clamped = newSpeed.coerceIn(0.25f, 3.0f)
                    onEvent(ReaderEvent.OnChangeReadAloudSpeed(clamped))
                },
                modifier = Modifier.size(36.dp),
                enabled = state.speed < 3.0f
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase speed",
                    tint = if (state.speed < 3.0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }

    if (showBgMusicDialog) {
        Dialog(
            icon = Icons.Default.MusicNote,
            title = stringResource(id = R.string.read_aloud_bg_music),
            description = stringResource(id = R.string.read_aloud_bg_music_track),
            actionEnabled = true,
            onDismiss = { showBgMusicDialog = false },
            onAction = { showBgMusicDialog = false },
            withContent = true
        ) {
            item {
                SwitchWithTitle(
                    selected = settings.readAloudBgMusic.value,
                    title = stringResource(id = R.string.read_aloud_bg_music),
                    description = stringResource(id = R.string.read_aloud_bg_music_desc),
                    onClick = {
                        settings.readAloudBgMusic.update(!settings.readAloudBgMusic.lastValue)
                    }
                )
            }

            item {
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
        }
    }
}
