package ua.acclorite.book_story.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import dagger.hilt.android.AndroidEntryPoint
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.reader.ReadAloudAction
import ua.acclorite.book_story.presentation.main.MainActivity
import javax.inject.Inject

@AndroidEntryPoint
class ReadAloudService : Service() {

    @Inject
    lateinit var readAloudNotificationManager: ReadAloudNotificationManager

    private var mediaSession: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isForeground = false

    private var currentBookTitle = ""
    private var currentParagraphText = ""
    private var isCurrentlyPlaying = false
    private var currentSpeed = 1.75f

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
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
        setupWakeLock()
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
                    readAloudNotificationManager.emitAction(ReadAloudAction.PLAY)
                }

                override fun onPause() {
                    readAloudNotificationManager.emitAction(ReadAloudAction.PAUSE)
                }

                override fun onSkipToNext() {
                    readAloudNotificationManager.emitAction(ReadAloudAction.NEXT)
                }

                override fun onSkipToPrevious() {
                    readAloudNotificationManager.emitAction(ReadAloudAction.PREVIOUS)
                }

                override fun onStop() {
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
                isCurrentlyPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, isCurrentlyPlaying)
                currentSpeed = intent.getFloatExtra(EXTRA_SPEED, currentSpeed)

                updateMediaPlayback()
            }
            ACTION_PLAY -> {
                readAloudNotificationManager.emitAction(ReadAloudAction.PLAY)
            }
            ACTION_PAUSE -> {
                readAloudNotificationManager.emitAction(ReadAloudAction.PAUSE)
            }
            ACTION_NEXT -> {
                readAloudNotificationManager.emitAction(ReadAloudAction.NEXT)
            }
            ACTION_PREV -> {
                readAloudNotificationManager.emitAction(ReadAloudAction.PREVIOUS)
            }
            ACTION_NOTIFICATION_STOP -> {
                readAloudNotificationManager.emitAction(ReadAloudAction.STOP)
                stopServiceAndNotification()
            }
            ACTION_STOP -> {
                // Programmatic stop from NotificationManager, do NOT re-emit action
                stopServiceAndNotification()
            }
        }
        return START_NOT_STICKY
    }

    private fun updateMediaPlayback() {
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
