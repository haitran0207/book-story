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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.domain.reader.model.ReadAloudState
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import kotlin.math.roundToInt

@Composable
fun ReaderReadAloudControls(
    state: ReadAloudState,
    onEvent: (ReaderEvent) -> Unit
) {
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
                        onEvent(ReaderEvent.OnPauseReadAloud)
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
}
