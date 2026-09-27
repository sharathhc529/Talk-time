package io.github.sharathhc529.talktime.ui.clock

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

internal object AnalogPalette {
    val background = Color(0xFFE9E6DF)
    val face = Color(0xFFF7F6F2)
    val ink = Color(0xFF1D1D1B)
    val muted = Color(0xFF5F5D57)
    val minorTick = Color(0xFF9A978F)
    val accent = Color(0xFFE0A800)
}

/** Classic analog: a clean clock face with a yellow second hand, the digital time above it. */
@Composable
internal fun AnalogClock(info: ClockInfo, onSpeak: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(AnalogPalette.background)) {
        val landscape = maxWidth > maxHeight
        val faceSize = if (landscape) maxHeight * 0.78f else min(maxWidth * 0.84f, maxHeight * 0.46f)
        val heading = @Composable {
            Column(horizontalAlignment = if (landscape) Alignment.Start else Alignment.CenterHorizontally) {
                Text(
                    text = listOfNotNull("${info.shortHours}:${info.minutes}", info.amPm).joinToString(" "),
                    color = AnalogPalette.ink,
                    fontFamily = ClockFonts.manrope,
                    fontWeight = FontWeight.Medium,
                    fontSize = 44.sp,
                    letterSpacing = (-0.02).em,
                )
                Text(info.date("EEEEdMMMM"), color = AnalogPalette.muted, fontFamily = ClockFonts.manrope, fontSize = 16.sp)
            }
        }
        val button = @Composable {
            SpeakButton(
                onClick = onSpeak,
                contentColor = AnalogPalette.face,
                containerColor = AnalogPalette.ink,
                shape = RoundedCornerShape(50),
                fontFamily = ClockFonts.manrope,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().height(64.dp),
            )
        }

        if (landscape) {
            Row(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Face(info, faceSize)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    heading()
                    button()
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.padding(top = 16.dp)) { heading() }
                Face(info, faceSize)
                button()
            }
        }
    }
}

@Composable
private fun Face(info: ClockInfo, size: Dp) {
    val measurer = rememberTextMeasurer()
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2
        val c = center
        // A soft shadow below the face, then the face itself.
        drawCircle(
            Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.16f), Color.Transparent), c + Offset(0f, r * 0.08f), r * 1.1f),
            radius = r * 1.1f,
            center = c + Offset(0f, r * 0.08f),
        )
        drawCircle(AnalogPalette.face, r, c)

        for (i in 0 until 60) {
            val major = i % 5 == 0
            val length = if (major) r * 0.09f else r * 0.045f
            rotate(i * 6f, c) {
                drawLine(
                    color = if (major) AnalogPalette.ink else AnalogPalette.minorTick,
                    start = Offset(c.x, c.y - r * 0.93f),
                    end = Offset(c.x, c.y - r * 0.93f + length),
                    strokeWidth = if (major) r * 0.02f else r * 0.008f,
                )
            }
        }
        val numberStyle = TextStyle(fontFamily = ClockFonts.manrope, fontWeight = FontWeight.Medium, fontSize = (r * 0.12f).toSp(), color = AnalogPalette.ink)
        for (n in 1..12) {
            val layout = measurer.measure(n.toString(), numberStyle)
            val angle = Math.toRadians(n * 30.0)
            val at = Offset(c.x + r * 0.7f * sin(angle).toFloat(), c.y - r * 0.7f * cos(angle).toFloat())
            drawText(layout, topLeft = at - Offset(layout.size.width / 2f, layout.size.height / 2f))
        }

        val minutes = info.minute + info.second / 60f
        hand((info.hour % 12 + minutes / 60f) * 30f, r * 0.5f, r * 0.06f, AnalogPalette.ink)
        hand(minutes * 6f, r * 0.78f, r * 0.045f, AnalogPalette.ink)
        hand(info.second * 6f, r * 0.84f, r * 0.014f, AnalogPalette.accent, tail = r * 0.18f)
        drawCircle(AnalogPalette.accent, r * 0.05f, c)
    }
}

/** A hand turned [degrees] clockwise from 12, [length] long, with an optional [tail] behind the centre. */
private fun DrawScope.hand(degrees: Float, length: Float, width: Float, color: Color, tail: Float = 0f) {
    rotate(degrees, center) {
        drawLine(color, Offset(center.x, center.y + tail), Offset(center.x, center.y - length), width, StrokeCap.Round)
    }
}
