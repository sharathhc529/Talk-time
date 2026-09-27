package io.github.sharathhc529.talktime.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.sharathhc529.talktime.BuildConfig
import io.github.sharathhc529.talktime.R
import io.github.sharathhc529.talktime.service.TalkTimeService
import io.github.sharathhc529.talktime.speech.SpeakingInterval

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    dismiss: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismiss) } },
    )
}

/** Lets the user pick how often the speaking clock announces the time. */
@Composable
fun IntervalDialog(withSeconds: Boolean, onSelect: (SpeakingInterval) -> Unit, onDismiss: () -> Unit) {
    val intervals = SpeakingInterval.entries.filter { withSeconds || !it.usesSeconds }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_pulse_interval_title)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                items(intervals) { interval ->
                    Text(
                        text = stringResource(TalkTimeService.intervalName(interval)),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(interval) }
                            .padding(vertical = 14.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.app_cancel)) } },
    )
}

@Composable
fun InfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_name)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.app_version_label) + " " + BuildConfig.VERSION_NAME)
                Text(
                    stringResource(R.string.info_credits),
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    stringResource(R.string.info_privacy),
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    stringResource(R.string.app_disclaimer_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(stringResource(R.string.info_disclaimer), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.app_ok)) } },
    )
}
