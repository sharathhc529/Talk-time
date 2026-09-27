package io.github.sharathhc529.talktime.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sharathhc529.talktime.settings.Keys
import io.github.sharathhc529.talktime.settings.Settings

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

/** Starts the background service after a reboot or an app update, if it is needed. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val settings = Settings(context)
                if (settings[Keys.autostart] && settings.needsBackgroundService) {
                    TalkTimeService.deliver(context, Intent(context, TalkTimeService::class.java))
                }
            }
        }
    }
}
