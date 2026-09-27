package io.github.sharathhc529.talktime.settings

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy

/** The current value of [pref], updated whenever it changes. */
@Composable
fun <T> Settings.collect(pref: Pref<T>): State<T> = rememberPrefState(this, pref)

/** A read/write state for [pref]: setting the value stores it. */
@Composable
fun <T> Settings.mutable(pref: Pref<T>): MutableState<T> {
    val state = rememberPrefState(this, pref)
    return remember(state) {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(value) {
                    this@mutable[pref] = value
                }

            override fun component1() = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

/** Recomposes whenever any setting changes; for values derived from several settings. */
@Composable
fun Settings.version(): State<Int> {
    val state = remember { mutableStateOf(0, neverEqual()) }
    DisposableEffect(this) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> state.value++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}

@Composable
private fun <T> rememberPrefState(settings: Settings, pref: Pref<T>): State<T> {
    val state = remember(settings, pref.key) { mutableStateOf(settings[pref], structuralEqualityPolicy()) }
    DisposableEffect(settings, pref.key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == pref.key) state.value = settings[pref]
        }
        settings.prefs.registerOnSharedPreferenceChangeListener(listener)
        state.value = settings[pref]
        onDispose { settings.prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}

private fun <T> neverEqual() = object : SnapshotMutationPolicy<T> {
    override fun equivalent(a: T, b: T) = false
}
