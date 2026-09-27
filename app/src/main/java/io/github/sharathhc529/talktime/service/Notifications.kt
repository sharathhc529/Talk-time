package io.github.sharathhc529.talktime.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.sharathhc529.talktime.R
import io.github.sharathhc529.talktime.speech.SpeakingInterval
import io.github.sharathhc529.talktime.speech.SpeechLanguage
import io.github.sharathhc529.talktime.ui.MainActivity

class Notifications(private val context: Context) {

    /** What the status notification shows. */
    data class StatusState(val interval: SpeakingInterval?, val nightClock: Boolean)

    private val manager = NotificationManagerCompat.from(context)

    init {
        val system = context.getSystemService(NotificationManager::class.java)
        system.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_STATUS, context.getString(R.string.channel_status), NotificationManager.IMPORTANCE_LOW,
                ).apply { setShowBadge(false) },
                NotificationChannel(
                    CHANNEL_NIGHT_CLOCK, context.getString(R.string.app_notification_title_night_clock),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                },
                NotificationChannel(
                    CHANNEL_ALERTS, context.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_DEFAULT,
                ),
            ),
        )
    }

    fun status(state: StatusState): Notification {
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(if (state.nightClock) R.drawable.ic_state_night_clock else R.drawable.ic_state_speaking_clock)
            .setContentIntent(open)
            .setOngoing(true)
            .setShowWhen(false)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, context.getString(R.string.action_announce), serviceIntent(TalkTimeService.ACTION_ANNOUNCE, 1))

        val lines = mutableListOf<String>()
        state.interval?.let {
            lines += context.getString(R.string.app_notification_title_pulse_generator) + ": " +
                context.getString(TalkTimeService.intervalName(it))
            builder.addAction(
                0, context.getString(R.string.app_stop_speaking_clock), serviceIntent(TalkTimeService.ACTION_STOP_INTERVAL, 2),
            )
        }
        if (state.nightClock) {
            lines += context.getString(R.string.app_notification_title_night_clock)
            builder.addAction(
                0, context.getString(R.string.app_stop_night_clock), serviceIntent(TalkTimeService.ACTION_STOP_NIGHT_CLOCK, 3),
            )
        }
        builder.setContentTitle(context.getString(R.string.app_name))
        builder.setContentText(if (lines.isEmpty()) context.getString(R.string.status_listening) else lines.joinToString(" · "))
        return builder.build()
    }

    fun updateStatus(state: StatusState) = notify(STATUS_ID, status(state))

    /** Brings the night clock to the front, over the lock screen. */
    fun showNightClockWake() {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_NIGHT_CLOCK, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val fullScreen = PendingIntent.getActivity(
            context, 4, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_NIGHT_CLOCK)
            .setSmallIcon(R.drawable.ic_state_night_clock)
            .setContentTitle(context.getString(R.string.app_notification_title_night_clock))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .setAutoCancel(true)
            .setTimeoutAfter(NIGHT_CLOCK_TIMEOUT_MS)
            .build()
        notify(NIGHT_CLOCK_ID, notification)
    }

    fun cancelNightClockWake() = manager.cancel(NIGHT_CLOCK_ID)

    fun showVoiceMissing(language: SpeechLanguage) {
        val install = PendingIntent.getActivity(
            context, 5,
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_state_speaking_clock)
            .setContentTitle(context.getString(R.string.app_install_tts_data_title))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.app_install_tts_voice_data_message, language.displayName)),
            )
            .setContentText(context.getString(R.string.app_install_tts_voice_data_message, language.displayName))
            .setContentIntent(install)
            .setAutoCancel(true)
            .build()
        notify(VOICE_MISSING_ID, notification)
    }

    private fun notify(id: Int, notification: Notification) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU
        ) {
            manager.notify(id, notification)
        }
    }

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        context, requestCode, Intent(context, TalkTimeService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val STATUS_ID = 1
        const val NIGHT_CLOCK_ID = 2
        const val VOICE_MISSING_ID = 3
        private const val CHANNEL_STATUS = "status"
        private const val CHANNEL_NIGHT_CLOCK = "night_clock"
        private const val CHANNEL_ALERTS = "alerts"
        private const val NIGHT_CLOCK_TIMEOUT_MS = 30_000L
    }
}
