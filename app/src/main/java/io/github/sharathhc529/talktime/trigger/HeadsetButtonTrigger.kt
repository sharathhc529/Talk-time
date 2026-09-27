package io.github.sharathhc529.talktime.trigger

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import io.github.sharathhc529.talktime.settings.ClickCount

/**
 * Receives headset and media button presses through a [MediaSession] and counts single, double
 * and triple clicks.
 *
 * Android sends media buttons to the app that played audio most recently, so while this is active
 * the buttons may not reach a music player that is paused.
 */
class HeadsetButtonTrigger(
    private val context: Context,
    /** The number of clicks that announces the time for a key code. */
    private val clicksFor: (keyCode: Int) -> ClickCount,
    /** How long to wait for another click of the key, in milliseconds. */
    private val clickDelayFor: (keyCode: Int) -> Int,
    private val onTrigger: () -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var session: MediaSession? = null

    private var pendingKey = 0
    private var clicks = 0
    private val evaluate = Runnable {
        if (clicks == clicksFor(pendingKey).clicks) onTrigger()
        clicks = 0
    }

    private val callback = object : MediaSession.Callback() {
        override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
            val event = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            } else {
                @Suppress("DEPRECATION")
                mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            } ?: return false
            val code = normalize(event.keyCode)
            if (clicksFor(code) == ClickCount.OFF) return false
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) onClick(code)
            return true
        }
    }

    val isActive: Boolean get() = session != null

    fun start() {
        if (session != null) return
        session = MediaSession(context, "TalkTimeHeadsetButtons").apply {
            setCallback(callback, handler)
            setPlaybackState(
                PlaybackState.Builder()
                    .setActions(
                        PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                            PlaybackState.ACTION_STOP or PlaybackState.ACTION_SKIP_TO_NEXT or
                            PlaybackState.ACTION_SKIP_TO_PREVIOUS,
                    )
                    .setState(PlaybackState.STATE_PAUSED, 0, 1f)
                    .build(),
            )
            isActive = true
        }
        playSilence()
    }

    fun stop() {
        handler.removeCallbacks(evaluate)
        session?.release()
        session = null
        clicks = 0
    }

    private fun onClick(keyCode: Int) {
        handler.removeCallbacks(evaluate)
        if (keyCode != pendingKey) clicks = 0
        pendingKey = keyCode
        clicks++
        if (clicks >= 3) evaluate.run() else handler.postDelayed(evaluate, clickDelayFor(keyCode).toLong())
    }

    /** Play and pause buttons of Bluetooth headsets are treated like the play/pause button. */
    private fun normalize(keyCode: Int) = when (keyCode) {
        KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        else -> keyCode
    }

    /**
     * Plays a short moment of silence, which makes this app the most recent audio player so that
     * Android routes the media buttons to this session.
     */
    private fun playSilence() {
        Thread {
            runCatching {
                val rate = 8000
                val samples = ShortArray(rate / 4)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(rate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(samples.size * 2)
                    .build()
                track.write(samples, 0, samples.size)
                track.play()
                Thread.sleep(400)
                track.release()
            }
        }.start()
    }
}
