package io.github.sharathhc529.talktime.speech

import io.github.sharathhc529.talktime.speech.lang.BrazilianPortuguese
import io.github.sharathhc529.talktime.speech.lang.Czech
import io.github.sharathhc529.talktime.speech.lang.Danish
import io.github.sharathhc529.talktime.speech.lang.Dutch
import io.github.sharathhc529.talktime.speech.lang.English
import io.github.sharathhc529.talktime.speech.lang.French
import io.github.sharathhc529.talktime.speech.lang.German
import io.github.sharathhc529.talktime.speech.lang.Hindi
import io.github.sharathhc529.talktime.speech.lang.Hungarian
import io.github.sharathhc529.talktime.speech.lang.Indonesian
import io.github.sharathhc529.talktime.speech.lang.Italian
import io.github.sharathhc529.talktime.speech.lang.Korean
import io.github.sharathhc529.talktime.speech.lang.LatinAmericanSpanish
import io.github.sharathhc529.talktime.speech.lang.Polish
import io.github.sharathhc529.talktime.speech.lang.Portuguese
import io.github.sharathhc529.talktime.speech.lang.Romanian
import io.github.sharathhc529.talktime.speech.lang.Russian
import io.github.sharathhc529.talktime.speech.lang.SimplifiedChinese
import io.github.sharathhc529.talktime.speech.lang.Slovak
import io.github.sharathhc529.talktime.speech.lang.Spanish
import io.github.sharathhc529.talktime.speech.lang.Thai
import io.github.sharathhc529.talktime.speech.lang.TraditionalChinese
import io.github.sharathhc529.talktime.speech.lang.Turkish
import java.util.Locale

/**
 * A language the time can be announced in: the phrase generator, the locale requested from the
 * text-to-speech engine, the welcome message and the name shown in the settings.
 */
enum class SpeechLanguage(
    val generator: TimePhraseGenerator,
    val ttsLocale: Locale,
    val displayName: String,
    val welcome: String,
) {
    CS_CZ(Czech, Locale("cs"), "Čeština", "Přivítání"),
    DA_DK(Danish, Locale("da"), "Dansk", "Velkommen"),
    DE_DE(German, Locale("de"), "Deutsch", "Willkommen"),
    EN_GB(English, Locale.UK, "English (United Kingdom)", "Welcome"),
    EN_US(English, Locale.US, "English (United States)", "Welcome"),
    EN_IN(English, Locale("en", "IN"), "English (India)", "Welcome"),
    EN_AU(English, Locale("en", "AU"), "English (Australia)", "Welcome"),
    ES_ES(Spanish, Locale("es", "ES"), "Español (España)", "Bienvenido"),
    ES_US(LatinAmericanSpanish, Locale("es", "US"), "Español (Estados Unidos)", "Bienvenido"),
    ES_MX(LatinAmericanSpanish, Locale("es", "MX"), "Español (México)", "Bienvenido"),
    FR_FR(French, Locale("fr"), "Français", "Bienvenue"),
    IT_IT(Italian, Locale("it"), "Italiano", "Bentornato"),
    HU_HU(Hungarian, Locale("hu"), "Magyar", "Üdvözlet"),
    NL_NL(Dutch, Locale("nl"), "Nederlands", "Welkom"),
    PL_PL(Polish, Locale("pl"), "Polski", "Witamy"),
    PT_PT(Portuguese, Locale("pt", "PT"), "Português (Portugal)", "Bem-vindo"),
    PT_BR(BrazilianPortuguese, Locale("pt", "BR"), "Português (Brasil)", "Bem-vindo"),
    RU_RU(Russian, Locale("ru"), "Русский", "Приветствие"),
    RO_RO(Romanian, Locale("ro"), "Română", "Bun venit"),
    SK_SK(Slovak, Locale("sk"), "Slovenčina", "Vitajte"),
    TR_TR(Turkish, Locale("tr"), "Türkçe", "Karşılama"),
    ZH_CN(SimplifiedChinese, Locale("zh", "CN"), "普通话（中国）", "欢迎"),
    ZH_TW(TraditionalChinese, Locale("zh", "TW"), "中文 (台灣)", "歡迎"),
    ZH_HK(TraditionalChinese, Locale("yue", "HK"), "粵文 (香港)", "歡迎"),
    HI_IN(Hindi, Locale("hi", "IN"), "हिन्दी", "स्वागत"),
    TH_TH(Thai, Locale("th"), "ไทย", "ยินดีต้อนรับ"),
    ID_ID(Indonesian, Locale("id"), "Bahasa Indonesia", "Menyambut"),
    KO_KR(Korean, Locale("ko"), "한국어", "환영"),
    ;

    fun phrase(time: ClockTime, options: PhraseOptions): String =
        generator.phrase(time, options.copy(seconds = options.seconds && generator.supportsSeconds))

    companion object {
        /** Picks the best matching language for a device locale, falling back to US English. */
        fun forLocale(locale: Locale): SpeechLanguage {
            val country = locale.country.uppercase(Locale.ROOT)
            return when (locale.language) {
                "de" -> DE_DE
                "en" -> when (country) {
                    "GB", "IE" -> EN_GB
                    "IN" -> EN_IN
                    "AU", "NZ" -> EN_AU
                    else -> EN_US
                }
                "fr" -> FR_FR
                "es" -> when (country) {
                    "ES", "" -> ES_ES
                    "MX" -> ES_MX
                    else -> ES_US
                }
                "it" -> IT_IT
                "cs" -> CS_CZ
                "pl" -> PL_PL
                "tr" -> TR_TR
                "pt" -> if (country == "BR") PT_BR else PT_PT
                "nl" -> NL_NL
                "ru" -> RU_RU
                "zh" -> when {
                    country == "HK" || country == "MO" -> ZH_HK
                    country == "TW" || locale.script == "Hant" -> ZH_TW
                    else -> ZH_CN
                }
                "yue" -> ZH_HK
                "sk" -> SK_SK
                "hu" -> HU_HU
                "hi" -> HI_IN
                "th" -> TH_TH
                "id", "in" -> ID_ID
                "ko" -> KO_KR
                "da" -> DA_DK
                "ro" -> RO_RO
                else -> EN_US
            }
        }
    }
}
