package ua.acclorite.book_story.data.service

import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ua.acclorite.book_story.domain.model.reader.ReadAloudAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadAloudNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val _actionEvents = MutableSharedFlow<ReadAloudAction>(extraBufferCapacity = 1)
    val actionEvents = _actionEvents.asSharedFlow()

    fun emitAction(action: ReadAloudAction) {
        scope.launch {
            _actionEvents.emit(action)
        }
    }

    fun update(bookTitle: String, paragraphText: String, isPlaying: Boolean, speed: Float) {
        try {
            val intent = Intent(context, ReadAloudService::class.java).apply {
                action = ReadAloudService.ACTION_UPDATE
                putExtra(ReadAloudService.EXTRA_BOOK_TITLE, bookTitle)
                putExtra(ReadAloudService.EXTRA_PARAGRAPH_TEXT, paragraphText)
                putExtra(ReadAloudService.EXTRA_IS_PLAYING, isPlaying)
                putExtra(ReadAloudService.EXTRA_SPEED, speed)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPlaying) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            val intent = Intent(context, ReadAloudService::class.java).apply {
                action = ReadAloudService.ACTION_STOP
            }
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
