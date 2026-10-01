package io.github.sharathhc529.talktime.ui.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.sharathhc529.talktime.R
import io.github.sharathhc529.talktime.settings.Settings
import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.TimeStyle
import io.github.sharathhc529.talktime.speech.lang.English

internal object WordsPalette {
    val background = Color(0xFFEFE9DD)
    val ink = Color(0xFF22201C)
    val muted = Color(0xFF6B6558)
    val accent = Color(0xFF9A3B24)
}

/**
 * Spoken words: the time written out as the app says it, in the chosen language and style,
 * with the digital time and date in small type.
 */
@Composable
internal fun WordsClock(settings: Settings, info: ClockInfo, onSpeak: () -> Unit) {
    val next = nextAnnouncement(settings)
    // Always the conversational phrasing, like a word clock, whatever style the voice uses.
    val options = settings.phraseOptions().copy(style = TimeStyle.COMMON, timeOfDay = true, introText = true, seconds = false)
    val language = settings.language
    val phrase = language.phrase(ClockTime(info.hour, info.minute), options)
        .let { if (language.generator == English) spellNumbers(it) else it }
    val small = TextStyle(fontFamily = ClockFonts.manrope, fontWeight = FontWeight.Medium, color = WordsPalette.muted)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WordsPalette.background)
            .safeDrawingPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = info.date("EEEEdMMMM").uppercase(info.locale),
            style = small.copy(fontSize = 14.sp, letterSpacing = 0.1.em),
            modifier = Modifier.padding(top = 24.dp, end = 48.dp),
        )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            BasicText(
                text = styledPhrase(phrase),
                style = TextStyle(fontFamily = ClockFonts.fraunces, color = WordsPalette.ink, lineHeight = 1.05.em),
                autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 88.sp, stepSize = 2.sp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = listOfNotNull("${info.shortHours}:${info.minutes}", info.amPm).joinToString(" "),
                    style = small.copy(color = WordsPalette.ink, fontSize = 22.sp),
                )
                if (next != null) {
                    Text(stringResource(R.string.clock_next_announcement_at, info.time(next)), style = small.copy(fontSize = 14.sp))
                }
            }
            SpeakButton(
                onClick = onSpeak,
                contentColor = Color.White,
                containerColor = WordsPalette.accent,
                shape = CircleShape,
                iconOnly = true,
                modifier = Modifier.size(72.dp),
            )
        }
    }
}

/**
 * The phrase with its first word light and muted, its last word in italic accent colour and the
 * words between in bold. Languages written without spaces keep one style throughout.
 */
private fun styledPhrase(phrase: String): AnnotatedString {
    val words = phrase.split(' ')
    val bold = SpanStyle(fontWeight = FontWeight.SemiBold)
    if (words.size < 3) return buildAnnotatedString { withStyle(bold) { append(phrase) } }
    return buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Light, color = WordsPalette.muted)) { append(words.first()) }
        append(' ')
        withStyle(bold) { append(words.subList(1, words.size - 1).joinToString(" ")) }
        append(' ')
        withStyle(SpanStyle(fontWeight = FontWeight.Light, fontStyle = FontStyle.Italic, color = WordsPalette.accent)) {
            append(words.last())
        }
    }
}

private val ones = listOf(
    "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
    "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
)
private val tens = listOf("", "", "twenty", "thirty", "forty", "fifty")

/** Writes out the numbers of an English phrase, which the speech engine gets as digits. */
private fun spellNumbers(phrase: String): String = Regex("""\b\d{1,2}\b""").replace(phrase) { match ->
    val n = match.value.toInt()
    when {
        n < 20 -> ones[n]
        n % 10 == 0 -> tens[n / 10]
        else -> tens[n / 10] + "-" + ones[n % 10]
    }
}
