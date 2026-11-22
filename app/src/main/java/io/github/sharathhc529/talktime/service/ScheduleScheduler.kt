package io.github.sharathhc529.talktime.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.sharathhc529.talktime.settings.Settings
import io.github.sharathhc529.talktime.ui.MainActivity
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Keeps one alarm for the next daily start or stop of the speaking clock. It is independent of
 * the service, so the speaking clock also starts when the app was closed.
 */
class ScheduleScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** Sets the alarm for the next change of the schedule after [now], or cancels it when there is none. */
    fun update(settings: Settings, now: LocalDateTime = LocalDateTime.now()) {
        val schedule = settings.intervalSchedule
        if (schedule == null) {
            alarmManager.cancel(alarmIntent(starts = false))
            return
        }
        val change = schedule.nextChangeAfter(now)
        val triggerAt = change.at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val operation = alarmIntent(change.starts)
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (exactAllowed) {
            // Like an alarm clock: delivered on time in deep sleep, and allowed to start the service.
            val show = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, show), operation)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
    }

    private fun alarmIntent(starts: Boolean): PendingIntent {
        val intent = Intent(context, ScheduleAlarmReceiver::class.java)
            .setAction(ScheduleAlarmReceiver.ACTION)
            .putExtra(ScheduleAlarmReceiver.EXTRA_STARTS, starts)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val REQUEST_CODE = 2
    }
}
