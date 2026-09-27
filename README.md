# Timesay

A talking clock for current Android versions. The app speaks the time with the phone's text-to-speech engine, on a
schedule or when you tap the screen, wave over the proximity sensor, shake the phone, double-press the
power button or press a headset button. It also works as a night clock.

The original APK targeted Android 6 (API 23) and no longer works reliably on modern phones. This project
recreates it from scratch in Kotlin and Jetpack Compose, targeting Android 16 (API 36) with a minimum of
Android 8.0 (API 26).

## Features

| Feature | Details |
| --- | --- |
| Clock screen | Full-screen clock with date and an announce button, in six styles: retro flip (default, with animated split-flap cards), stacked bold, liquid glass, spoken words, minute ring and classic analog. The flip clock's colours and text weights, the 12/24-hour format and the screen orientation are configurable. Touch it to hear the time. |
| Languages | 23 announcement grammars in 28 language variants: Chinese (Mandarin, Cantonese, Taiwan), Czech, Danish, Dutch, English (US, UK, India, Australia), French, German, Hindi, Hungarian, Indonesian, Italian, Korean, Polish, Portuguese (Portugal, Brazil), Romanian, Russian, Slovak, Spanish (Spain, US, Mexico), Thai and Turkish. |
| Phrasing | Formal ("14 25") or common ("twenty-five past two"), 12 or 24 hours, optional part of day ("in the afternoon"), seconds, and an intro ("It's …"). |
| Interval speaking clock | Every 15/20/30 seconds, every minute, even or odd minutes, every 5/10/15/20/30 minutes or every hour. Uses exact alarms, optionally registered as an alarm clock so announcements are on time in deep sleep. Can start automatically when a headset is plugged in. |
| Night clock | Shows the clock over the lock screen; a wave over the proximity sensor wakes the screen and/or speaks the time. Can start automatically on the charger. |
| Triggers | Proximity gestures (1–3 swipes), shaking (sensitivity and count), power button double press, headset and Bluetooth media buttons (single/double/triple click), opening the app (with optional auto close). |
| Audio | Intro sound, announcement volume independent of the media volume, lowering or pausing other audio during announcements, and the ringer modes (silent/vibrate/normal) in which announcements are allowed. No announcements during calls. |
| Translations | The app's texts are available in the 16 languages of the original app. Texts added in the rebuild are in English. |

## Getting the APK

Every push builds the app on GitHub Actions (`.github/workflows/build.yml`). Open the latest run of
**Build APK** in the repository's *Actions* tab and download the `talk-time-apk` artifact. It contains:

- `app-debug.apk` – debug build.
- `app-release.apk` – optimised build. Without a release keystore it is signed with the debug key, which
  is fine for installing on your own phone.

To sign release builds with your own key, set these environment variables (or GitHub secrets exposed to
the build step): `TALKTIME_KEYSTORE` (path to the keystore file), `TALKTIME_KEYSTORE_PASSWORD`,
`TALKTIME_KEY_ALIAS`, `TALKTIME_KEY_PASSWORD`.

The rebuilt app has its own application ID (`io.github.sharathhc529.timesay`), so it installs next to the
original app instead of replacing it.

## Building locally

Requirements: JDK 17 or newer and the Android SDK (platform 36).

```sh
./gradlew :timespeech:test          # time phrase tests
./gradlew :app:assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
```

## Project structure

- `timespeech/` – plain Kotlin library that turns a time into a sentence in each language
  (`SpeechLanguage`, `lang/*`) and calculates the interval schedule (`SpeakingInterval`).
- `app/` – the Android app:
  - `ui/` – menu and dialogs (`MainActivity`) and the settings screens; `ui/clock/` – the clock styles
    (`ClockScreen` picks the one chosen in the display settings).
  - `service/TalkTimeService` – foreground service that owns the triggers and the speech engine;
    `IntervalScheduler` sets the alarms.
  - `speech/Announcer` – text-to-speech, intro sound, audio focus and volume handling.
  - `trigger/` – proximity, shake, power button, headset button, headset and charger detection.
  - `settings/` – typed settings stored in `SharedPreferences`.

## How the rebuild was verified

The time phrases are the heart of the app, so each language was ported from the original bytecode and
checked against it: the original `classes.dex` was converted to a JVM jar and every language was compared
with the port for all 1,440 minutes of the day, three seconds values and all 32 combinations of options
(about 3.2 million comparisons, all equal). `timespeech/src/test/resources/golden-phrases.tsv` keeps 1,610
of these announcements, recorded from the original app, as a regression test.

## Differences from the original

Deliberate fixes to the announcements:

- **Dutch** – a space was missing before the part of the day ("twee uur's middags"), and "secunde" is now
  spelled "seconde". "'s namiddag" became "'s namiddags".
- **Russian** – several number words mixed Latin letters into Cyrillic ones (e.g. "однa", "двe"), which can
  make the speech engine mispronounce them.
- **Romanian** – uses the correct comma-below letters ș and ț everywhere instead of a mix with the legacy
  cedilla forms ş and ţ.

Changes forced by, or taking advantage of, modern Android:

- The background work runs in a foreground service with a status notification (Android no longer allows
  silent background services). Stopping the speaking clock or night clock is possible from the notification.
- Headset buttons are received through a media session. Android routes media buttons to the app that played
  audio most recently, so while this feature is on, a paused music player may not get its buttons. The
  original forwarded buttons it did not use to the music player; that is no longer possible.
- Calls are detected from the audio mode, so the phone-state permission is no longer needed.
- The night clock shows itself over the lock screen with a full-screen notification; on Android 14 and
  later this needs the "full-screen notifications" permission, which the night clock settings link to.
- The legacy SVOX/IVONA voice download buttons were replaced by a notification that opens the system's
  voice data installer when a language is missing.
- The unfinished scheduler screen of the original (not reachable from its menu) and the links to the
  original author's store page, e-mail and privacy policy were left out.
- Settings are stored under new keys; settings from the original app are not imported.

## Fonts

The clock styles use Oswald, Bricolage Grotesque, Fraunces, Space Grotesk and Manrope, licensed under
the SIL Open Font License 1.1 (see `licenses/`).

## Author and copyright

Timesay is developed by sharathchandrahc. © 2026 sharathchandrahc. All rights reserved for the
code written for this project; no open-source licence is granted at this time.

Parts that belong to others keep their owners' rights:

- The app texts and their translations in `app/src/main/res/values*/strings_original.xml` are not
  covered by this copyright.
- The fonts in `app/src/main/res/font/` are licensed under the SIL Open Font License 1.1 (see `licenses/`).
