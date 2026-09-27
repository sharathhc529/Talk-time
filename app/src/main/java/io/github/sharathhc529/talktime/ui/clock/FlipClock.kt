package io.github.sharathhc529.talktime.ui.clock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import io.github.sharathhc529.talktime.settings.Keys
import io.github.sharathhc529.talktime.settings.Settings
import io.github.sharathhc529.talktime.settings.collect

/** Digit size as a fraction of the card height. */
private const val DIGIT_SIZE = 0.76f

/**
 * Retro flip clock: the hours and minutes on two split-flap cards whose top half folds down
 * whenever the digits change. Its colours and text weights are display settings.
 */
@Composable
internal fun FlipClock(settings: Settings, info: ClockInfo, onSpeak: () -> Unit) {
    val backgroundColor = Color(settings.collect(Keys.flipBackgroundColor).value)
    val cardColor = Color(settings.collect(Keys.cardColor).value)
    val textColor = Color(settings.collect(Keys.textColor).value)
    val timeBold by settings.collect(Keys.timeBold)
    val amPmBold by settings.collect(Keys.amPmBold)
    val dateBold by settings.collect(Keys.dateBold)
    val secondary = textColor.copy(alpha = 0.6f)

    BoxWithConstraints(Modifier.fillMaxSize().background(backgroundColor)) {
        val landscape = maxWidth > maxHeight
        // Card size: two cards stacked in portrait, side by side in landscape.
        val cardWidth = if (landscape) min(maxWidth * 0.36f, maxHeight * 0.75f) else min(maxWidth * 0.78f, maxHeight * 0.4f)
        val cardHeight = if (landscape) cardWidth * 0.95f else min(cardWidth * 0.8f, maxHeight * 0.3f)
        val gap = cardHeight * 0.07f
        val labelSize = with(LocalDensity.current) { (cardHeight * 0.07f).toSp() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = info.date("EEEEdMMM").uppercase(info.locale),
                color = secondary,
                style = labelStyle(labelSize, dateBold),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 32.dp),
            )

            val hourCard = @Composable {
                FlipCard(info.hours, cardWidth, cardHeight, cardColor, backgroundColor, textColor, timeBold) {
                    if (info.amPm != null) {
                        Text(
                            text = info.amPm,
                            color = secondary,
                            style = labelStyle(labelSize, amPmBold),
                            modifier = Modifier.align(Alignment.BottomStart).padding(cardHeight * 0.06f),
                        )
                    }
                }
            }
            val minuteCard = @Composable {
                FlipCard(info.minutes, cardWidth, cardHeight, cardColor, backgroundColor, textColor, timeBold)
            }
            if (landscape) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) { hourCard(); minuteCard() }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(gap)) { hourCard(); minuteCard() }
            }

            SpeakButton(
                onClick = onSpeak,
                contentColor = textColor,
                containerColor = Color.Transparent,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, textColor.copy(alpha = 0.25f)),
                fontFamily = ClockFonts.oswald,
                fontSize = labelSize * 1.1f,
                modifier = Modifier
                    .then(if (landscape) Modifier.width(cardWidth) else Modifier.fillMaxWidth())
                    .height(60.dp),
            )
        }
    }
}

/**
 * One split-flap card. When [text] changes, the upper flap with the old value folds down to the
 * middle, then the lower flap with the new value falls into place.
 */
@Composable
private fun FlipCard(
    text: String,
    width: Dp,
    height: Dp,
    cardColor: Color,
    backgroundColor: Color,
    textColor: Color,
    bold: Boolean,
    content: @Composable BoxScope.() -> Unit = {},
) {
    var current by remember { mutableStateOf(text) }
    var previous by remember { mutableStateOf(text) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(text) {
        if (text == current) return@LaunchedEffect
        previous = current
        current = text
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 700, easing = FastOutSlowInEasing))
    }

    val density = LocalDensity.current
    val style = TextStyle(
        color = textColor,
        fontFamily = ClockFonts.oswald,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        fontSize = with(density) { (height * DIGIT_SIZE).toSp() },
        textAlign = TextAlign.Center,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
    val radius = height * 0.08f
    val p = progress.value
    val animating = p < 1f

    Box(Modifier.size(width, height)) {
        Column {
            Half(current, top = true, width, height, cardColor, radius, style)
            Half(if (animating) previous else current, top = false, width, height, cardColor, radius, style)
        }
        if (animating) {
            val cameraDistance = with(density) { 16.dp.toPx() } * 12
            if (p < 0.5f) {
                // The old upper half folds down towards the middle, darkening as it turns away.
                Half(
                    previous, top = true, width, height, cardColor, radius, style,
                    Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 1f)
                            rotationX = -180f * p
                            this.cameraDistance = cameraDistance
                        }
                        .shade(p),
                )
            } else {
                // The new lower half falls from the middle into place, brightening as it lands.
                Half(
                    current, top = false, width, height, cardColor, radius, style,
                    Modifier
                        .align(Alignment.BottomCenter)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 0f)
                            rotationX = 180f * (1f - p)
                            this.cameraDistance = cameraDistance
                        }
                        .shade(1f - p),
                )
            }
        }
        // The split across the middle of the card and the notches at its sides.
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .size(width, height * 0.012f)
                .background(backgroundColor),
        )
        val notch = Modifier.size(height * 0.04f, height * 0.08f).background(backgroundColor, RoundedCornerShape(3.dp))
        Box(notch.align(Alignment.CenterStart))
        Box(notch.align(Alignment.CenterEnd))
        content()
    }
}

/** The upper or lower half of a card, showing that half of [text]. */
@Composable
private fun Half(
    text: String,
    top: Boolean,
    width: Dp,
    height: Dp,
    color: Color,
    radius: Dp,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val shape = if (top) {
        RoundedCornerShape(topStart = radius, topEnd = radius)
    } else {
        RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
    }
    Box(
        modifier
            .size(width, height / 2)
            .clip(shape)
            .background(color)
            // The upper half is a touch lighter, like light falling on a real flip clock.
            .then(if (top) Modifier.background(Color.White.copy(alpha = 0.03f)) else Modifier),
    ) {
        Box(
            Modifier
                .wrapContentHeight(if (top) Alignment.Top else Alignment.Bottom, unbounded = true)
                .requiredHeight(height)
                .width(width),
            contentAlignment = Alignment.Center,
        ) {
            // Oswald's figures sit low in their line box; lift them so they are centred on the card.
            Text(text, style = style, maxLines = 1, modifier = Modifier.offset(y = -height * DIGIT_SIZE * 0.135f))
        }
    }
}

private fun Modifier.shade(alpha: Float) = drawWithContent {
    drawContent()
    drawRect(Color.Black.copy(alpha = alpha.coerceIn(0f, 1f)))
}

private fun labelStyle(size: TextUnit, bold: Boolean) = TextStyle(
    fontFamily = ClockFonts.oswald,
    fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
    fontSize = size,
    letterSpacing = size * 0.15f,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)
