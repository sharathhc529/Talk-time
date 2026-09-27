package io.github.sharathhc529.talktime.trigger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.content.ContextCompat

/** Reports screen on/off changes. */
class ScreenMonitor(private val context: Context, private val onChange: (screenOn: Boolean) -> Unit) {
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> onChange(true)
                Intent.ACTION_SCREEN_OFF -> onChange(false)
            }
        }
    }

    val isScreenOn: Boolean get() = context.getSystemService(PowerManager::class.java).isInteractive

    fun start() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        registered = true
    }

    fun stop() {
        if (!registered) return
        context.unregisterReceiver(receiver)
        registered = false
    }
}

/**
 * Detects a double press of the power button: switching the screen on and off again within
 * [delaySeconds] seconds.
 */
class PowerButtonDetector(private val onDoublePress: () -> Unit) {
    var delaySeconds: Int = 2
    private var screenOnAt = 0L

    fun onScreenChanged(screenOn: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (screenOn) {
            screenOnAt = now
        } else if (screenOnAt != 0L && now - screenOnAt <= delaySeconds * 1000L) {
            screenOnAt = 0L
            onDoublePress()
        }
    }
}

/** Reports when the device is connected to or disconnected from a charger. */
class ChargerMonitor(private val context: Context, private val onChange: (connected: Boolean) -> Unit) {
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> onChange(true)
                Intent.ACTION_POWER_DISCONNECTED -> onChange(false)
            }
        }
    }

    val isCharging: Boolean
        get() {
            val status = ContextCompat.registerReceiver(
                context, null, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            return (status?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
        }

    fun start() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        registered = true
    }

    fun stop() {
        if (!registered) return
        context.unregisterReceiver(receiver)
        registered = false
    }
}

/** Reports when a wired or Bluetooth headset is connected or disconnected. */
class HeadsetMonitor(context: Context, private val onChange: (connected: Boolean) -> Unit) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var registered = false
    private var connected = false

    private val headsetTypes = buildSet {
        add(AudioDeviceInfo.TYPE_WIRED_HEADSET)
        add(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
        add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
        add(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        add(AudioDeviceInfo.TYPE_USB_HEADSET)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            add(AudioDeviceInfo.TYPE_BLE_HEADSET)
        }
    }

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = update()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = update()
    }

    val isConnected: Boolean
        get() = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in headsetTypes }

    private fun update() {
        val now = isConnected
        if (now != connected) {
            connected = now
            onChange(now)
        }
    }

    fun start() {
        if (registered) return
        connected = false
        // The callback immediately reports the devices that are already connected.
        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        registered = true
    }

    fun stop() {
        if (!registered) return
        audioManager.unregisterAudioDeviceCallback(callback)
        registered = false
    }
}
