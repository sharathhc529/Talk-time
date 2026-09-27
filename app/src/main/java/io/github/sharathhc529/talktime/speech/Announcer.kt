package io.github.sharathhc529.talktime.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import io.github.sharathhc529.talktime.settings.AudioFocusMode
import io.github.sharathhc529.talktime.settings.Keys
import io.github.sharathhc529.talktime.settings.Settings
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Speaks announcements with the system text-to-speech engine.
 *
 * Handles the optional intro sound, audio focus, the announcement volume that is independent of
 * the media volume, and skips announcements during calls or in a ringer mode the user excluded.
 */
class Announcer(context: Context, private val settings: Settings) {

    /** Why an announcement did not happen. */
    enum class Failure { BUSY, RINGER_MODE, IN_CALL, ENGINE_UNAVAILABLE, LANGUAGE_UNAVAILABLE }

    interface Listener {
        fun onAnnouncementFinished() {}
        fun onAnnouncementFailed(failure: Failure, language: SpeechLanguage) {}
    }

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val wakeLock = appContext.getSystemService(PowerManager::class.java)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TalkTime:Announcer")
        .apply { setReferenceCounted(false) }

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    var listener: Listener? = null

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pending: Request? = null
    private var introPlayer: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null

    private var speaking = false
    /** Incremented for every announcement, so stale callbacks of an earlier one are ignored. */
    private var generation = 0
    private var speakingSince = 0L
    private var savedVolume = -1
    private var appliedVolume = -1

    private data class Request(val text: String, val language: SpeechLanguage, val withIntroSound: Boolean)

    val isSpeaking: Boolean get() = speaking

    /** Starts the TTS engine early so the first announcement is not delayed. */
    fun warmUp() {
        if (tts == null) createEngine()
    }

    /** Announces the current time, as configured in the settings. */
    fun announceTime(): Boolean = speak(settings.phrase(), settings.language, withIntroSound = true)

    fun speak(text: String, language: SpeechLanguage = settings.language, withIntroSound: Boolean = false): Boolean {
        if (speaking && SystemClock.elapsedRealtime() - speakingSince < BUSY_TIMEOUT_MS) {
            Log.d(TAG, "Announcement already in progress")
            return false
        }
        if (speaking) finish(notify = false)
        if (!ringerModeAllowed()) return fail(Failure.RINGER_MODE, language)
        if (inCall()) return fail(Failure.IN_CALL, language)

        speaking = true
        generation++
        speakingSince = SystemClock.elapsedRealtime()
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
        requestFocus()
        applyVolume()

        val request = Request(text, language, withIntroSound)
        if (ttsReady) start(request) else {
            pending = request
            if (tts == null) createEngine()
        }
        return true
    }

    fun stop() {
        tts?.stop()
        finish()
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
    }

    private fun createEngine() {
        ttsReady = false
        tts = TextToSpeech(appContext) { status ->
            main.post {
                if (status == TextToSpeech.SUCCESS) {
                    tts?.setAudioAttributes(attributes)
                    tts?.setOnUtteranceProgressListener(progressListener)
                    ttsReady = true
                    pending?.let { pending = null; start(it) }
                } else {
                    Log.w(TAG, "Text-to-speech initialization failed: $status")
                    tts?.shutdown()
                    tts = null
                    pending?.let { pending = null; finishWithFailure(Failure.ENGINE_UNAVAILABLE, it.language) }
                }
            }
        }
    }

    private fun start(request: Request) {
        val engine = tts ?: return finishWithFailure(Failure.ENGINE_UNAVAILABLE, request.language)
        when (engine.setLanguage(request.language.ttsLocale)) {
            TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED ->
                return finishWithFailure(Failure.LANGUAGE_UNAVAILABLE, request.language)
        }
        val introUri = settings.introSoundUri
        if (request.withIntroSound && introUri != null) {
            playIntroSound(introUri) { speakNow(request) }
        } else {
            speakNow(request)
        }
    }

    /** Speaks each sentence with a short pause between them. */
    private fun speakNow(request: Request) {
        val engine = tts ?: return finishWithFailure(Failure.ENGINE_UNAVAILABLE, request.language)
        val params = Bundle()
        val parts = request.text.split('.').map { it.trim() }.filter { it.isNotEmpty() }
        parts.forEachIndexed { index, part ->
            engine.speak(part.take(TextToSpeech.getMaxSpeechInputLength()), TextToSpeech.QUEUE_ADD, params, "part$index")
            if (index < parts.lastIndex) engine.playSilentUtterance(SENTENCE_PAUSE_MS, TextToSpeech.QUEUE_ADD, "pause$index")
        }
        val result = engine.playSilentUtterance(END_PAUSE_MS, TextToSpeech.QUEUE_ADD, UTTERANCE_END)
        if (result != TextToSpeech.SUCCESS) finishWithFailure(Failure.ENGINE_UNAVAILABLE, request.language)
    }

    private fun playIntroSound(uri: android.net.Uri, then: () -> Unit) {
        releaseIntroPlayer()
        val owner = generation
        var started = false
        val proceed = Runnable {
            if (!started && speaking && owner == generation) {
                started = true
                releaseIntroPlayer()
                then()
            }
        }
        try {
            introPlayer = MediaPlayer().apply {
                setAudioAttributes(attributes)
                setDataSource(appContext, uri)
                setOnCompletionListener { main.post(proceed) }
                setOnErrorListener { _, _, _ -> main.post(proceed); true }
                prepare()
                start()
            }
            // Long ringtones would otherwise delay the announcement for minutes.
            main.postDelayed(proceed, INTRO_SOUND_MAX_MS)
        } catch (e: Exception) {
            Log.w(TAG, "Intro sound could not be played", e)
            proceed.run()
        }
    }

    private fun releaseIntroPlayer() {
        introPlayer?.let {
            runCatching { it.stop() }
            it.release()
        }
        introPlayer = null
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}

        override fun onDone(utteranceId: String?) {
            if (utteranceId == UTTERANCE_END) main.post { finish() }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            main.post { finish() }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            main.post { finish() }
        }
    }

    private fun fail(failure: Failure, language: SpeechLanguage): Boolean {
        Log.d(TAG, "Announcement skipped: $failure")
        listener?.onAnnouncementFailed(failure, language)
        return false
    }

    private fun finishWithFailure(failure: Failure, language: SpeechLanguage) {
        finish()
        fail(failure, language)
    }

    private fun finish(notify: Boolean = true) {
        if (!speaking) return
        speaking = false
        releaseIntroPlayer()
        restoreVolume()
        abandonFocus()
        if (wakeLock.isHeld) wakeLock.release()
        if (notify) listener?.onAnnouncementFinished()
    }

    private fun ringerModeAllowed() = audioManager.ringerMode in settings[Keys.ringerModes]

    private fun inCall() = audioManager.mode in setOf(
        AudioManager.MODE_IN_CALL,
        AudioManager.MODE_IN_COMMUNICATION,
        AudioManager.MODE_RINGTONE,
    )

    private fun requestFocus() {
        val gain = when (settings[Keys.audioFocus]) {
            AudioFocusMode.NONE -> return
            AudioFocusMode.DUCK -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            AudioFocusMode.PAUSE -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        }
        val request = AudioFocusRequest.Builder(gain)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { }
            .build()
        audioManager.requestAudioFocus(request)
        focusRequest = request
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun applyVolume() {
        if (!settings[Keys.independentVolume]) return
        val stream = AudioManager.STREAM_MUSIC
        val max = audioManager.getStreamMaxVolume(stream)
        savedVolume = audioManager.getStreamVolume(stream)
        appliedVolume = ceil(max * settings[Keys.volume] / 100.0).toInt().coerceIn(1, max)
        setStreamVolume(appliedVolume)
    }

    private fun restoreVolume() {
        if (savedVolume < 0) return
        val stream = AudioManager.STREAM_MUSIC
        val current = audioManager.getStreamVolume(stream)
        // Volume keys pressed during the announcement change the announcement volume setting.
        if (settings[Keys.volumeKeys] && current != appliedVolume) {
            val max = audioManager.getStreamMaxVolume(stream)
            settings[Keys.volume] = (current * 100.0 / max).roundToInt().coerceIn(1, 100)
        }
        setStreamVolume(savedVolume)
        savedVolume = -1
        appliedVolume = -1
    }

    private fun setStreamVolume(volume: Int) {
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
        } catch (e: SecurityException) {
            // Changing the volume is not allowed while "Do not disturb" is on.
            Log.w(TAG, "Could not change the volume", e)
        }
    }

    private companion object {
        const val TAG = "Announcer"
        const val UTTERANCE_END = "end"
        const val SENTENCE_PAUSE_MS = 500L
        const val END_PAUSE_MS = 300L
        const val BUSY_TIMEOUT_MS = 15_000L
        const val WAKE_LOCK_TIMEOUT_MS = 60_000L
        const val INTRO_SOUND_MAX_MS = 10_000L
    }
}
