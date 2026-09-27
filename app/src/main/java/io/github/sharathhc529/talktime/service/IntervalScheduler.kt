package io.github.sharathhc529.talktime.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.sharathhc529.talktime.speech.SpeakingInterval
import io.github.sharathhc529.talktime.ui.MainActivity
import java.time.LocalDateTime
import java.time.ZoneId

/** Schedules the next announcement of the interval speaking clock with [AlarmManager]. */
class IntervalScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * Schedules the next announcement after [now].
     *
     * In alarm mode the alarm is registered as an alarm clock, which Android delivers on time
     * even in deep sleep; otherwise an exact alarm that is allowed while idle is used.
     */
    fun scheduleNext(interval: SpeakingInterval, alarmMode: Boolean, now: LocalDateTime = LocalDateTime.now()): Long {
        val next = interval.nextAfter(now)
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val operation = alarmIntent(triggerAt)
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        when {
            exactAllowed && alarmMode -> {
                val show = PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
                )
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, show), operation)
            }
            exactAllowed -> alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            else -> alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
        return triggerAt
    }

    fun cancel() {
        alarmManager.cancel(alarmIntent(0))
    }

    private fun alarmIntent(triggerAt: Long): PendingIntent {
        val intent = Intent(context, IntervalAlarmReceiver::class.java)
            .setAction(IntervalAlarmReceiver.ACTION)
            .putExtra(IntervalAlarmReceiver.EXTRA_SCHEDULED_AT, triggerAt)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val REQUEST_CODE = 1
    }
}
