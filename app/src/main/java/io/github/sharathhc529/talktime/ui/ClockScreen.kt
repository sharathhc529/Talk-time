package io.github.sharathhc529.talktime.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import io.github.sharathhc529.talktime.settings.Keys
import io.github.sharathhc529.talktime.settings.Settings
import io.github.sharathhc529.talktime.settings.TextEffect
import io.github.sharathhc529.talktime.settings.collect
import io.github.sharathhc529.talktime.settings.version
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Full-screen digital clock in the colours and styles chosen in the display settings. */
@Composable
fun ClockScreen(
    settings: Settings,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    overlay: @Composable () -> Unit = {},
) {
    val backgroundColor = Color(settings.collect(Keys.backgroundColor).value)
    val gradient by settings.collect(Keys.gradient)
    val textColor = Color(settings.collect(Keys.textColor).value)
    val effect by settings.collect(Keys.textEffect)
    val timeBold by settings.collect(Keys.timeBold)
    val amPmBold by settings.collect(Keys.amPmBold)
    val dateBold by settings.collect(Keys.dateBold)
    settings.version().value // Recompose when the hour format settings change.
    val is24Hour = settings.displayIs24Hour

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000 - now % 1000)
        }
    }

    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val date = Date(now)
    val timeText = SimpleDateFormat(if (is24Hour) "HH:mm" else "hh:mm", locale).format(date)
    val amPmText = SimpleDateFormat("a", locale).format(date).uppercase(locale)
    val dateText = SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEEyyyyMMMMd"), locale)
        .format(date).uppercase(locale)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush(backgroundColor, gradient)),
    ) {
        val density = LocalDensity.current
        // Sizes follow the original: relative to the screen width, limited so that the clock
        // still fits on wide landscape screens.
        val reference = with(density) { minOf(maxWidth, maxHeight * 1.6f).toPx() }
        fun sp(fraction: Float): TextUnit = with(density) { (reference * fraction).toSp() }
        val shadow = when (effect) {
            TextEffect.NONE -> null
            TextEffect.SHADOW -> {
                val size = reference * 0.004f
                Shadow(Color.Black, Offset(size, size), size)
            }
            TextEffect.GLOW -> Shadow(textColor, Offset.Zero, reference * 0.025f)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onPress = { onTap() }) }
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = timeText,
                    color = textColor,
                    style = clockStyle(sp(0.26f), timeBold, shadow),
                )
                if (!is24Hour) {
                    Text(
                        text = amPmText,
                        color = textColor,
                        style = clockStyle(sp(0.05f), amPmBold, shadow),
                        modifier = Modifier.padding(bottom = with(density) { (reference * 0.035f).toDp() }),
                    )
                }
            }
            Text(
                text = dateText,
                color = textColor,
                textAlign = TextAlign.Center,
                style = clockStyle(sp(0.05f), dateBold, shadow),
            )
        }
        Box(Modifier.fillMaxSize()) { overlay() }
    }
}

private fun clockStyle(size: TextUnit, bold: Boolean, shadow: Shadow?) = TextStyle(
    fontSize = size,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    shadow = shadow,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
)

/**
 * A radial gradient from the chosen colour in the centre to a very dark shade of it at the edges.
 * At 100 % the background is a single colour.
 */
private fun backgroundBrush(color: Color, gradient: Int): Brush {
    if (gradient >= 100) return SolidColor(color)
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[2] *= 0.05f
    val edge = Color(android.graphics.Color.HSVToColor(hsv))
    return RadialBrush(color, edge, gradient / 100f + 0.6f)
}

/** Radial gradient whose radius is [factor] times the screen's smaller side. */
private data class RadialBrush(val inner: Color, val outer: Color, val factor: Float) : ShaderBrush() {
    override fun createShader(size: Size): Shader = RadialGradientShader(
        center = size.center,
        radius = (minOf(size.width, size.height) * factor).coerceAtLeast(1f),
        colors = listOf(inner, outer),
    )
}
