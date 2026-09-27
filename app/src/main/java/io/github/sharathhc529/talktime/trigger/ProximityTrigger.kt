package io.github.sharathhc529.talktime.trigger

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Detects hand swipes over the proximity sensor.
 *
 * A swipe is a "near" reading. With more than one swipe, each further swipe must follow the
 * previous one within [speedMs] milliseconds. After a detected gesture, further gestures are
 * ignored for two seconds.
 */
class ProximityTrigger(context: Context, private val onGesture: () -> Unit) : SensorEventListener {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val haptics = Haptics(context)

    var swipes: Int = 3
    var speedMs: Int = 750
    var vibrate: Boolean = false

    var isListening = false
        private set

    private var lastGestureUs = Long.MIN_VALUE / 2
    private var lastSwipeUs = 0L
    private var swipeCount = 0

    val isAvailable: Boolean get() = sensor != null

    fun start(): Boolean {
        if (isListening || sensor == null) return isListening
        swipeCount = 0
        isListening = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)
        return isListening
    }

    fun stop() {
        if (!isListening) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val nowUs = event.timestamp / 1000
        val near = event.values[0] < event.sensor.maximumRange
        if (!near) return
        if (vibrate) haptics.tick()
        if (nowUs - lastSwipeUs >= speedMs * 1000L) swipeCount = 0
        lastSwipeUs = nowUs
        swipeCount++
        if (swipeCount >= swipes && nowUs - lastGestureUs >= COOLDOWN_US) {
            lastGestureUs = nowUs
            swipeCount = 0
            onGesture()
        } else if (swipeCount >= swipes) {
            swipeCount = 0
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private companion object {
        const val COOLDOWN_US = 2_000_000L
    }
}
