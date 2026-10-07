package ua.acclorite.book_story.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.reader.ReadAloudAction
import ua.acclorite.book_story.domain.service.TextToSpeechService
import ua.acclorite.book_story.presentation.main.MainActivity
import javax.inject.Inject

@AndroidEntryPoint
class ReadAloudService : Service() {

    @Inject
    lateinit var readAloudNotificationManager: ReadAloudNotificationManager

    @Inject
    lateinit var textToSpeechService: TextToSpeechService

    @Inject
    lateinit var backgroundMusicPlayer: ReadAloudBackgroundMusicPlayer

    private var mediaSession: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isForeground = false

    private var currentBookTitle = ""
    private var currentParagraphText = ""
    private var isCurrentlyPlaying = false
    private var currentSpeed = 1.75f

    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private var pausedByTransientLoss = false

    private var becomingNoisyReceiver: BroadcastReceiver? = null
    private var isNoisyReceiverRegistered = false
    private var onModeChangedListener: AudioManager.OnModeChangedListener? = null

    companion object {
        const val CHANNEL_ID = "read_aloud_channel"
        const val NOTIFICATION_ID = 2026

        const val ACTION_UPDATE = "ua.acclorite.book_story.ACTION_UPDATE"
        const val ACTION_STOP = "ua.acclorite.book_story.ACTION_STOP"
        const val ACTION_NOTIFICATION_STOP = "ua.acclorite.book_story.ACTION_NOTIFICATION_STOP"
        const val ACTION_PLAY = "ua.acclorite.book_story.ACTION_PLAY"
        const val ACTION_PAUSE = "ua.acclorite.book_story.ACTION_PAUSE"
        const val ACTION_NEXT = "ua.acclorite.book_story.ACTION_NEXT"
        const val ACTION_PREV = "ua.acclorite.book_story.ACTION_PREV"

        const val EXTRA_BOOK_TITLE = "extra_book_title"
        const val EXTRA_PARAGRAPH_TEXT = "extra_paragraph_text"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_SPEED = "extra_speed"
        const val EXTRA_IS_USER_ACTION = "extra_is_user_action"
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        setupMediaSession()
        setupWakeLock()
        setupModeChangedListener()
    }

    private fun setupModeChangedListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val listener = AudioManager.OnModeChangedListener { mode ->
                Log.d("ReadAloudService", "AudioManager mode changed: $mode")
                when (mode) {
                    AudioManager.MODE_RINGTONE,
                    AudioManager.MODE_IN_CALL,
                    AudioManager.MODE_IN_COMMUNICATION -> {
                        if (isCurrentlyPlaying) {
                            Log.d("ReadAloudService", "Call/Ringtone mode active ($mode), pausing read-aloud")
                            pausedByTransientLoss = true
                            textToSpeechService.pause()
                            backgroundMusicPlayer.onReadAloudPause()
                            readAloudNotificationManager.emitAction(ReadAloudAction.PAUSE)
                        }
                    }
                    AudioManager.MODE_NORMAL -> {
                        if (pausedByTransientLoss && hasAudioFocus) {
                            Log.d("ReadAloudService", "Audio mode returned to NORMAL and has focus, resuming read-aloud")
                            pausedByTransientLoss = false
                            readAloudNotificationManager.emitAction(ReadAloudAction.PLAY)
                        }
                    }
                }
            }
            onModeChangedListener = listener
            try {
                audioManager.addOnModeChangedListener(mainExecutor, listener)
            } catch (e: Exception) {
                Log.e("ReadAloudService", "Failed to add OnModeChangedListener", e)
            }
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        Log.d("ReadAloudService", "onAudioFocusChange: $focusChange")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d("ReadAloudService", "AudioFocus permanent loss")
                pausedByTransientLoss = false
                hasAudioFocus = false
                textToSpeechService.pause()
                backgroundMusicPlayer.onReadAloudPause()
                readAloudNotificationManager.emitAction(ReadAloudAction.PAUSE)
                abandonAudioFocusInternal()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d("ReadAloudService", "AudioFocus transient loss: $focusChange")
                if (isCurrentlyPlaying) {
                    pausedByTransientLoss = true
                    textToSpeechService.pause()
                    backgroundMusicPlayer.onReadAloudPause()
                    readAloudNotificationManager.emitAction(ReadAloudAction.PAUSE)
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d("ReadAloudService", "AudioFocus gained. pausedByTransientLoss=$pausedByTransientLoss")
                hasAudioFocus = true
                if (pausedByTransientLoss) {
                    pausedByTransientLoss = false
                    backgroundMusicPlayer.onReadAloudPlay()
                    readAloudNotificationManager.emitAction(ReadAloudAction.PLAY)
                }
            }
        }
    }

    private fun requestAudioFocusInternal(): Boolean {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(false)
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()

        audioFocusRequest = focusRequest
        val result = audioManager.requestAudioFocus(focusRequest)
        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        Log.d("ReadAloudService", "requestAudioFocus result: $result, hasAudioFocus: $hasAudioFocus")
        return hasAudioFocus
    }

    private fun abandonAudioFocusInternal() {
        if (hasAudioFocus || audioFocusRequest != null) {
            audioFocusRequest?.let { request ->
                try {
                    audioManager.abandonAudioFocusRequest(request)
                } catch (e: Exception) {
                    Log.e("ReadAloudService", "Error abandoning audio focus", e)
                }
            }
            audioFocusRequest = null
            hasAudioFocus = false
        }
        unregisterBecomingNoisyReceiver()
    }

    private fun registerBecomingNoisyReceiver() {
        if (!isNoisyReceiverRegistered) {
            if (becomingNoisyReceiver == null) {
                becomingNoisyReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                            Log.d("ReadAloudService", "Audio becoming noisy, pausing read-aloud")
                            pausedByTransientLoss = false
                            textToSpeechService.pause()
                            backgroundMusicPlayer.onReadAloudPause()
                            readAloudNotificationManager.emitAction(ReadAloudAction.PAUSE)
                            abandonAudioFocusInternal()
                        }
                    }
                }
            }
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            try {
                ContextCompat.registerReceiver(
                    this,
                    becomingNoisyReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
                isNoisyReceiverRegistered = true
            } catch (e: Exception) {
                Log.e("ReadAloudService", "Error registering becomingNoisyReceiver", e)
            }
        }
    }

    private fun unregisterBecomingNoisyReceiver() {
        if (isNoisyReceiverRegistered) {
            try {
                unregisterReceiver(becomingNoisyReceiver)
            } catch (e: Exception) {
                Log.e("ReadAloudService", "Error unregistering becomingNoisyReceiver", e)
            }
            isNoisyReceiverRegistered = false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Read Aloud",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Book's Story Read Aloud Media Controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "BookStoryReadAloudSession").apply {
            isActive = true
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    pausedByTransientLoss = false
                    backgroundMusicPlayer.onReadAloudPlay()
                    readAloudNotificationManager.emitAction(ReadAloudAction.PLAY)
                }

                override fun onPause() {
                    pausedByTransientLoss = false
                    abandonAudioFocusInternal()
                    textToSpeechService.pause()
                    backgroundMusicPlayer.onReadAloudPause()
                    readAloudNotificationManager.emitAction(ReadAloudAction.PAUSE)
                }

                override fun onSkipToNext() {
                    readAloudNotificationManager.emitAction(ReadAloudAction.NEXT)
                }

                override fun onSkipToPrevious() {
                    readAloudNotificationManager.emitAction(ReadAloudAction.PREVIOUS)
                }

                override fun onStop() {
                    pausedByTransientLoss = false
                    abandonAudioFocusInternal()
                    textToSpeechService.stop()
                    backgroundMusicPlayer.onReadAloudStop()
                    readAloudNotificationManager.emitAction(ReadAloudAction.STOP)
                    stopServiceAndNotification()
                }
            })
        }
    }

    private fun setupWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "book_story:read_aloud_wakelock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE -> {
                currentBookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE) ?: currentBookTitle
                currentParagraphText = intent.getStringExtra(EXTRA_PARAGRAPH_TEXT) ?: currentParagraphText
                val newIsPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, isCurrentlyPlaying)
                currentSpeed = intent.getFloatExtra(EXTRA_SPEED, currentSpeed)
                val isExplicitUserAction = intent.getBooleanExtra(EXTRA_IS_USER_ACTION, false)

                if (!newIsPlaying) {
                    if (isExplicitUserAction || !pausedByTransientLoss) {
                        pausedByTransientLoss = false
                        abandonAudioFocusInternal()
                    }
                }

                isCurrentlyPlaying = newIsPlaying
                updateMediaPlayback()

                if (isCurrentlyPlaying) {
                    backgroundMusicPlayer.onReadAloudPlay()
                } else {
                    backgroundMusicPlayer.onReadAloudPause()
                }
            }
            ACTION_PLAY -> {
                pausedByTransientLoss = false
                backgroundMusicPlayer.onReadAloudPlay()
                readAloudNotificationManager.emitAction(ReadAloudAction.PLAY)
            }
            ACTION_PAUSE -> {
                pausedByTransientLoss = false
                abandonAudioFocusInternal()
                textToSpeechService.pause()
                backgroundMusicPlayer.onReadAloudPause()
                readAloudNotificationManager.emitAction(ReadAloudAction.PAUSE)
            }
            ACTION_NEXT -> {
                readAloudNotificationManager.emitAction(ReadAloudAction.NEXT)
            }
            ACTION_PREV -> {
                readAloudNotificationManager.emitAction(ReadAloudAction.PREVIOUS)
            }
            ACTION_NOTIFICATION_STOP -> {
                pausedByTransientLoss = false
                abandonAudioFocusInternal()
                textToSpeechService.stop()
                backgroundMusicPlayer.onReadAloudStop()
                readAloudNotificationManager.emitAction(ReadAloudAction.STOP)
                stopServiceAndNotification()
            }
            ACTION_STOP -> {
                // Programmatic stop from NotificationManager, do NOT re-emit action
                pausedByTransientLoss = false
                abandonAudioFocusInternal()
                textToSpeechService.stop()
                backgroundMusicPlayer.onReadAloudStop()
                stopServiceAndNotification()
            }
        }
        return START_NOT_STICKY
    }

    private fun updateMediaPlayback() {
        if (isCurrentlyPlaying) {
            if (!hasAudioFocus) {
                val granted = requestAudioFocusInternal()
                if (!granted) {
                    Log.w("ReadAloudService", "Audio focus request denied, pausing playback")
                    pausedByTransientLoss = false
                    textToSpeechService.pause()
                    readAloudNotificationManager.emitAction(ReadAloudAction.PAUSE)
                    return
                }
            }
            registerBecomingNoisyReceiver()
        }

        val session = mediaSession ?: return

        val state = if (isCurrentlyPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        val playbackState = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_STOP
            )
            .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, currentSpeed)
            .build()
        session.setPlaybackState(playbackState)

        val title = if (currentParagraphText.isNotBlank()) currentParagraphText else currentBookTitle
        val artist = if (currentParagraphText.isNotBlank()) currentBookTitle else "Book's Story"
        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, "Book's Story")
            .build()
        session.setMetadata(metadata)

        // WakeLock keeps CPU active when the screen turns off
        if (isCurrentlyPlaying) {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(30 * 60 * 1000L)
            }
        } else {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        }

        val notification = buildMediaNotification()

        if (isCurrentlyPlaying) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isForeground = true
        } else {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun buildMediaNotification(): Notification {
        val session = mediaSession ?: return Notification.Builder(this, CHANNEL_ID).build()

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ReadAloudService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, ReadAloudService::class.java).apply {
                action = if (isCurrentlyPlaying) ACTION_PAUSE else ACTION_PLAY
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, ReadAloudService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, ReadAloudService::class.java).apply { action = ACTION_NOTIFICATION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevAction = Notification.Action.Builder(
            android.R.drawable.ic_media_previous,
            "Previous",
            prevIntent
        ).build()

        val playPauseAction = Notification.Action.Builder(
            if (isCurrentlyPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (isCurrentlyPlaying) "Pause" else "Play",
            playPauseIntent
        ).build()

        val nextAction = Notification.Action.Builder(
            android.R.drawable.ic_media_next,
            "Next",
            nextIntent
        ).build()

        val stopAction = Notification.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop",
            stopIntent
        ).build()

        val mediaStyle = Notification.MediaStyle()
            .setMediaSession(session.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)

        val speedText = if (currentSpeed % 1.0f == 0.0f) "${currentSpeed.toInt()}x" else "${currentSpeed}x"

        return Notification.Builder(this, CHANNEL_ID)
            .setStyle(mediaStyle)
            .setSmallIcon(R.mipmap.app_icon)
            .setContentTitle(if (currentParagraphText.isNotBlank()) currentParagraphText else currentBookTitle)
            .setContentText(currentBookTitle)
            .setSubText(speedText)
            .setContentIntent(contentIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(isCurrentlyPlaying)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(stopAction)
            .build()
    }

    private fun stopServiceAndNotification() {
        pausedByTransientLoss = false
        abandonAudioFocusInternal()
        backgroundMusicPlayer.onReadAloudStop()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        mediaSession?.isActive = false
        if (isForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
        } else {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(NOTIFICATION_ID)
        }
        stopSelf()
    }

    override fun onDestroy() {
        pausedByTransientLoss = false
        abandonAudioFocusInternal()
        backgroundMusicPlayer.onReadAloudStop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            onModeChangedListener?.let {
                try {
                    audioManager.removeOnModeChangedListener(it)
                } catch (e: Exception) {
                    Log.e("ReadAloudService", "Error removing OnModeChangedListener", e)
                }
            }
        }
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
