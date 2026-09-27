package io.github.sharathhc529.talktime.ui.clock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import io.github.sharathhc529.talktime.R
import io.github.sharathhc529.talktime.settings.Settings

private val GlassBackground = Color(0xFF0D0B1A)
private val GlassFill = Color.White.copy(alpha = 0.12f)
private val GlassEdge = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.12f)))

/**
 * Liquid glass: the time on a frosted card over soft glowing colours. The glow is drawn as
 * gradients rather than blurred, so it looks the same on every Android version.
 */
@Composable
internal fun GlassClock(settings: Settings, info: ClockInfo, onSpeak: () -> Unit) {
    val next = nextAnnouncement(settings)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(GlassBackground)
            .drawBehind { drawGlow() },
    ) {
        val landscape = maxWidth > maxHeight
        val timeSize = with(LocalDensity.current) { (if (landscape) min(maxWidth * 0.16f, maxHeight * 0.34f) else maxWidth * 0.3f).toSp() }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = info.date("EEEEdMMMM"),
                color = Color.White,
                fontFamily = ClockFonts.manrope,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .glass(RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )

            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(44.dp))
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${info.shortHours}:${info.minutes}",
                        color = Color.White,
                        style = TextStyle(
                            fontFamily = ClockFonts.manrope,
                            fontWeight = FontWeight.Light,
                            fontSize = timeSize,
                            letterSpacing = (-0.04).em,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                        ),
                        maxLines = 1,
                    )
                    if (info.amPm != null) {
                        Text(
                            text = info.amPm,
                            color = Color.White,
                            fontFamily = ClockFonts.manrope,
                            fontWeight = FontWeight.Medium,
                            fontSize = 22.sp,
                            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
                        )
                    }
                }
                if (next != null) {
                    Text(
                        text = stringResource(R.string.clock_next_announcement_at, info.time(next)),
                        color = Color.White.copy(alpha = 0.85f),
                        fontFamily = ClockFonts.manrope,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                    )
                }
            }

            SpeakButton(
                onClick = onSpeak,
                contentColor = Color.White,
                containerColor = Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, GlassEdge),
                fontFamily = ClockFonts.manrope,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().height(68.dp),
            )
        }
    }
}

private fun Modifier.glass(shape: RoundedCornerShape) = this
    .background(GlassFill, shape)
    .border(BorderStroke(1.dp, GlassEdge), shape)

/** Three soft blobs of colour: coral at the upper left, violet at the right, teal at the bottom. */
private fun DrawScope.drawGlow() {
    val w = size.width
    val h = size.height
    val r = minOf(w, h)
    blob(Color(0xFFFF7A59), Offset(w * 0.1f, h * 0.3f), r * 0.85f)
    blob(Color(0xFF7B5CFF), Offset(w * 0.95f, h * 0.58f), r * 0.85f)
    blob(Color(0xFF22C1C3), Offset(w * 0.35f, h * 1.02f), r * 0.8f)
}

private fun DrawScope.blob(color: Color, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            0f to color.copy(alpha = 0.85f),
            0.5f to color.copy(alpha = 0.35f),
            1f to Color.Transparent,
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
