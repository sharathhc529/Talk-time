package io.github.sharathhc529.talktime.ui.clock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import io.github.sharathhc529.talktime.R
import io.github.sharathhc529.talktime.settings.Settings

internal object StackedPalette {
    val background = Color(0xFF0E0E0E)
    val paper = Color(0xFFF2EDE4)
    val muted = Color(0xFFA8A29A)
    val accent = Color(0xFFFF6B3D)
}

/** Stacked bold: the hours above the minutes in huge heavy type, the minutes in an accent colour. */
@Composable
internal fun StackedClock(settings: Settings, info: ClockInfo, onSpeak: () -> Unit) {
    val next = nextAnnouncement(settings)
    val label = TextStyle(fontFamily = ClockFonts.spaceGrotesk, fontSize = 14.sp, letterSpacing = 0.2.em, color = StackedPalette.muted)

    BoxWithConstraints(Modifier.fillMaxSize().background(StackedPalette.background)) {
        val landscape = maxWidth > maxHeight
        val digitSize = with(LocalDensity.current) {
            (if (landscape) min(maxWidth * 0.3f, maxHeight * 0.6f) else min(maxWidth * 0.64f, maxHeight * 0.3f)).toSp()
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(info.date("EEEdMMM").uppercase(info.locale), style = label, modifier = Modifier.padding(top = 16.dp))

            Column {
                if (landscape) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Digits(info.hours, digitSize, StackedPalette.paper)
                        Digits(info.minutes, digitSize, StackedPalette.accent)
                    }
                } else {
                    Digits(info.hours, digitSize, StackedPalette.paper)
                    Digits(info.minutes, digitSize, StackedPalette.accent)
                }
                val details = listOfNotNull(
                    info.amPm,
                    next?.let { stringResource(R.string.clock_next_short, info.time(it)).uppercase(info.locale) },
                )
                if (details.isNotEmpty()) {
                    Text(details.joinToString("  ·  "), style = label, modifier = Modifier.padding(top = 16.dp))
                }
            }

            SpeakButton(
                onClick = onSpeak,
                contentColor = StackedPalette.paper,
                containerColor = Color.Transparent,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, StackedPalette.paper),
                fontFamily = ClockFonts.bricolage,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().height(60.dp),
            )
        }
    }
}

@Composable
private fun Digits(text: String, size: TextUnit, color: Color) {
    Text(
        text = text,
        color = color,
        style = TextStyle(
            fontFamily = ClockFonts.bricolage,
            fontWeight = FontWeight.ExtraBold,
            fontSize = size,
            lineHeight = size * 0.86f,
            letterSpacing = (-0.05).em,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
        ),
        maxLines = 1,
        // The font's line box is much taller than its figures; keep only the middle of it so the
        // hours and minutes sit close together.
        modifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val height = (placeable.height * 0.78f).toInt()
            layout(placeable.width, height) { placeable.place(0, (height - placeable.height) / 2) }
        },
    )
}
