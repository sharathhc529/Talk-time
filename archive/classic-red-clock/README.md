# Classic red clock (archived)

The original main screen: a large digital clock over a red radial gradient, with text colour,
shadow/glow effect, background colour and gradient radius options. It was replaced by the retro
flip clock and is kept here, outside the app's build, so it can be brought back.

- `ClockScreen.kt` — the complete screen as it was.
- Git tag `classic-red-clock` — the whole app at the last commit that used this screen.

## Restoring it

The main screen now offers several clock styles (`ui/clock/`, chosen with `Keys.clockStyle`),
so the red clock is easiest to bring back as one more style:

1. Copy `ClockScreen.kt` to `app/src/main/java/io/github/sharathhc529/talktime/ui/clock/ClassicClock.kt`,
   change its package to `...ui.clock`, rename `ClockScreen` to `internal fun ClassicClock(settings: Settings)`
   and remove its tap handling and `overlay` (the shared `ClockScreen` provides both).
2. Add `CLASSIC` to `ClockStyle` in `Settings.kt`, a branch for it in `ClockScreen`, and a name in
   the style list of `DisplayScreen` in `SettingsScreens.kt`.
3. Show the text effect, background colour and gradient options in `DisplayScreen` while that
   style is chosen (see `git show classic-red-clock:app/src/main/java/io/github/sharathhc529/talktime/ui/settings/SettingsScreens.kt`).

The settings the screen reads (`Keys.textEffect`, `Keys.backgroundColor`, `Keys.gradient`) and
their translated texts are still in the app, so users' earlier choices come back with it.
