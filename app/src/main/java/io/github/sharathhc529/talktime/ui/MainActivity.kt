package io.github.sharathhc529.talktime.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.sharathhc529.talktime.ui.clock.ClockScreen
import io.github.sharathhc529.talktime.R
import io.github.sharathhc529.talktime.service.Notifications
import io.github.sharathhc529.talktime.service.TalkTimeService
import io.github.sharathhc529.talktime.settings.Keys
import io.github.sharathhc529.talktime.settings.ScreenOrientation
import io.github.sharathhc529.talktime.settings.Settings
import io.github.sharathhc529.talktime.settings.collect
import io.github.sharathhc529.talktime.speech.SpeakingInterval
import io.github.sharathhc529.talktime.ui.settings.SettingsActivity
import io.github.sharathhc529.talktime.ui.theme.TalkTimeTheme

/** The full-screen clock. Touching it announces the time. */
class MainActivity : ComponentActivity() {

    private lateinit var settings: Settings
    private var service by mutableStateOf<TalkTimeService?>(null)
    private var bound = false
    private val handler = Handler(Looper.getMainLooper())

    /** Pending action once the service is connected. */
    private var announceWhenConnected = false
    private var welcomeWhenConnected = false

    private var dialog by mutableStateOf<Dialog?>(null)

    private enum class Dialog { CLOSE, INFO, INTERVAL, TTS_MISSING }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val connected = (binder as TalkTimeService.LocalBinder).service
            service = connected
            if (welcomeWhenConnected) connected.announceWelcome()
            else if (announceWhenConnected) connected.announceTime()
            welcomeWhenConnected = false
            announceWhenConnected = false
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    private val autoClose = Runnable { finish() }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        settings = Settings(this)
        settings.initializeDefaults()
        hideSystemBars()

        if (savedInstanceState == null) {
            if (settings[Keys.announceOnOpen]) {
                announceWhenConnected = true
                if (settings[Keys.autoClose] && !settings[Keys.nightClockActive]) {
                    handler.postDelayed(autoClose, settings[Keys.autoCloseDelay] * 1000L)
                }
            }
            checkTextToSpeech()
            requestNotificationPermission()
        }
        handleIntent(intent)

        setContent {
            TalkTimeTheme(darkTheme = true) {
                val nightClock by settings.collect(Keys.nightClockActive)
                LaunchedEffect(nightClock) { applyNightClockWindow(nightClock) }
                ClockScreen(
                    settings = settings,
                    onTap = {
                        cancelAutoClose()
                        if (settings[Keys.touch]) announce()
                    },
                    onSpeak = {
                        cancelAutoClose()
                        announce()
                    },
                    overlay = { tint -> Menu(tint) },
                )
                Dialogs()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        applyOrientation()
        bound = bindService(Intent(this, TalkTimeService::class.java), connection, BIND_AUTO_CREATE)
        TalkTimeService.refresh(this)
    }

    override fun onStop() {
        if (bound) {
            unbindService(connection)
            bound = false
        }
        service = null
        super.onStop()
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoClose)
        super.onDestroy()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_NIGHT_CLOCK, false) == true) {
            Notifications(this).cancelNightClockWake()
        }
    }

    private fun announce() {
        val connected = service
        if (connected != null) connected.announceTime() else announceWhenConnected = true
    }

    private fun cancelAutoClose() = handler.removeCallbacks(autoClose)

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /** While the night clock runs, the clock is shown over the lock screen and switches the screen on. */
    private fun applyNightClockWindow(active: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(active)
            setTurnScreenOn(active)
        } else {
            @Suppress("DEPRECATION")
            val flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            if (active) window.addFlags(flags) else window.clearFlags(flags)
        }
    }

    private fun applyOrientation() {
        requestedOrientation = when (settings[Keys.orientation]) {
            ScreenOrientation.SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_USER
            ScreenOrientation.AUTOMATIC -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    /** On the first starts, checks that a text-to-speech engine is installed, then says hello. */
    private fun checkTextToSpeech() {
        if (!settings[Keys.ttsCheck]) return
        val engines = packageManager.queryIntentServices(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0)
        if (engines.isEmpty()) {
            dialog = Dialog.TTS_MISSING
        } else {
            settings[Keys.ttsCheck] = false
            welcomeWhenConnected = true
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startSpeakingClock(interval: SpeakingInterval) {
        val connected = service ?: return
        connected.startInterval(interval)
    }

    private fun closeApp() {
        stopService(Intent(this, TalkTimeService::class.java))
        finishAndRemoveTask()
    }

    @Composable
    private fun Menu(tint: Color) {
        var expanded by remember { mutableStateOf(false) }
        val connected = service
        val intervalActive by settings.collect(Keys.intervalActive)
        val nightClockActive by settings.collect(Keys.nightClockActive)
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            Box(Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { cancelAutoClose(); expanded = true }) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.menu), tint = tint)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    if (connected?.hasProximitySensor != false) {
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(if (nightClockActive) R.string.app_stop_night_clock else R.string.app_start_night_clock))
                            },
                            onClick = {
                                expanded = false
                                if (nightClockActive) connected?.stopNightClock() else connected?.startNightClock()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(if (intervalActive) R.string.app_stop_speaking_clock else R.string.app_start_speaking_clock))
                        },
                        onClick = {
                            expanded = false
                            when {
                                intervalActive -> connected?.stopInterval()
                                settings[Keys.intervalSelectionDialog] -> dialog = Dialog.INTERVAL
                                else -> startSpeakingClock(settings[Keys.interval])
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.app_preferences)) },
                        onClick = {
                            expanded = false
                            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.app_info)) },
                        onClick = { expanded = false; dialog = Dialog.INFO },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.app_close)) },
                        onClick = {
                            expanded = false
                            if (settings[Keys.closeDialogShown]) closeApp() else {
                                settings[Keys.closeDialogShown] = true
                                dialog = Dialog.CLOSE
                            }
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun Dialogs() {
        when (dialog) {
            Dialog.CLOSE -> ConfirmDialog(
                title = stringResource(R.string.app_close),
                text = stringResource(R.string.app_request_close_question),
                confirm = stringResource(R.string.app_yes),
                dismiss = stringResource(R.string.app_no),
                onConfirm = { dialog = null; closeApp() },
                onDismiss = { dialog = null },
            )
            Dialog.INFO -> InfoDialog(onDismiss = { dialog = null })
            Dialog.INTERVAL -> IntervalDialog(
                withSeconds = settings.secondsAvailable && settings[Keys.seconds],
                onSelect = { dialog = null; startSpeakingClock(it) },
                onDismiss = { dialog = null },
            )
            Dialog.TTS_MISSING -> ConfirmDialog(
                title = stringResource(R.string.app_tts_install_title),
                text = stringResource(R.string.app_tts_install_message),
                confirm = stringResource(R.string.app_ok),
                dismiss = stringResource(R.string.app_cancel),
                onConfirm = { dialog = null; openTtsStore() },
                onDismiss = { dialog = null },
            )
            null -> {}
        }
    }

    private fun openTtsStore() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.tts"))
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
            } catch (e: ActivityNotFoundException) {
                android.widget.Toast.makeText(this, R.string.app_tts_install_activity_missing, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val EXTRA_NIGHT_CLOCK = "night_clock"
    }
}
