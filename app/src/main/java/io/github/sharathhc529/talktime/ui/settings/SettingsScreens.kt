package io.github.sharathhc529.talktime.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import io.github.sharathhc529.talktime.R
import io.github.sharathhc529.talktime.service.TalkTimeService
import io.github.sharathhc529.talktime.settings.AudioFocusMode
import io.github.sharathhc529.talktime.settings.ClickCount
import io.github.sharathhc529.talktime.settings.DisplayHourFormat
import io.github.sharathhc529.talktime.settings.HourFormat
import io.github.sharathhc529.talktime.settings.ClockStyle
import io.github.sharathhc529.talktime.settings.Keys
import io.github.sharathhc529.talktime.settings.NightClockBehaviour
import io.github.sharathhc529.talktime.settings.RingerMode
import io.github.sharathhc529.talktime.settings.ScreenOrientation
import io.github.sharathhc529.talktime.settings.Settings
import io.github.sharathhc529.talktime.settings.collect
import io.github.sharathhc529.talktime.settings.version
import io.github.sharathhc529.talktime.speech.SpeakingInterval
import io.github.sharathhc529.talktime.speech.SpeechLanguage
import io.github.sharathhc529.talktime.speech.TimeStyle

enum class Screen(@StringRes val title: Int) {
    MAIN(R.string.app_preferences),
    CONTROL(R.string.pref_screen_title_control),
    HEADSET(R.string.pref_screen_title_headset_trigger),
    PROXIMITY(R.string.pref_screen_title_proximity_trigger),
    NIGHT_CLOCK(R.string.pref_screen_title_night_clock),
    INTERVAL(R.string.pref_screen_title_interval_speaking_clock),
    POWER_BUTTON(R.string.pref_screen_title_power_button_trigger),
    SHAKE(R.string.pref_screen_title_shake_detector_trigger),
    OPEN_APP(R.string.pref_screen_title_open_app),
    DISPLAY(R.string.pref_screen_title_appearance),
    AUDIO(R.string.pref_screen_title_audio),
    SYSTEM(R.string.pref_screen_title_system),
}

@Composable
fun SettingsScreen(screen: Screen, settings: Settings, service: TalkTimeService?, open: (Screen) -> Unit) {
    when (screen) {
        Screen.MAIN -> MainScreen(open)
        Screen.CONTROL -> ControlScreen(settings, open)
        Screen.HEADSET -> HeadsetScreen(settings)
        Screen.PROXIMITY -> ProximityScreen(settings)
        Screen.NIGHT_CLOCK -> NightClockScreen(settings)
        Screen.INTERVAL -> IntervalScreen(settings)
        Screen.POWER_BUTTON -> PowerButtonScreen(settings)
        Screen.SHAKE -> ShakeScreen(settings)
        Screen.OPEN_APP -> OpenAppScreen(settings)
        Screen.DISPLAY -> DisplayScreen(settings)
        Screen.AUDIO -> AudioScreen(settings, service)
        Screen.SYSTEM -> SystemScreen(settings)
    }
}

@Composable
private fun MainScreen(open: (Screen) -> Unit) {
    ScreenLink(stringResource(R.string.pref_screen_title_control), stringResource(R.string.pref_screen_summary_control)) { open(Screen.CONTROL) }
    ScreenLink(stringResource(R.string.pref_screen_title_appearance), stringResource(R.string.pref_screen_summary_appearance)) { open(Screen.DISPLAY) }
    ScreenLink(stringResource(R.string.pref_screen_title_audio), stringResource(R.string.pref_screen_summary_audio)) { open(Screen.AUDIO) }
    ScreenLink(stringResource(R.string.pref_screen_title_system), stringResource(R.string.pref_screen_summary_system)) { open(Screen.SYSTEM) }
}

@Composable
private fun ControlScreen(settings: Settings, open: (Screen) -> Unit) {
    ScreenLink(stringResource(R.string.pref_screen_title_headset_trigger), stringResource(R.string.pref_screen_summary_headset_trigger)) { open(Screen.HEADSET) }
    ScreenLink(stringResource(R.string.pref_screen_title_proximity_trigger), stringResource(R.string.pref_screen_summary_proximity_trigger)) { open(Screen.PROXIMITY) }
    ScreenLink(stringResource(R.string.pref_screen_title_night_clock), stringResource(R.string.pref_screen_summary_night_clock)) { open(Screen.NIGHT_CLOCK) }
    ScreenLink(stringResource(R.string.pref_screen_title_interval_speaking_clock), stringResource(R.string.pref_screen_summary_interval_speaking_clock)) { open(Screen.INTERVAL) }
    ScreenLink(stringResource(R.string.pref_screen_title_power_button_trigger), stringResource(R.string.pref_screen_summary_power_button_trigger)) { open(Screen.POWER_BUTTON) }
    SwitchPreference(settings, Keys.touch, stringResource(R.string.pref_checkbox_title_touch), stringResource(R.string.pref_checkbox_summary_touch))
    ScreenLink(stringResource(R.string.pref_screen_title_shake_detector_trigger), stringResource(R.string.pref_screen_summary_shake_detector_trigger)) { open(Screen.SHAKE) }
    ScreenLink(stringResource(R.string.pref_screen_title_open_app), stringResource(R.string.pref_screen_summary_open_app)) { open(Screen.OPEN_APP) }
}

@Composable
private fun clickOptions(): List<Pair<ClickCount, String>> {
    val names = stringArrayResource(R.array.pref_list_headset_event_type_name)
    return ClickCount.entries.map { it to names[it.ordinal] }
}

@Composable
private fun clickSummary(): (ClickCount) -> String {
    val summaries = listOf(
        stringResource(R.string.pref_list_summary_headset_event_none),
        stringResource(R.string.pref_list_summary_headset_event_single_click),
        stringResource(R.string.pref_list_summary_headset_event_double_click),
        stringResource(R.string.pref_list_summary_headset_event_triple_click),
    )
    return { summaries[it.ordinal] }
}

@Composable
private fun HeadsetScreen(settings: Settings) {
    val options = clickOptions()
    val summary = clickSummary()
    val ms = stringResource(R.string.app_seekbar_milliseconds_unit)
    PreferenceCategory(stringResource(R.string.pref_category_trigger_title_wired_headset_button))
    ListPreference(settings, Keys.wiredHook, stringResource(R.string.pref_list_title_headset_hook_button), options, summary = summary)
    val delayDescription = stringResource(R.string.pref_preference_summary_wired_headset_multiple_click_delay)
    SliderPreference(
        settings, Keys.wiredClickDelay, stringResource(R.string.pref_preference_title_wired_headset_multiple_click_delay),
        100..1000, step = 10, format = { "$it$ms" }, summary = { "$it$ms $delayDescription" },
    )
    PreferenceCategory(stringResource(R.string.pref_category_trigger_title_headset_media_buttons))
    ListPreference(settings, Keys.mediaPlayPause, stringResource(R.string.pref_list_title_headset_media_play_pause_button), options, summary = summary)
    ListPreference(settings, Keys.mediaStop, stringResource(R.string.pref_list_title_headset_media_stop_button), options, summary = summary)
    ListPreference(settings, Keys.mediaNext, stringResource(R.string.pref_list_title_headset_media_next_button), options, summary = summary)
    ListPreference(settings, Keys.mediaPrevious, stringResource(R.string.pref_list_title_headset_media_previous_button), options, summary = summary)
    SliderPreference(
        settings, Keys.mediaClickDelay, stringResource(R.string.pref_preference_title_headset_media_button_multiple_click_delay),
        100..2000, step = 10, format = { "$it$ms" }, summary = { "$it$ms $delayDescription" },
    )
    Note(stringResource(R.string.headset_note))
}

@Composable
private fun ProximityScreen(settings: Settings) {
    val context = LocalContext.current
    val available = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_PROXIMITY) }
    val enabled by settings.collect(Keys.proximity)
    SwitchPreference(
        settings, Keys.proximity, stringResource(R.string.pref_checkbox_title_proximity),
        stringResource(if (available) R.string.pref_checkbox_summary_proximity else R.string.proximity_missing),
        enabled = available,
    )
    ListPreference(
        settings, Keys.proximitySwipes, stringResource(R.string.pref_list_title_proximity_gesture),
        listOf(
            1 to stringResource(R.string.pref_list_summary_proximity_gesture_1),
            2 to stringResource(R.string.pref_list_summary_proximity_gesture_2),
            3 to stringResource(R.string.pref_list_summary_proximity_gesture_3),
        ),
        enabled = enabled && available,
    )
    SwitchPreference(
        settings, Keys.proximityVibrate, stringResource(R.string.pref_checkbox_title_proximity_vibrate),
        stringResource(R.string.pref_checkbox_summary_proximity_vibrate), enabled = enabled && available,
    )
    SwitchPreference(
        settings, Keys.proximityScreenOffOnly, stringResource(R.string.pref_checkbox_title_proximity_disabled_while_screen_on),
        stringResource(R.string.pref_checkbox_summary_proximity_disabled_while_screen_on), enabled = enabled && available,
    )
    val ms = stringResource(R.string.app_seekbar_milliseconds_unit)
    val speedSummary = stringResource(R.string.pref_preference_summary_proximity_speed_of_recognition)
    SliderPreference(
        settings, Keys.proximitySpeed, stringResource(R.string.pref_preference_title_proximity_speed_of_recognition),
        10..1000, step = 10, enabled = enabled && available,
        format = { "$it$ms" }, summary = { speedSummary.format("$it$ms") },
    )
}

@Composable
private fun NightClockScreen(settings: Settings) {
    val context = LocalContext.current
    PreferenceCategory(stringResource(R.string.pref_category_title_night_clock_start_options))
    SwitchPreference(settings, Keys.nightClockOnCharger, stringResource(R.string.pref_checkbox_title_power), stringResource(R.string.pref_checkbox_summary_power))
    PreferenceCategory(stringResource(R.string.pref_category_title_night_clock_general))
    val behaviourNames = stringArrayResource(R.array.pref_list_night_clock_behaviour_name)
    val behaviourSummaries = listOf(
        stringResource(R.string.pref_list_summary_night_clock_behaviour_1),
        stringResource(R.string.pref_list_summary_night_clock_behaviour_2),
        stringResource(R.string.pref_list_summary_night_clock_behaviour_3),
    )
    ListPreference(
        settings, Keys.nightClockBehaviour, stringResource(R.string.pref_list_title_night_clock_behaviour),
        NightClockBehaviour.entries.map { it to behaviourNames[it.ordinal] },
        summary = { behaviourSummaries[it.ordinal] },
    )
    SwitchPreference(
        settings, Keys.nightClockNotification, stringResource(R.string.pref_checkbox_title_night_clock_notification),
        stringResource(R.string.pref_checkbox_summary_night_clock_notification_shown),
        stringResource(R.string.pref_checkbox_summary_night_clock_notification_hidden),
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val granted = rememberOnResume { context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent() }
        if (!granted) {
            PermissionRow(stringResource(R.string.perm_full_screen_title), stringResource(R.string.perm_full_screen_summary)) {
                context.openSettings(
                    Intent(AndroidSettings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}")),
                )
            }
        }
    }
}

@Composable
private fun intervalOptions(settings: Settings): List<Pair<SpeakingInterval, String>> {
    settings.version().value
    val withSeconds = settings.secondsAvailable && settings[Keys.seconds]
    return SpeakingInterval.entries.filter { withSeconds || !it.usesSeconds }
        .map { it to stringResource(TalkTimeService.intervalName(it)) }
}

@Composable
private fun IntervalScreen(settings: Settings) {
    val context = LocalContext.current
    PreferenceCategory(stringResource(R.string.pref_category_title_speaking_clock_general))
    SwitchPreference(settings, Keys.intervalActive, stringResource(R.string.app_start_speaking_clock), stringResource(R.string.pref_checkbox_summary_pulse))
    SwitchPreference(
        settings, Keys.intervalNotification, stringResource(R.string.pref_checkbox_title_pulse_generator_notification),
        stringResource(R.string.pref_checkbox_summary_pulse_generator_notification_shown),
        stringResource(R.string.pref_checkbox_summary_pulse_generator_notification_hidden),
    )
    SwitchPreference(
        settings, Keys.intervalSelectionDialog, stringResource(R.string.pref_checkbox_title_speaking_clock_interval_selection_dialog),
        stringResource(R.string.pref_checkbox_summary_speaking_clock_interval_selection_dialog),
    )
    ListPreference(settings, Keys.interval, stringResource(R.string.pref_list_title_pulse_interval), intervalOptions(settings))
    SwitchPreference(
        settings, Keys.intervalAlarmMode, stringResource(R.string.pref_checkbox_title_speaking_clock_interval_alarm_mode),
        stringResource(R.string.pref_checkbox_summary_speaking_clock_interval_alarm_mode),
    )
    SwitchPreference(
        settings, Keys.intervalAnnounceAtStart, stringResource(R.string.pref_checkbox_title_speaking_clock_interval_start_info),
        stringResource(R.string.pref_checkbox_summary_speaking_clock_interval_start_info),
    )
    PreferenceCategory(stringResource(R.string.pref_category_title_speaking_clock_start_options))
    SwitchPreference(settings, Keys.intervalOnHeadset, stringResource(R.string.pref_checkbox_title_speaking_clock_headset), stringResource(R.string.pref_checkbox_summary_speaking_clock_headset))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val granted = rememberOnResume { context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms() }
        if (!granted) {
            PermissionRow(stringResource(R.string.perm_exact_alarm_title), stringResource(R.string.perm_exact_alarm_summary)) {
                context.openSettings(
                    Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                )
            }
        }
    }
}

@Composable
private fun PowerButtonScreen(settings: Settings) {
    val enabled by settings.collect(Keys.powerButton)
    SwitchPreference(
        settings, Keys.powerButton, stringResource(R.string.pref_checkbox_title_power_button),
        stringResource(R.string.pref_checkbox_summary_power_button_1), stringResource(R.string.pref_checkbox_summary_power_button_2),
    )
    val s = stringResource(R.string.app_seekbar_seconds_unit)
    val description = stringResource(R.string.pref_preference_summary_power_button_click_delay)
    SliderPreference(
        settings, Keys.powerButtonDelay, stringResource(R.string.pref_preference_title_power_button_click_delay),
        1..5, enabled = enabled, format = { "$it$s" }, summary = { "$it$s $description" },
    )
}

@Composable
private fun ShakeScreen(settings: Settings) {
    val enabled by settings.collect(Keys.shake)
    SwitchPreference(
        settings, Keys.shake, stringResource(R.string.pref_checkbox_title_shake_detector),
        stringResource(R.string.pref_checkbox_summary_shake_detector_1), stringResource(R.string.pref_checkbox_summary_shake_detector_2),
    )
    val g = stringResource(R.string.app_seekbar_gravity_unit)
    val sensitivity = stringResource(R.string.pref_preference_summary_shake_detector_sensitivity)
    SliderPreference(
        settings, Keys.shakeSensitivity, stringResource(R.string.pref_preference_title_shake_detector_sensitivity),
        11..40, enabled = enabled, format = { "${it / 10f}$g" }, summary = { "${it / 10f}$g - $sensitivity" },
    )
    val x = stringResource(R.string.app_seekbar_x)
    val count = stringResource(R.string.pref_preference_summary_shake_counter)
    SliderPreference(
        settings, Keys.shakeCount, stringResource(R.string.pref_preference_title_shake_counter),
        1..10, enabled = enabled, format = { "$it$x" }, summary = { "$it$x - $count" },
    )
    SwitchPreference(
        settings, Keys.shakeVibrate, stringResource(R.string.pref_checkbox_title_shake_vibrate),
        stringResource(R.string.pref_checkbox_summary_shake_vibrate), enabled = enabled,
    )
    Note(stringResource(R.string.shake_note))
}

@Composable
private fun OpenAppScreen(settings: Settings) {
    val announce by settings.collect(Keys.announceOnOpen)
    val autoClose by settings.collect(Keys.autoClose)
    val delay by settings.collect(Keys.autoCloseDelay)
    SwitchPreference(settings, Keys.announceOnOpen, stringResource(R.string.pref_checkbox_title_open_app), stringResource(R.string.pref_checkbox_summary_open_app))
    SwitchPreference(
        settings, Keys.autoClose, stringResource(R.string.pref_checkbox_open_app_title_auto_close),
        stringResource(R.string.pref_checkbox_open_app_summary_auto_close, delay), enabled = announce,
    )
    val s = stringResource(R.string.app_seekbar_seconds_unit)
    SliderPreference(
        settings, Keys.autoCloseDelay, stringResource(R.string.pref_preference_open_app_title_auto_close_delay),
        3..120, enabled = announce && autoClose, format = { "$it$s" },
    )
}

@Composable
private fun DisplayScreen(settings: Settings) {
    val styleNames = stringArrayResource(R.array.pref_list_text_style_name)
    val styleOptions = listOf(false to styleNames[0], true to styleNames[1])
    val clockStyleNames = listOf(
        ClockStyle.FLIP to stringResource(R.string.clock_style_flip),
        ClockStyle.STACKED to stringResource(R.string.clock_style_stacked),
        ClockStyle.GLASS to stringResource(R.string.clock_style_glass),
        ClockStyle.WORDS to stringResource(R.string.clock_style_words),
        ClockStyle.RING to stringResource(R.string.clock_style_ring),
        ClockStyle.ANALOG to stringResource(R.string.clock_style_analog),
    )
    ListPreference(settings, Keys.clockStyle, stringResource(R.string.pref_clock_style_title), clockStyleNames)
    // The other styles come with their own colours and type; these settings shape the flip clock.
    val clockStyle by settings.collect(Keys.clockStyle)
    if (clockStyle == ClockStyle.FLIP) {
        PreferenceCategory(stringResource(R.string.pref_category_title_clock_text))
        ColorPreference(settings, Keys.textColor, stringResource(R.string.pref_preference_title_night_clock_text_color), stringResource(R.string.pref_preference_summary_night_clock_text_color))
        val timeSummaries = stringResource(R.string.pref_list_summary_time_text_style_normal) to stringResource(R.string.pref_list_summary_time_text_style_bold)
        ListPreference(settings, Keys.timeBold, stringResource(R.string.pref_list_title_time_text_style), styleOptions) { if (it) timeSummaries.second else timeSummaries.first }
        val amPmSummaries = stringResource(R.string.pref_list_summary_time_of_day_text_style_normal) to stringResource(R.string.pref_list_summary_time_of_day_text_style_bold)
        ListPreference(settings, Keys.amPmBold, stringResource(R.string.pref_list_title_time_of_day_text_style), styleOptions) { if (it) amPmSummaries.second else amPmSummaries.first }
        val dateSummaries = stringResource(R.string.pref_list_summary_date_text_style_normal) to stringResource(R.string.pref_list_summary_date_text_style_bold)
        ListPreference(settings, Keys.dateBold, stringResource(R.string.pref_list_title_date_text_style), styleOptions) { if (it) dateSummaries.second else dateSummaries.first }

        PreferenceCategory(stringResource(R.string.pref_category_title_clock_background))
        ColorPreference(settings, Keys.cardColor, stringResource(R.string.pref_card_color_title), stringResource(R.string.pref_card_color_summary))
        ColorPreference(settings, Keys.flipBackgroundColor, stringResource(R.string.pref_preference_title_night_clock_background_color), stringResource(R.string.pref_preference_summary_night_clock_background_color))
    }

    PreferenceCategory(stringResource(R.string.pref_category_title_time_format))
    val formatNames = stringArrayResource(R.array.pref_list_time_hour_format_appearance_name)
    ListPreference(
        settings, Keys.displayHourFormat, stringResource(R.string.pref_list_title_time_hour_format),
        DisplayHourFormat.entries.map { it to formatNames[it.ordinal] },
    )

    PreferenceCategory(stringResource(R.string.pref_category_title_appearance_general))
    val orientationNames = stringArrayResource(R.array.pref_list_screen_orientation_name)
    val orientationSummaries = listOf(
        stringResource(R.string.pref_list_summary_screen_orientation_system),
        stringResource(R.string.pref_list_summary_screen_orientation_automatic),
        stringResource(R.string.pref_list_summary_screen_orientation_vertical),
        stringResource(R.string.pref_list_summary_screen_orientation_horizontal),
    )
    ListPreference(
        settings, Keys.orientation, stringResource(R.string.pref_list_title_screen_orientation),
        ScreenOrientation.entries.map { it to orientationNames[it.ordinal] }, summary = { orientationSummaries[it.ordinal] },
    )
}

@Composable
private fun AudioScreen(settings: Settings, service: TalkTimeService?) {
    val context = LocalContext.current
    settings.version().value // Several summaries depend on more than one setting.

    PreferenceCategory(stringResource(R.string.pref_category_title_speech))
    val deviceLanguage = stringResource(R.string.lang_device)
    ListPreference(
        settings, Keys.language, stringResource(R.string.pref_list_title_language),
        listOf<Pair<SpeechLanguage?, String>>(null to deviceLanguage) +
            SpeechLanguage.entries.sortedBy { it.displayName }.map { it to it.displayName },
        summary = { it?.displayName ?: "$deviceLanguage (${settings.language.displayName})" },
    )
    PreferenceRow(stringResource(R.string.pref_preference_title_speech_tts_settings), onClick = {
        try {
            context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.app_open_tts_setting_failure, Toast.LENGTH_SHORT).show()
        }
    })
    PreferenceRow(
        stringResource(R.string.pref_test_title), settings.phrase(),
        onClick = { service?.announceTime() },
    )

    PreferenceCategory(stringResource(R.string.pref_category_title_time_format))
    val styleNames = stringArrayResource(R.array.pref_list_speaking_clock_style_name)
    ListPreference(
        settings, Keys.style, stringResource(R.string.pref_list_title_speaking_clock_style),
        listOf(TimeStyle.FORMAL to styleNames[0], TimeStyle.COMMON to styleNames[1]),
    )
    val hourNames = stringArrayResource(R.array.pref_list_time_hour_format_name)
    ListPreference(
        settings, Keys.hourFormat, stringResource(R.string.pref_list_title_time_hour_format),
        HourFormat.entries.map { it to hourNames[it.ordinal] },
    )
    SwitchPreference(settings, Keys.timeOfDay, stringResource(R.string.pref_checkbox_title_time_of_day))
    val secondsAvailable = settings.secondsAvailable
    SwitchPreference(
        settings, Keys.seconds, stringResource(R.string.pref_checkbox_title_seconds),
        if (secondsAvailable) stringResource(R.string.pref_checkbox_summary_seconds)
        else stringResource(R.string.pref_checkbox_summary_seconds_not_supported, settings.language.displayName),
        enabled = secondsAvailable,
    )
    SwitchPreference(settings, Keys.introText, stringResource(R.string.pref_checkbox_title_intro_text), stringResource(R.string.pref_checkbox_summary_intro_text))

    PreferenceCategory(stringResource(R.string.pref_list_title_audio_general))
    IntroSoundPreference(settings)
    val independent by settings.collect(Keys.independentVolume)
    SwitchPreference(
        settings, Keys.independentVolume, stringResource(R.string.pref_checkbox_title_system_independent_volume),
        stringResource(R.string.pref_checkbox_summary_system_independent_volume),
    )
    val percent = stringResource(R.string.app_seekbar_percent_unit)
    SliderPreference(
        settings, Keys.volume, stringResource(R.string.pref_preference_title_tts_volume), 1..100,
        enabled = independent, format = { "$it$percent" },
    )
    SwitchPreference(
        settings, Keys.volumeKeys, stringResource(R.string.pref_checkbox_title_volume_control_is_allowed),
        stringResource(R.string.pref_checkbox_summary_volume_control_is_allowed), enabled = independent,
    )
    val focusNames = stringArrayResource(R.array.pref_list_audio_focus_name)
    val focusSummaries = listOf(
        stringResource(R.string.pref_list_summary_audio_focus_0),
        stringResource(R.string.pref_list_summary_audio_focus_1),
        stringResource(R.string.pref_list_summary_audio_focus_2),
    )
    ListPreference(
        settings, Keys.audioFocus, stringResource(R.string.pref_list_title_audio_focus_selection),
        AudioFocusMode.entries.map { it to focusNames[it.ordinal] }, summary = { focusSummaries[it.ordinal] },
    )
    val ringerNames = stringArrayResource(R.array.pref_checkbox_list_ringer_mode_name)
    val ringerOptions = listOf(RingerMode.SILENT, RingerMode.VIBRATE, RingerMode.NORMAL).map { it to ringerNames[it] }
    val ringerSummary = stringResource(R.string.pref_list_summary_ringer_mode)
    val noRinger = stringResource(R.string.pref_list_summary_no_ringer_mode_selected)
    MultiSelectPreference(
        settings, Keys.ringerModes, stringResource(R.string.pref_list_title_ringer_mode), ringerOptions,
        summary = { modes ->
            if (modes.isEmpty()) noRinger
            else ringerSummary + " " + ringerOptions.filter { it.first in modes }.joinToString(", ") { it.second }
        },
    )
}

@Composable
private fun SystemScreen(settings: Settings) {
    val context = LocalContext.current
    SwitchPreference(settings, Keys.autostart, stringResource(R.string.pref_checkbox_title_autostart), stringResource(R.string.pref_checkbox_summary_autostart))
    SwitchPreference(settings, Keys.ttsCheck, stringResource(R.string.pref_checkbox_title_tts_check), stringResource(R.string.pref_checkbox_summary_tts_check))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = rememberOnResume {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        if (!granted) {
            PermissionRow(stringResource(R.string.perm_notifications_title), stringResource(R.string.perm_notifications_summary)) {
                context.openSettings(
                    Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName),
                )
            }
        }
    }
    val unrestricted = rememberOnResume {
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    }
    PreferenceRow(
        stringResource(R.string.perm_battery_title),
        stringResource(if (unrestricted) R.string.perm_battery_summary_granted else R.string.perm_battery_summary),
        onClick = { context.openSettings(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
    )
}

/** Picks the sound played before each announcement with the system ringtone picker. */
@Composable
private fun IntroSoundPreference(settings: Settings) {
    val context = LocalContext.current
    val uri = settings.collect(Keys.introSound).value
    val launcher = rememberLauncherForActivityResult(PickRingtone) { result ->
        if (result != null) settings[Keys.introSound] = result.toString().takeIf { result != Uri.EMPTY }.orEmpty()
    }
    val name = remember(uri) {
        if (uri.isEmpty()) null else runCatching { RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context) }.getOrNull()
    }
    PreferenceRow(
        stringResource(R.string.pref_preference_title_intro_sound),
        if (name == null) stringResource(R.string.pref_preference_summary_no_intro_sound)
        else stringResource(R.string.pref_preference_summary_intro_sound, name),
        onClick = { launcher.launch(uri) },
    )
}

/** Ringtone picker; returns [Uri.EMPTY] for "silent" and null when cancelled. */
private object PickRingtone : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String) = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.pref_list_title_intro_sound))
        if (input.isNotEmpty()) putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(input))
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode != android.app.Activity.RESULT_OK || intent == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        } ?: Uri.EMPTY
    }
}

@Composable
private fun PermissionRow(title: String, summary: String, onClick: () -> Unit) {
    PreferenceRow(title, summary, onClick = onClick)
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp),
    )
}

/** Re-evaluates [check] every time the screen is resumed, e.g. after returning from system settings. */
@Composable
private fun rememberOnResume(check: () -> Boolean): Boolean {
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }
    return remember(tick) { check() }
}

private fun Context.openSettings(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, R.string.settings_not_found, Toast.LENGTH_SHORT).show()
    }
}
