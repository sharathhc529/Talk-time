package io.github.sharathhc529.talktime.trigger

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.os.SystemClock
import kotlin.math.sqrt

/**
 * Detects shaking: [count] accelerations of at least [thresholdG] g, at least 500 ms apart and
 * with no pause longer than five seconds between them.
 */
class ShakeTrigger(context: Context, private val onShake: () -> Unit) : SensorEventListener {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val haptics = Haptics(context)

    /** Keeps the accelerometer running while the screen is off. */
    private val wakeLock = context.getSystemService(PowerManager::class.java)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TalkTime:Shake")
        .apply { setReferenceCounted(false) }

    var thresholdG: Float = 2.0f
    var count: Int = 3
    var vibrate: Boolean = false

    private var listening = false
    private var lastShake = 0L
    private var shakes = 0

    /** The wake lock is held for as long as the user keeps shake detection enabled. */
    @Suppress("WakelockTimeout")
    fun start(): Boolean {
        if (listening || sensor == null) return listening
        listening = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        if (listening) wakeLock.acquire()
        return listening
    }

    fun stop() {
        if (!listening) return
        sensorManager.unregisterListener(this)
        if (wakeLock.isHeld) wakeLock.release()
        listening = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0] / SensorManager.GRAVITY_EARTH
        val y = event.values[1] / SensorManager.GRAVITY_EARTH
        val z = event.values[2] / SensorManager.GRAVITY_EARTH
        if (sqrt(x * x + y * y + z * z) < thresholdG) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastShake < MIN_GAP_MS) return
        if (now - lastShake > RESET_MS) shakes = 0
        lastShake = now
        shakes++
        if (vibrate) haptics.tick()
        if (shakes >= count) {
            shakes = 0
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private companion object {
        const val MIN_GAP_MS = 500L
        const val RESET_MS = 5_000L
    }
}
