package io.github.sharathhc529.talktime.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.sharathhc529.talktime.R
import io.github.sharathhc529.talktime.settings.BoolPref
import io.github.sharathhc529.talktime.settings.IntPref
import io.github.sharathhc529.talktime.settings.IntSetPref
import io.github.sharathhc529.talktime.settings.Pref
import io.github.sharathhc529.talktime.settings.Settings
import io.github.sharathhc529.talktime.settings.mutable
import kotlin.math.roundToInt

@Composable
fun PreferenceCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

/** A row with a title, an optional summary and an optional control on the right. */
@Composable
fun PreferenceRow(
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .alpha(if (enabled) 1f else 0.38f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (!summary.isNullOrEmpty()) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(16.dp))
            trailing()
        }
    }
}

/** Opens a sub-screen. */
@Composable
fun ScreenLink(title: String, summary: String?, onClick: () -> Unit) {
    PreferenceRow(title, summary, onClick = onClick) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
fun SwitchPreference(
    settings: Settings,
    pref: BoolPref,
    title: String,
    summary: String? = null,
    summaryOff: String? = summary,
    enabled: Boolean = true,
    onChange: ((Boolean) -> Unit)? = null,
) {
    var value by settings.mutable(pref)
    val toggle = { checked: Boolean -> if (onChange != null) onChange(checked) else value = checked }
    PreferenceRow(title, if (value) summary else summaryOff, enabled, onClick = { toggle(!value) }) {
        Switch(checked = value, onCheckedChange = toggle, enabled = enabled)
    }
}

/** Picks one of [options]; the summary shows the selected option unless [summary] is given. */
@Composable
fun <T> ListPreference(
    settings: Settings,
    pref: Pref<T>,
    title: String,
    options: List<Pair<T, String>>,
    enabled: Boolean = true,
    summary: ((T) -> String)? = null,
) {
    var value by settings.mutable(pref)
    var open by remember { mutableStateOf(false) }
    val label = options.firstOrNull { it.first == value }?.second.orEmpty()
    PreferenceRow(title, summary?.invoke(value) ?: label, enabled, onClick = { open = true })
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    itemsIndexed(options) { _, (option, name) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { value = option; open = false }
                                .padding(vertical = 4.dp),
                        ) {
                            RadioButton(selected = option == value, onClick = { value = option; open = false })
                            Text(name, Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.app_cancel)) } },
        )
    }
}

/** Chooses a number with a slider in a dialog. */
@Composable
fun SliderPreference(
    settings: Settings,
    pref: IntPref,
    title: String,
    range: IntRange,
    step: Int = 1,
    enabled: Boolean = true,
    format: (Int) -> String,
    summary: (Int) -> String = format,
) {
    var value by settings.mutable(pref)
    var open by remember { mutableStateOf(false) }
    PreferenceRow(title, summary(value), enabled, onClick = { open = true })
    if (open) {
        var draft by remember { mutableFloatStateOf(value.toFloat()) }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                Column {
                    Text(format(draft.roundToInt()), style = MaterialTheme.typography.headlineSmall)
                    Slider(
                        value = draft,
                        onValueChange = { draft = (it / step).roundToInt() * step.toFloat() },
                        valueRange = range.first.toFloat()..range.last.toFloat(),
                        steps = ((range.last - range.first) / step - 1).coerceAtLeast(0),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { value = draft.roundToInt().coerceIn(range); open = false }) {
                    Text(stringResource(R.string.app_ok))
                }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.app_cancel)) } },
        )
    }
}

/** Selects several values. */
@Composable
fun MultiSelectPreference(
    settings: Settings,
    pref: IntSetPref,
    title: String,
    options: List<Pair<Int, String>>,
    summary: (Set<Int>) -> String,
) {
    var value by settings.mutable(pref)
    var open by remember { mutableStateOf(false) }
    PreferenceRow(title, summary(value), onClick = { open = true })
    if (open) {
        var draft by remember { mutableStateOf(value) }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                Column {
                    options.forEach { (option, name) ->
                        val checked = option in draft
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { draft = if (checked) draft - option else draft + option },
                        ) {
                            Checkbox(checked = checked, onCheckedChange = { draft = if (it) draft + option else draft - option })
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { value = draft; open = false }) { Text(stringResource(R.string.app_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.app_cancel)) } },
        )
    }
}

private val presetColors = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFFFF0000, 0xFFB71C1C, 0xFFFF9800, 0xFFFFEB3B, 0xFF4CAF50, 0xFF1B5E20,
    0xFF00BCD4, 0xFF2196F3, 0xFF0D47A1, 0xFF3F51B5, 0xFF9C27B0, 0xFFE91E63, 0xFF795548, 0xFF607D8B,
).map { Color(it) }

/** Chooses a colour from presets or with hue, saturation and brightness sliders. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPreference(settings: Settings, pref: IntPref, title: String, summary: String) {
    var value by settings.mutable(pref)
    var open by remember { mutableStateOf(false) }
    PreferenceRow(title, summary, onClick = { open = true }) { ColorSwatch(Color(value)) }
    if (open) {
        val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(value, it) } }
        var hue by remember { mutableFloatStateOf(hsv[0]) }
        var saturation by remember { mutableFloatStateOf(hsv[1]) }
        var brightness by remember { mutableFloatStateOf(hsv[2]) }
        val color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)))
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                Column {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(color, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 12.dp),
                    ) {
                        presetColors.forEach { preset ->
                            ColorSwatch(
                                preset,
                                Modifier.clickable {
                                    val values = FloatArray(3)
                                    android.graphics.Color.colorToHSV(preset.toArgb(), values)
                                    hue = values[0]; saturation = values[1]; brightness = values[2]
                                },
                            )
                        }
                    }
                    Text(stringResource(R.string.color_hue))
                    Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
                    Text(stringResource(R.string.color_saturation))
                    Slider(value = saturation, onValueChange = { saturation = it })
                    Text(stringResource(R.string.color_brightness))
                    Slider(value = brightness, onValueChange = { brightness = it })
                }
            },
            confirmButton = {
                TextButton(onClick = { value = color.toArgb(); open = false }) { Text(stringResource(R.string.app_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.app_cancel)) } },
        )
    }
}

@Composable
private fun ColorSwatch(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(32.dp)
            .background(color, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
    )
}
