package io.github.sharathhc529.talktime.settings

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.text.format.DateFormat
import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.SpeakingInterval
import io.github.sharathhc529.talktime.speech.SpeechLanguage
import io.github.sharathhc529.talktime.speech.TimeStyle
import java.util.Locale

/** How many clicks of a headset button announce the time. */
enum class ClickCount(val clicks: Int) { OFF(0), SINGLE(1), DOUBLE(2), TRIPLE(3) }

enum class HourFormat { SYSTEM, H24, H12 }

/** Hour format of the on-screen clock; [AUDIO] follows the announcement settings. */
enum class DisplayHourFormat { AUDIO, H24, H12 }

enum class NightClockBehaviour { ANNOUNCE_AND_DISPLAY, ANNOUNCE, DISPLAY }

/** What happens to other audio apps during an announcement. */
enum class AudioFocusMode { NONE, DUCK, PAUSE }

enum class TextEffect { NONE, SHADOW, GLOW }

/** Designs of the main screen clock. */
enum class ClockStyle { FLIP, STACKED, GLASS, WORDS, RING, ANALOG }

enum class ScreenOrientation { SYSTEM, AUTOMATIC, PORTRAIT, LANDSCAPE }

/** Ringer modes, numbered like [android.media.AudioManager.RINGER_MODE_SILENT] etc. */
object RingerMode {
    const val SILENT = 0
    const val VIBRATE = 1
    const val NORMAL = 2
}

/** All user settings, stored in [SharedPreferences]. */
object Keys {
    // Control: touch and opening the app
    val touch = BoolPref("touch", true)
    val announceOnOpen = BoolPref("announce_on_open", false)
    val autoClose = BoolPref("auto_close", false)
    val autoCloseDelay = IntPref("auto_close_delay", 15, 3..120)

    // Control: headset buttons
    val wiredHook = EnumPref("headset_hook", ClickCount.OFF, ClickCount.entries.toTypedArray())
    val mediaPlayPause = EnumPref("headset_play_pause", ClickCount.OFF, ClickCount.entries.toTypedArray())
    val mediaStop = EnumPref("headset_stop", ClickCount.OFF, ClickCount.entries.toTypedArray())
    val mediaNext = EnumPref("headset_next", ClickCount.OFF, ClickCount.entries.toTypedArray())
    val mediaPrevious = EnumPref("headset_previous", ClickCount.OFF, ClickCount.entries.toTypedArray())
    val wiredClickDelay = IntPref("headset_wired_delay", 300, 100..1000)
    val mediaClickDelay = IntPref("headset_media_delay", 1200, 100..2000)

    // Control: proximity sensor
    val proximity = BoolPref("proximity", false)
    val proximitySwipes = IntPref("proximity_swipes", 3, 1..3)
    val proximityVibrate = BoolPref("proximity_vibrate", false)
    val proximityScreenOffOnly = BoolPref("proximity_screen_off_only", true)
    val proximitySpeed = IntPref("proximity_speed", 750, 10..1000)

    // Control: shaking
    val shake = BoolPref("shake", false)
    /** Acceleration in tenths of g. */
    val shakeSensitivity = IntPref("shake_sensitivity", 20, 11..40)
    val shakeCount = IntPref("shake_count", 3, 1..10)
    val shakeVibrate = BoolPref("shake_vibrate", false)

    // Control: power button
    val powerButton = BoolPref("power_button", false)
    val powerButtonDelay = IntPref("power_button_delay", 2, 1..5)

    // Control: night clock
    val nightClockOnCharger = BoolPref("night_clock_on_charger", false)
    val nightClockBehaviour = EnumPref(
        "night_clock_behaviour", NightClockBehaviour.ANNOUNCE_AND_DISPLAY, NightClockBehaviour.entries.toTypedArray(),
    )
    val nightClockNotification = BoolPref("night_clock_notification", true)
    val nightClockActive = BoolPref("night_clock_active", false)

    // Control: interval speaking clock
    val intervalActive = BoolPref("interval_active", false)
    val intervalNotification = BoolPref("interval_notification", true)
    val intervalSelectionDialog = BoolPref("interval_selection_dialog", true)
    val interval = EnumPref("interval", SpeakingInterval.EVERY_HOUR, SpeakingInterval.entries.toTypedArray())
    val intervalAlarmMode = BoolPref("interval_alarm_mode", true)
    val intervalAnnounceAtStart = BoolPref("interval_announce_at_start", true)
    val intervalOnHeadset = BoolPref("interval_on_headset", false)

    // Audio
    val language = OptionalEnumPref("language", SpeechLanguage.entries.toTypedArray())
    val style = EnumPref("style", TimeStyle.COMMON, TimeStyle.entries.toTypedArray())
    val hourFormat = EnumPref("hour_format", HourFormat.SYSTEM, HourFormat.entries.toTypedArray())
    val timeOfDay = BoolPref("time_of_day", false)
    val seconds = BoolPref("seconds", false)
    val introText = BoolPref("intro_text", true)
    val introSound = StringPref("intro_sound", "")
    val independentVolume = BoolPref("independent_volume", false)
    val volume = IntPref("volume", 50, 1..100)
    val volumeKeys = BoolPref("volume_keys", false)
    val audioFocus = EnumPref("audio_focus", AudioFocusMode.PAUSE, AudioFocusMode.entries.toTypedArray())
    val ringerModes = IntSetPref("ringer_modes", setOf(RingerMode.SILENT, RingerMode.VIBRATE, RingerMode.NORMAL))

    // Display
    val clockStyle = EnumPref("clock_style", ClockStyle.FLIP, ClockStyle.entries.toTypedArray())
    val textColor = IntPref("text_color", Color.WHITE)
    val textEffect = EnumPref("text_effect", TextEffect.NONE, TextEffect.entries.toTypedArray())
    val timeBold = BoolPref("time_bold", true)
    val amPmBold = BoolPref("am_pm_bold", false)
    val dateBold = BoolPref("date_bold", false)
    val backgroundColor = IntPref("background_color", Color.RED)
    val gradient = IntPref("gradient", 60, 0..100)
    val flipBackgroundColor = IntPref("flip_background_color", 0xFF121212.toInt())
    val cardColor = IntPref("card_color", 0xFF262626.toInt())
    val displayHourFormat = EnumPref("display_hour_format", DisplayHourFormat.AUDIO, DisplayHourFormat.entries.toTypedArray())
    val orientation = EnumPref("orientation", ScreenOrientation.SYSTEM, ScreenOrientation.entries.toTypedArray())

    // System
    val autostart = BoolPref("autostart", true)
    val ttsCheck = BoolPref("tts_check", true)
    val initialized = BoolPref("initialized", false)
    val closeDialogShown = BoolPref("close_dialog_shown", false)
}

class Settings(context: Context) {
    private val appContext = context.applicationContext
    val prefs: SharedPreferences = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    operator fun <T> get(pref: Pref<T>): T = prefs[pref]

    operator fun <T> set(pref: Pref<T>, value: T) {
        prefs[pref] = value
    }

    /** First start: pick formal 24-hour phrasing on devices that use the 24-hour clock. */
    fun initializeDefaults() {
        if (this[Keys.initialized]) return
        if (DateFormat.is24HourFormat(appContext)) this[Keys.style] = TimeStyle.FORMAL
        this[Keys.initialized] = true
    }

    /** The configured language, or the one matching the device locale. */
    val language: SpeechLanguage
        get() = this[Keys.language] ?: SpeechLanguage.forLocale(Locale.getDefault())

    val is24Hour: Boolean
        get() = when (this[Keys.hourFormat]) {
            HourFormat.SYSTEM -> DateFormat.is24HourFormat(appContext)
            HourFormat.H24 -> true
            HourFormat.H12 -> false
        }

    fun phraseOptions() = PhraseOptions(
        is24Hour = is24Hour,
        style = this[Keys.style],
        timeOfDay = this[Keys.timeOfDay],
        introText = this[Keys.introText],
        seconds = this[Keys.seconds],
    )

    fun phrase(time: ClockTime = ClockTime.now()): String = language.phrase(time, phraseOptions())

    /** Seconds are only announced in formal style, when the language supports them. */
    val secondsAvailable: Boolean
        get() = language.generator.supportsSeconds && this[Keys.style] == TimeStyle.FORMAL

    /** Whether the on-screen clock uses a 24-hour format. */
    val displayIs24Hour: Boolean
        get() = when (this[Keys.displayHourFormat]) {
            DisplayHourFormat.H24 -> true
            DisplayHourFormat.H12 -> false
            DisplayHourFormat.AUDIO -> this[Keys.style] == TimeStyle.FORMAL && is24Hour
        }

    val introSoundUri: Uri?
        get() = this[Keys.introSound].takeIf { it.isNotEmpty() }?.let(Uri::parse)

    fun clickCountFor(keyCode: Int): ClickCount = when (keyCode) {
        android.view.KeyEvent.KEYCODE_HEADSETHOOK -> this[Keys.wiredHook]
        android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        android.view.KeyEvent.KEYCODE_MEDIA_PLAY,
        android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> this[Keys.mediaPlayPause]
        android.view.KeyEvent.KEYCODE_MEDIA_STOP -> this[Keys.mediaStop]
        android.view.KeyEvent.KEYCODE_MEDIA_NEXT -> this[Keys.mediaNext]
        android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS -> this[Keys.mediaPrevious]
        else -> ClickCount.OFF
    }

    val headsetButtonsEnabled: Boolean
        get() = listOf(Keys.wiredHook, Keys.mediaPlayPause, Keys.mediaStop, Keys.mediaNext, Keys.mediaPrevious)
            .any { this[it] != ClickCount.OFF }

    /** Whether any feature needs the background service to keep running. */
    val needsBackgroundService: Boolean
        get() = this[Keys.intervalActive] || this[Keys.nightClockActive] || this[Keys.nightClockOnCharger] ||
            this[Keys.proximity] || this[Keys.shake] || this[Keys.powerButton] ||
            this[Keys.intervalOnHeadset] || headsetButtonsEnabled
}
