package io.github.sharathhc529.talktime.ui.clock

import androidx.annotation.FontRes
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.github.sharathhc529.talktime.R

/** The typefaces of the clock styles. Each is one variable font file, set to the weights used. */
internal object ClockFonts {
    val oswald = family(R.font.oswald, FontWeight.Normal, FontWeight.SemiBold)
    val bricolage = family(R.font.bricolage, FontWeight.Medium, FontWeight.ExtraBold)
    val fraunces = family(R.font.fraunces, FontWeight.Light, FontWeight.SemiBold, opticalSize = 144f)
    val spaceGrotesk = family(R.font.space_grotesk, FontWeight.Normal, FontWeight.Medium)
    val manrope = family(R.font.manrope, FontWeight.Light, FontWeight.Medium, FontWeight.Bold)
}

@OptIn(ExperimentalTextApi::class)
private fun family(@FontRes font: Int, vararg weights: FontWeight, opticalSize: Float? = null) = FontFamily(
    weights.map { weight ->
        val axes = listOfNotNull(
            FontVariation.weight(weight.weight),
            opticalSize?.let { FontVariation.Setting("opsz", it) },
        )
        Font(font, weight, variationSettings = FontVariation.Settings(*axes.toTypedArray()))
    },
)
