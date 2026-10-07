package ua.acclorite.book_story.data.service

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ua.acclorite.book_story.R
import ua.acclorite.book_story.data.settings.SettingsManager
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ReadAloudBgMusicPlayer"

@Singleton
class ReadAloudBackgroundMusicPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var mediaPlayer: MediaPlayer? = null

    @Volatile
    private var shouldBePlaying = false

    init {
        scope.launch {
            settingsManager.readAloudBgMusic.flow.collectLatest { enabled ->
                if (!enabled) {
                    pauseInternal()
                } else if (shouldBePlaying) {
                    startInternal()
                }
            }
        }

        scope.launch {
            settingsManager.readAloudBgMusicVolume.flow.collectLatest { volumePercent ->
                applyVolume(volumePercent)
            }
        }
    }

    @Synchronized
    fun onReadAloudPlay() {
        shouldBePlaying = true
        if (settingsManager.readAloudBgMusic.lastValue) {
            startInternal()
        }
    }

    @Synchronized
    fun onReadAloudPause() {
        shouldBePlaying = false
        pauseInternal()
    }

    @Synchronized
    fun onReadAloudStop() {
        shouldBePlaying = false
        stopInternal()
    }

    private fun startInternal() {
        scope.launch {
            try {
                if (mediaPlayer == null) {
                    val afd: AssetFileDescriptor = try {
                        context.resources.openRawResourceFd(R.raw.read_aloud_bg_music)
                    } catch (e: Exception) {
                        Log.e(TAG, "Cannot open raw resource read_aloud_bg_music", e)
                        return@launch
                    }

                    val player = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                        isLooping = true
                        setOnErrorListener { _, what, extra ->
                            Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                            releasePlayer()
                            false
                        }
                        prepare()
                    }

                    mediaPlayer = player
                    applyVolume(settingsManager.readAloudBgMusicVolume.lastValue)
                }

                mediaPlayer?.let { player ->
                    if (!player.isPlaying && shouldBePlaying && settingsManager.readAloudBgMusic.lastValue) {
                        applyVolume(settingsManager.readAloudBgMusicVolume.lastValue)
                        player.start()
                        Log.d(TAG, "Background music started/resumed successfully")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize or start background music MediaPlayer", e)
                releasePlayer()
            }
        }
    }

    private fun pauseInternal() {
        scope.launch {
            try {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.pause()
                        Log.d(TAG, "Background music paused successfully")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pause background music", e)
            }
        }
    }

    private fun stopInternal() {
        scope.launch {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release MediaPlayer", e)
        } finally {
            mediaPlayer = null
            Log.d(TAG, "Background music MediaPlayer released")
        }
    }

    private fun applyVolume(volumePercent: Int) {
        try {
            val vol = (volumePercent.coerceIn(0, 100) / 100f)
            mediaPlayer?.setVolume(vol, vol)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set volume: $volumePercent", e)
        }
    }
}
