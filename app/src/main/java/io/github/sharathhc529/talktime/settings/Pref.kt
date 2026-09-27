package io.github.sharathhc529.talktime.settings

import android.content.SharedPreferences
import androidx.core.content.edit

/** A typed key in [SharedPreferences] with its default value. */
sealed class Pref<T>(val key: String, val default: T) {
    abstract fun read(prefs: SharedPreferences): T
    abstract fun write(editor: SharedPreferences.Editor, value: T)
}

class BoolPref(key: String, default: Boolean) : Pref<Boolean>(key, default) {
    override fun read(prefs: SharedPreferences) = prefs.getBoolean(key, default)
    override fun write(editor: SharedPreferences.Editor, value: Boolean) {
        editor.putBoolean(key, value)
    }
}

class IntPref(key: String, default: Int, val range: IntRange = Int.MIN_VALUE..Int.MAX_VALUE) : Pref<Int>(key, default) {
    override fun read(prefs: SharedPreferences) = prefs.getInt(key, default).coerceIn(range)
    override fun write(editor: SharedPreferences.Editor, value: Int) {
        editor.putInt(key, value.coerceIn(range))
    }
}

class StringPref(key: String, default: String) : Pref<String>(key, default) {
    override fun read(prefs: SharedPreferences) = prefs.getString(key, default) ?: default
    override fun write(editor: SharedPreferences.Editor, value: String) {
        editor.putString(key, value)
    }
}

/** An enum stored by name; unknown names fall back to the default. */
class EnumPref<E : Enum<E>>(key: String, default: E, private val values: Array<E>) : Pref<E>(key, default) {
    override fun read(prefs: SharedPreferences): E {
        val name = prefs.getString(key, null) ?: return default
        return values.firstOrNull { it.name == name } ?: default
    }

    override fun write(editor: SharedPreferences.Editor, value: E) {
        editor.putString(key, value.name)
    }
}

/** An optional enum: null means "use the system default". */
class OptionalEnumPref<E : Enum<E>>(key: String, private val values: Array<E>) : Pref<E?>(key, null) {
    override fun read(prefs: SharedPreferences): E? {
        val name = prefs.getString(key, null) ?: return null
        return values.firstOrNull { it.name == name }
    }

    override fun write(editor: SharedPreferences.Editor, value: E?) {
        if (value == null) editor.remove(key) else editor.putString(key, value.name)
    }
}

class IntSetPref(key: String, default: Set<Int>) : Pref<Set<Int>>(key, default) {
    override fun read(prefs: SharedPreferences): Set<Int> {
        val raw = prefs.getString(key, null) ?: return default
        return raw.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    override fun write(editor: SharedPreferences.Editor, value: Set<Int>) {
        editor.putString(key, value.sorted().joinToString(","))
    }
}

operator fun <T> SharedPreferences.get(pref: Pref<T>): T = pref.read(this)

operator fun <T> SharedPreferences.set(pref: Pref<T>, value: T) = edit { pref.write(this, value) }
