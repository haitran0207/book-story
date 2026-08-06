package ua.acclorite.book_story.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ua.acclorite.book_story.domain.reader.model.ReadAloudState
import ua.acclorite.book_story.presentation.reader.ReaderEvent

@Composable
fun ReaderReadAloudControls(
    state: ReadAloudState,
    onEvent: (ReaderEvent) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                if (state.isPlaying) {
                    onEvent(ReaderEvent.OnPauseReadAloud)
                } else {
                    onEvent(ReaderEvent.OnStartReadAloud)
                }
            }
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
            onClick = { onEvent(ReaderEvent.OnStopReadAloud) }
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop"
            )
        }
        
        Text(text = "Speed: ${state.speed}x")
    }
}
