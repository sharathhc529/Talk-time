package io.github.sharathhc529.talktime.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.sharathhc529.talktime.settings.Keys
import io.github.sharathhc529.talktime.settings.Settings
import java.time.LocalDateTime

/** Delivers the interval speaking clock alarm to the service. */
class IntervalAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val scheduledAt = intent.getLongExtra(EXTRA_SCHEDULED_AT, 0L)
        TalkTimeService.deliver(
            context,
            Intent(context, TalkTimeService::class.java)
                .setAction(TalkTimeService.ACTION_INTERVAL_ALARM)
                .putExtra(EXTRA_SCHEDULED_AT, scheduledAt),
        )
    }

    companion object {
        const val ACTION = "io.github.sharathhc529.talktime.INTERVAL_ALARM"
        const val EXTRA_SCHEDULED_AT = "scheduled_at"
    }
}

/** Starts or stops the speaking clock at the times of its daily schedule. */
class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val settings = Settings(context)
        val schedule = settings.intervalSchedule ?: return
        val now = LocalDateTime.now()
        val starts = intent.getBooleanExtra(EXTRA_STARTS, false)
        Log.i(TAG, "Schedule alarm: ${if (starts) "start" else "stop"} at $now")
        if (starts) {
            // A start delivered after the stop time (e.g. the phone was off) is skipped.
            if (now in schedule) deliver(context, TalkTimeService.ACTION_SCHEDULED_START)
        } else if (settings[Keys.intervalActive]) {
            if (TalkTimeService.isRunning) {
                deliver(context, TalkTimeService.ACTION_STOP_INTERVAL)
            } else {
                settings[Keys.intervalActive] = false
            }
        }
        ScheduleScheduler(context).update(settings, now)
    }

    private fun deliver(context: Context, action: String) {
        TalkTimeService.deliver(context, Intent(context, TalkTimeService::class.java).setAction(action))
    }

    companion object {
        const val ACTION = "io.github.sharathhc529.talktime.SCHEDULE_ALARM"
        const val EXTRA_STARTS = "starts"
        private const val TAG = "ScheduleAlarm"
    }
}

/** Starts the background service after a reboot or an app update, if it is needed. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val settings = Settings(context)
                // Alarms are lost on reboot; the daily schedule is set up again.
                ScheduleScheduler(context).update(settings)
                if (settings[Keys.autostart] && settings.needsBackgroundService) {
                    TalkTimeService.deliver(context, Intent(context, TalkTimeService::class.java))
                }
            }
        }
    }
}
