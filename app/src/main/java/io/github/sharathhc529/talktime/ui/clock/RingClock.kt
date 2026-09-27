package io.github.sharathhc529.talktime.ui.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import io.github.sharathhc529.talktime.R
import io.github.sharathhc529.talktime.settings.Settings

internal object RingPalette {
    val background = Color(0xFF0F1720)
    val text = Color(0xFFE6EDF3)
    val muted = Color(0xFF9FB0C0)
    val track = Color(0xFF1F2B37)
    val accent = Color(0xFF7DD3C0)
}

/** Minute ring: the time inside a ring that fills up over the hour. */
@Composable
internal fun RingClock(settings: Settings, info: ClockInfo, onSpeak: () -> Unit) {
    val next = nextAnnouncement(settings)

    BoxWithConstraints(Modifier.fillMaxSize().background(RingPalette.background)) {
        val landscape = maxWidth > maxHeight
        val ringSize = if (landscape) maxHeight * 0.72f else min(maxWidth * 0.82f, maxHeight * 0.45f)
        val details: @Composable ColumnScope.() -> Unit = {
            Row(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.clock_next_announcement),
                    color = RingPalette.muted,
                    fontFamily = ClockFonts.spaceGrotesk,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = next?.let { info.time(it) } ?: stringResource(R.string.clock_off),
                    color = if (next != null) RingPalette.accent else RingPalette.muted,
                    fontFamily = ClockFonts.spaceGrotesk,
                    fontSize = 15.sp,
                )
            }
            SpeakButton(
                onClick = onSpeak,
                contentColor = RingPalette.background,
                containerColor = RingPalette.accent,
                shape = RoundedCornerShape(20.dp),
                fontFamily = ClockFonts.spaceGrotesk,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().height(64.dp),
            )
        }
        val date = @Composable {
            Text(info.date("EEEEdMMMM"), color = RingPalette.muted, fontFamily = ClockFonts.spaceGrotesk, fontSize = 16.sp)
        }

        if (landscape) {
            Row(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(40.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Ring(info, ringSize)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    date()
                    details()
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(Modifier.padding(top = 16.dp)) { date() }
                Ring(info, ringSize)
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { details() }
            }
        }
    }
}

@Composable
private fun Ring(info: ClockInfo, size: Dp) {
    val progress = (info.minute * 60 + info.second) / 3600f
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = this.size.minDimension * 0.045f
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(RingPalette.track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(
                RingPalette.accent, -90f, 360f * progress, false, Offset(inset, inset), arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${info.shortHours}:${info.minutes}",
                color = RingPalette.text,
                style = TextStyle(
                    fontFamily = ClockFonts.spaceGrotesk,
                    fontWeight = FontWeight.Medium,
                    fontSize = with(LocalDensity.current) { (size * 0.28f).toSp() },
                    letterSpacing = (-0.03).em,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
                maxLines = 1,
            )
            if (info.amPm != null) {
                Text(info.amPm, color = RingPalette.muted, fontFamily = ClockFonts.spaceGrotesk, fontSize = 18.sp, letterSpacing = 0.2.em)
            }
        }
    }
}
