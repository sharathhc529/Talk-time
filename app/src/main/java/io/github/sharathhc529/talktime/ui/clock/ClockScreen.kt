package io.github.sharathhc529.talktime.ui.clock

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sharathhc529.talktime.R
import io.github.sharathhc529.talktime.settings.ClockStyle
import io.github.sharathhc529.talktime.settings.Keys
import io.github.sharathhc529.talktime.settings.Settings
import io.github.sharathhc529.talktime.settings.collect
import io.github.sharathhc529.talktime.settings.version
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The full-screen clock in the style chosen in the display settings. [overlay] is drawn on top
 * and gets a colour that reads well on the chosen style.
 */
@Composable
fun ClockScreen(
    settings: Settings,
    onTap: () -> Unit,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier,
    overlay: @Composable (tint: Color) -> Unit = {},
) {
    val style by settings.collect(Keys.clockStyle)
    val textColor = Color(settings.collect(Keys.textColor).value)
    settings.version().value // Recompose when the hour format settings change.

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000 - now % 1000)
        }
    }
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val info = ClockInfo(now, locale, settings.displayIs24Hour)
    val currentOnTap by rememberUpdatedState(onTap)

    Box(modifier.fillMaxSize()) {
        // Taps that no control of the clock handles fall through to this layer.
        Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures(onPress = { currentOnTap() }) })
        when (style) {
            ClockStyle.FLIP -> FlipClock(settings, info, onSpeak)
            ClockStyle.STACKED -> StackedClock(settings, info, onSpeak)
            ClockStyle.GLASS -> GlassClock(settings, info, onSpeak)
            ClockStyle.WORDS -> WordsClock(settings, info, onSpeak)
            ClockStyle.RING -> RingClock(settings, info, onSpeak)
            ClockStyle.ANALOG -> AnalogClock(info, onSpeak)
        }
        Box(Modifier.fillMaxSize()) {
            overlay(if (style == ClockStyle.FLIP) textColor.copy(alpha = 0.6f) else style.contentColor.copy(alpha = 0.7f))
        }
    }
}

/** Colour of text and icons on each style other than the flip clock, whose colours are settings. */
private val ClockStyle.contentColor: Color
    get() = when (this) {
        ClockStyle.FLIP -> Color.White
        ClockStyle.STACKED -> StackedPalette.paper
        ClockStyle.GLASS -> Color.White
        ClockStyle.WORDS -> WordsPalette.ink
        ClockStyle.RING -> RingPalette.text
        ClockStyle.ANALOG -> AnalogPalette.ink
    }

/** The current time, formatted the ways the clock styles need it. */
@Immutable
internal class ClockInfo(val now: Long, val locale: Locale, val is24Hour: Boolean) {
    private val date = Date(now)
    private val calendar = Calendar.getInstance().apply { timeInMillis = now }

    val hour: Int = calendar.get(Calendar.HOUR_OF_DAY)
    val minute: Int = calendar.get(Calendar.MINUTE)
    val second: Int = calendar.get(Calendar.SECOND)

    /** Two-digit hours ("07", or "19" on a 24-hour clock). */
    val hours: String = format(if (is24Hour) "HH" else "hh")

    /** Hours without a leading zero on a 12-hour clock ("7"). */
    val shortHours: String = format(if (is24Hour) "HH" else "h")
    val minutes: String = format("mm")
    val amPm: String? = if (is24Hour) null else format("a").uppercase(locale)

    fun format(pattern: String, at: Date = date): String = SimpleDateFormat(pattern, locale).format(at)

    /** The date in the locale's order for the fields in [skeleton], such as "EEEEdMMMM". */
    fun date(skeleton: String): String = format(DateFormat.getBestDateTimePattern(locale, skeleton))

    /** [millis] as a time of day in the clock's hour format. */
    fun time(millis: Long): String = format(if (is24Hour) "HH:mm" else "h:mm a", Date(millis))
}

/** When the interval speaking clock announces the time next, or null when it is off. */
@Composable
internal fun nextAnnouncement(settings: Settings): Long? {
    val active by settings.collect(Keys.intervalActive)
    val interval by settings.collect(Keys.interval)
    if (!active) return null
    return interval.nextAfter(LocalDateTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

/** The button that announces the time, styled by each clock. */
@Composable
internal fun SpeakButton(
    onClick: () -> Unit,
    contentColor: Color,
    containerColor: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    fontSize: TextUnit = 18.sp,
    iconOnly: Boolean = false,
) {
    val label = stringResource(R.string.action_announce)
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = border,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_speak), contentDescription = if (iconOnly) label else null)
            if (!iconOnly) {
                Spacer(Modifier.width(10.dp))
                Text(label, fontFamily = fontFamily, fontWeight = fontWeight, fontSize = fontSize)
            }
        }
    }
}
