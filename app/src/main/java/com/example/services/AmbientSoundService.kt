package com.example.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class AmbientSoundService : Service() {

    companion object {
        private const val TAG = "AmbientSoundService"
        private const val NOTIFICATION_ID = 4001
        private const val CHANNEL_ID = "soundscape_playback_channel"

        const val ACTION_PLAY = "com.example.action.PLAY"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_SET_VOLUME = "com.example.action.SET_VOLUME"

        const val EXTRA_TRACK_INDEX = "extra_track_index"
        const val EXTRA_VOLUME = "extra_volume"

        var isPlaying = false
            private set
        var currentTrackIndex = -1
            private set
        var currentVolume = 0.7f
            private set
    }

    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSessionCompat? = null

    // Replace the soundscapes titles so they match a file-based music feature
    private val soundscapes = listOf(
        Pair("Cosmic Binarual Waves", "Local File: cosmic_binarual_waves.mp3"),
        Pair("Cozy Rain Sound", "Local File: cozy_rain_sound.mp3"),
        Pair("Forest Ambient Meditation", "Local File: forest_ambient_meditation.mp3"),
        Pair("Indian Tanpura", "Local File: indian_tanpura.mp3"),
        Pair("Mantra OM", "Local File: mantra_om.mp3"),
        Pair("Night Sound", "Local File: night_sound.mp3")
    )
    
    private val fileNames = listOf(
        "cosmic_binarual_waves", "cozy_rain_sound", "forest_ambient_meditation", "indian_tanpura", "mantra_om", "night_sound"
    )

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaSession = MediaSessionCompat(this, TAG)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand: action = $action")

        when (action) {
            ACTION_PLAY -> {
                val index = intent.getIntExtra(EXTRA_TRACK_INDEX, 0)
                val vol = intent.getFloatExtra(EXTRA_VOLUME, currentVolume)
                playTrack(index, vol)
            }
            ACTION_PAUSE, ACTION_STOP -> {
                stopTrack()
                stopSelf()
            }
            ACTION_SET_VOLUME -> {
                val vol = intent.getFloatExtra(EXTRA_VOLUME, currentVolume)
                setVolumeLevel(vol)
            }
        }
        return START_NOT_STICKY
    }

    private fun playTrack(index: Int, vol: Float) {
        stopTrackInternal()
        currentTrackIndex = index
        currentVolume = vol
        isPlaying = true

        try {
            val fileName = fileNames.getOrNull(index) ?: return
            // Use reflection to get resource ID dynamically to avoid compilation errors if files don't exist yet
            val resId = applicationContext.resources.getIdentifier(fileName, "raw", applicationContext.packageName)
            if (resId != 0) {
                mediaPlayer = MediaPlayer.create(this, resId).apply {
                    isLooping = true
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setVolume(currentVolume, currentVolume)
                    start()
                }
            } else {
                Log.w(TAG, "Sound file not found: res/raw/$fileName. Please upload your audio file there!")
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer setup failure", e)
            isPlaying = false
            return
        }

        // Build notification and start foreground
        val trackTitle = soundscapes.getOrNull(index)?.first ?: "Local Soundscape File"
        startForeground(NOTIFICATION_ID, buildPlaybackNotification(trackTitle))
    }

    private fun stopTrack() {
        Log.d(TAG, "Stopping service audio playback completely")
        stopTrackInternal()
        currentTrackIndex = -1
        isPlaying = false
        stopForeground(true)
    }

    private fun stopTrackInternal() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            // Quiet ignore
        }
        mediaPlayer = null
    }

    private fun setVolumeLevel(vol: Float) {
        currentVolume = vol.coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(currentVolume, currentVolume)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply volume on MediaPlayer", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Soundscapes",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls and displays real-time ambient focus sounds playing in the background."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildPlaybackNotification(trackName: String): Notification {
        val stopIntent = Intent(this, AmbientSoundService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 101, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPending = PendingIntent.getActivity(this, 102, openAppIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(trackName)
            .setContentText("Devlance Studio")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openAppPending)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPending)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0)
            )
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTrackInternal()
        mediaSession?.release()
        super.onDestroy()
    }
}

