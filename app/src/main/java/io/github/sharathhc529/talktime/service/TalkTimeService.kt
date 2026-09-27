package io.github.sharathhc529.talktime.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.sharathhc529.talktime.R
import io.github.sharathhc529.talktime.settings.Keys
import io.github.sharathhc529.talktime.settings.NightClockBehaviour
import io.github.sharathhc529.talktime.settings.Settings
import io.github.sharathhc529.talktime.speech.Announcer
import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.SpeakingInterval
import io.github.sharathhc529.talktime.speech.SpeechLanguage
import io.github.sharathhc529.talktime.trigger.ChargerMonitor
import io.github.sharathhc529.talktime.trigger.HeadsetButtonTrigger
import io.github.sharathhc529.talktime.trigger.HeadsetMonitor
import io.github.sharathhc529.talktime.trigger.PowerButtonDetector
import io.github.sharathhc529.talktime.trigger.ProximityTrigger
import io.github.sharathhc529.talktime.trigger.ScreenMonitor
import io.github.sharathhc529.talktime.trigger.ShakeTrigger
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs

/**
 * Speaks the time and runs everything that can trigger an announcement in the background: the
 * interval speaking clock, the night clock, the proximity sensor, shaking, the power button and
 * headset buttons.
 *
 * Activities bind to it to announce the time. While a background feature is enabled it runs as a
 * foreground service with a status notification.
 */
class TalkTimeService : Service(), SharedPreferences.OnSharedPreferenceChangeListener, Announcer.Listener {

    inner class LocalBinder : Binder() {
        val service: TalkTimeService get() = this@TalkTimeService
    }

    private val binder = LocalBinder()
    private lateinit var settings: Settings
    private lateinit var announcer: Announcer
    private lateinit var notifications: Notifications
    private lateinit var scheduler: IntervalScheduler

    private lateinit var proximity: ProximityTrigger
    private lateinit var shake: ShakeTrigger
    private lateinit var screen: ScreenMonitor
    private lateinit var charger: ChargerMonitor
    private lateinit var headset: HeadsetMonitor
    private lateinit var headsetButtons: HeadsetButtonTrigger
    private val powerButton = PowerButtonDetector { announceTime() }

    private var foreground = false

    val isIntervalActive: Boolean get() = settings[Keys.intervalActive]
    val isNightClockActive: Boolean get() = settings[Keys.nightClockActive]
    val hasProximitySensor: Boolean get() = proximity.isAvailable

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = Settings(this)
        settings.initializeDefaults()
        notifications = Notifications(this)
        announcer = Announcer(this, settings).also { it.listener = this }
        scheduler = IntervalScheduler(this)
        proximity = ProximityTrigger(this) { onProximityGesture() }
        shake = ShakeTrigger(this) { announceTime() }
        screen = ScreenMonitor(this) { on -> onScreenChanged(on) }
        charger = ChargerMonitor(this) { connected -> onChargerChanged(connected) }
        headset = HeadsetMonitor(this) { connected -> onHeadsetChanged(connected) }
        headsetButtons = HeadsetButtonTrigger(
            this,
            clicksFor = settings::clickCountFor,
            clickDelayFor = { code ->
                if (code == KeyEvent.KEYCODE_HEADSETHOOK) settings[Keys.wiredClickDelay] else settings[Keys.mediaClickDelay]
            },
            onTrigger = { announceTime() },
        )
        settings.prefs.registerOnSharedPreferenceChangeListener(this)
        announcer.warmUp()
        if (settings[Keys.intervalActive]) scheduleInterval()
        applyTriggers()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Started with startForegroundService(): the notification must be shown right away.
        enterForeground()
        handle(intent)
        return START_STICKY
    }

    private fun handle(intent: Intent?) {
        when (intent?.action) {
            ACTION_ANNOUNCE -> announceTime()
            ACTION_INTERVAL_ALARM -> onIntervalAlarm(intent.getLongExtra(IntervalAlarmReceiver.EXTRA_SCHEDULED_AT, 0L))
            ACTION_STOP_INTERVAL -> stopInterval()
            ACTION_STOP_NIGHT_CLOCK -> stopNightClock()
        }
        updateForeground()
    }

    override fun onDestroy() {
        settings.prefs.unregisterOnSharedPreferenceChangeListener(this)
        // Closing the app stops the speaking clock until the app is opened again.
        scheduler.cancel()
        stopTriggers()
        announcer.release()
        notifications.cancelNightClockWake()
        if (instance === this) instance = null
        super.onDestroy()
    }

    // region Announcements

    fun announceTime(): Boolean = announcer.announceTime()

    /** Says the welcome message followed by the time. */
    fun announceWelcome(): Boolean {
        val language = settings.language
        return announcer.speak(language.welcome + " " + settings.phrase(), language, withIntroSound = true)
    }

    override fun onAnnouncementFailed(failure: Announcer.Failure, language: SpeechLanguage) {
        when (failure) {
            Announcer.Failure.LANGUAGE_UNAVAILABLE -> notifications.showVoiceMissing(language)
            Announcer.Failure.ENGINE_UNAVAILABLE ->
                Toast.makeText(this, R.string.app_tts_install_title, Toast.LENGTH_LONG).show()
            else -> {}
        }
    }

    // endregion

    // region Interval speaking clock

    fun startInterval(interval: SpeakingInterval = settings[Keys.interval], announce: Boolean = true) {
        settings[Keys.interval] = interval
        settings[Keys.intervalActive] = true
        scheduleInterval()
        if (announce && settings[Keys.intervalAnnounceAtStart]) {
            val text = settings.phrase() + ". " + getString(R.string.pref_list_title_pulse_interval) + " " +
                getString(intervalName(interval))
            announcer.speak(text)
        }
        ensureRunning()
    }

    fun stopInterval() {
        settings[Keys.intervalActive] = false
        scheduler.cancel()
        updateForeground()
    }

    private fun scheduleInterval() {
        scheduler.scheduleNext(settings[Keys.interval], settings[Keys.intervalAlarmMode])
    }

    private fun onIntervalAlarm(scheduledAt: Long) {
        if (!settings[Keys.intervalActive]) return
        val now = System.currentTimeMillis()
        // Announce the scheduled time unless the alarm was delivered late.
        val time = if (scheduledAt != 0L && abs(now - scheduledAt) < ON_TIME_TOLERANCE_MS) {
            ClockTime.of(LocalDateTime.ofInstant(Instant.ofEpochMilli(scheduledAt), ZoneId.systemDefault()))
        } else {
            ClockTime.now()
        }
        announcer.speak(settings.phrase(time), settings.language, withIntroSound = true)
        scheduler.scheduleNext(
            settings[Keys.interval],
            settings[Keys.intervalAlarmMode],
            LocalDateTime.ofInstant(Instant.ofEpochMilli(maxOf(now, scheduledAt)), ZoneId.systemDefault()),
        )
    }

    private fun onHeadsetChanged(connected: Boolean) {
        if (!settings[Keys.intervalOnHeadset]) return
        if (connected && !settings[Keys.intervalActive]) {
            startInterval()
        } else if (!connected && settings[Keys.intervalActive]) {
            stopInterval()
        }
    }

    // endregion

    // region Night clock

    fun startNightClock() {
        settings[Keys.nightClockActive] = true
        ensureRunning()
    }

    fun stopNightClock() {
        settings[Keys.nightClockActive] = false
        notifications.cancelNightClockWake()
        updateForeground()
    }

    private fun onChargerChanged(connected: Boolean) {
        if (!settings[Keys.nightClockOnCharger]) return
        if (connected) startNightClock() else if (settings[Keys.nightClockActive]) stopNightClock()
    }

    private fun onProximityGesture() {
        if (!settings[Keys.nightClockActive]) {
            announceTime()
            return
        }
        val behaviour = settings[Keys.nightClockBehaviour]
        if (behaviour != NightClockBehaviour.ANNOUNCE) showNightClock()
        if (behaviour != NightClockBehaviour.DISPLAY) announceTime()
    }

    /** Wakes the screen and shows the clock over the lock screen. */
    private fun showNightClock() {
        @Suppress("DEPRECATION") // There is no other way for a service to switch the screen on.
        val wake = getSystemService(PowerManager::class.java).newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "TalkTime:NightClock",
        )
        wake.acquire(NIGHT_CLOCK_WAKE_MS)
        notifications.showNightClockWake()
    }

    // endregion

    // region Triggers

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        when (key) {
            Keys.intervalAlarmMode.key, Keys.interval.key -> if (settings[Keys.intervalActive]) scheduleInterval()
            Keys.intervalActive.key -> if (settings[Keys.intervalActive]) scheduleInterval() else scheduler.cancel()
            Keys.nightClockActive.key -> if (!settings[Keys.nightClockActive]) notifications.cancelNightClockWake()
        }
        applyTriggers()
        if (settings.needsBackgroundService) ensureRunning() else updateForeground()
    }

    /** Starts or stops each trigger according to the settings. */
    private fun applyTriggers() {
        val nightClock = settings[Keys.nightClockActive]
        val proximityWanted = settings[Keys.proximity] || nightClock
        // The night clock always reacts to a single swipe, and only while the screen is off.
        proximity.swipes = if (nightClock) 1 else settings[Keys.proximitySwipes]
        proximity.speedMs = settings[Keys.proximitySpeed]
        proximity.vibrate = !nightClock && settings[Keys.proximityVibrate]
        val proximityScreenOffOnly = nightClock || settings[Keys.proximityScreenOffOnly]

        powerButton.delaySeconds = settings[Keys.powerButtonDelay]
        val screenWanted = settings[Keys.powerButton] || (proximityWanted && proximityScreenOffOnly)
        if (screenWanted) screen.start() else screen.stop()

        if (proximityWanted && (!proximityScreenOffOnly || !screen.isScreenOn)) proximity.start() else proximity.stop()

        shake.thresholdG = settings[Keys.shakeSensitivity] / 10f
        shake.count = settings[Keys.shakeCount]
        shake.vibrate = settings[Keys.shakeVibrate]
        if (settings[Keys.shake]) shake.start() else shake.stop()

        if (settings[Keys.nightClockOnCharger]) charger.start() else charger.stop()
        if (settings[Keys.intervalOnHeadset]) headset.start() else headset.stop()
        if (settings.headsetButtonsEnabled) headsetButtons.start() else headsetButtons.stop()
    }

    private fun stopTriggers() {
        proximity.stop()
        shake.stop()
        screen.stop()
        charger.stop()
        headset.stop()
        headsetButtons.stop()
    }

    private fun onScreenChanged(screenOn: Boolean) {
        if (settings[Keys.powerButton]) powerButton.onScreenChanged(screenOn)
        if (screenOn) notifications.cancelNightClockWake()
        applyTriggers()
    }

    // endregion

    // region Foreground state

    /** Makes sure the service keeps running in the background while a feature needs it. */
    private fun ensureRunning() {
        if (!settings.needsBackgroundService) return
        if (!foreground) {
            try {
                ContextCompat.startForegroundService(this, Intent(this, TalkTimeService::class.java))
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Could not start the service in the foreground", e)
            }
        } else {
            updateForeground()
        }
    }

    private fun enterForeground() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        try {
            ServiceCompat.startForeground(this, Notifications.STATUS_ID, notifications.status(statusState()), type)
            foreground = true
        } catch (e: Exception) {
            Log.w(TAG, "Could not enter the foreground", e)
        }
    }

    private fun updateForeground() {
        if (!foreground) return
        if (settings.needsBackgroundService) {
            notifications.updateStatus(statusState())
        } else if (!announcer.isSpeaking) {
            leaveForeground()
        }
    }

    private fun leaveForeground() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        foreground = false
        stopSelf()
    }

    override fun onAnnouncementFinished() {
        // A one-off announcement (e.g. from an alarm after the feature was switched off) is done.
        if (foreground && !settings.needsBackgroundService) leaveForeground()
    }

    private fun statusState() = Notifications.StatusState(
        interval = if (settings[Keys.intervalActive] && settings[Keys.intervalNotification]) settings[Keys.interval] else null,
        nightClock = settings[Keys.nightClockActive] && settings[Keys.nightClockNotification],
    )

    // endregion

    companion object {
        private const val TAG = "TalkTimeService"
        const val ACTION_ANNOUNCE = "io.github.sharathhc529.talktime.ANNOUNCE"
        const val ACTION_INTERVAL_ALARM = "io.github.sharathhc529.talktime.INTERVAL"
        const val ACTION_STOP_INTERVAL = "io.github.sharathhc529.talktime.STOP_INTERVAL"
        const val ACTION_STOP_NIGHT_CLOCK = "io.github.sharathhc529.talktime.STOP_NIGHT_CLOCK"
        private const val ON_TIME_TOLERANCE_MS = 5_000L
        private const val NIGHT_CLOCK_WAKE_MS = 10_000L

        /** The running service, if any (same process only). */
        @Volatile
        private var instance: TalkTimeService? = null

        /**
         * Sends an intent to the service: directly if it is already running, otherwise by starting
         * it as a foreground service.
         */
        fun deliver(context: Context, intent: Intent) {
            val running = instance
            if (running != null) {
                running.handle(intent)
                return
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Service could not be started from the background", e)
            }
        }

        /**
         * Starts the service in the background if the settings need it. Called from the app's
         * screens, which are in the foreground and therefore allowed to start it.
         */
        fun refresh(context: Context) {
            if (instance == null && Settings(context).needsBackgroundService) {
                deliver(context, Intent(context, TalkTimeService::class.java))
            }
        }

        fun intervalName(interval: SpeakingInterval): Int = when (interval) {
            SpeakingInterval.EVERY_MINUTE -> R.string.app_pulse_generator_interval_1
            SpeakingInterval.EVERY_EVEN_MINUTE -> R.string.app_pulse_generator_interval_2
            SpeakingInterval.EVERY_ODD_MINUTE -> R.string.app_pulse_generator_interval_3
            SpeakingInterval.EVERY_5_MINUTES -> R.string.app_pulse_generator_interval_4
            SpeakingInterval.EVERY_10_MINUTES -> R.string.app_pulse_generator_interval_5
            SpeakingInterval.EVERY_15_MINUTES -> R.string.app_pulse_generator_interval_6
            SpeakingInterval.EVERY_20_MINUTES -> R.string.app_pulse_generator_interval_7
            SpeakingInterval.EVERY_30_MINUTES -> R.string.app_pulse_generator_interval_8
            SpeakingInterval.EVERY_HOUR -> R.string.app_pulse_generator_interval_9
            SpeakingInterval.EVERY_15_SECONDS -> R.string.app_pulse_generator_interval_10
            SpeakingInterval.EVERY_20_SECONDS -> R.string.app_pulse_generator_interval_11
            SpeakingInterval.EVERY_30_SECONDS -> R.string.app_pulse_generator_interval_12
        }
    }
}
