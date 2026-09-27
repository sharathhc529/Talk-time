package io.github.sharathhc529.talktime.ui.settings

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.sharathhc529.talktime.R
import io.github.sharathhc529.talktime.service.TalkTimeService
import io.github.sharathhc529.talktime.settings.Settings
import io.github.sharathhc529.talktime.ui.theme.TalkTimeTheme

/** The settings, organised in the same screens as the original app. */
class SettingsActivity : ComponentActivity() {

    private lateinit var settings: Settings
    private val stack = mutableStateListOf(Screen.MAIN)
    private var service by mutableStateOf<TalkTimeService?>(null)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as TalkTimeService.LocalBinder).service
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    /** Starts the background service as soon as a setting needs it. */
    private val refresh = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> TalkTimeService.refresh(this) }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        settings = Settings(this)
        savedInstanceState?.getStringArrayList(STATE_STACK)?.let { saved ->
            stack.clear()
            stack.addAll(saved.map(Screen::valueOf))
        }
        setContent {
            TalkTimeTheme {
                val screen = stack.last()
                BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(screen.title)) },
                            navigationIcon = {
                                IconButton(onClick = { if (stack.size > 1) stack.removeAt(stack.lastIndex) else finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                                }
                            },
                        )
                    },
                ) { padding ->
                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "settings",
                        modifier = Modifier.padding(padding),
                    ) { target ->
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            SettingsScreen(
                                screen = target,
                                settings = settings,
                                service = service,
                                open = { stack.add(it) },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        settings.prefs.registerOnSharedPreferenceChangeListener(refresh)
        bindService(Intent(this, TalkTimeService::class.java), connection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        settings.prefs.unregisterOnSharedPreferenceChangeListener(refresh)
        unbindService(connection)
        service = null
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(STATE_STACK, ArrayList(stack.map { it.name }))
    }

    private companion object {
        const val STATE_STACK = "stack"
    }
}
