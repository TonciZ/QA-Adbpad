package jp.kaleidot725.adbpad.ui.section.top.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.kaleidot725.adbpad.domain.model.language.Language
import jp.kaleidot725.adbpad.domain.model.setting.WirelessAdbTarget
import jp.kaleidot725.adbpad.ui.component.button.FloatingDialog

@Composable
fun WirelessAdbDialog(
    status: String,
    loading: Boolean,
    initialTarget: WirelessAdbTarget,
    initialName: String,
    isEditing: Boolean,
    onSave: (String, Int, String) -> Unit,
    onRemove: () -> Unit,
    onConnect: (String, Int, String) -> Unit,
    onPair: (String, Int, String) -> Unit,
    onDisconnect: (String, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var host by remember(initialTarget) { mutableStateOf(initialTarget.host) }
    var port by remember(initialTarget) { mutableStateOf(initialTarget.port.toString()) }
    var name by remember(initialName) { mutableStateOf(initialName) }
    var pairingCode by remember { mutableStateOf("") }
    var showHelp by remember { mutableStateOf(false) }

    FloatingDialog(onDismiss = onDismiss, modifier = Modifier.width(400.dp)) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (isEditing) Language.wirelessAdbEditTitle else Language.wirelessAdbTitle,
                style = MaterialTheme.typography.titleMedium,
            )

            // For someone who has never enabled debugging on a TV: the four steps, on demand.
            Text(
                text = Language.wirelessAdbHelpTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { showHelp = !showHelp },
            )
            if (showHelp) {
                Text(
                    text = Language.wirelessAdbHelp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text(Language.wirelessAdbHost) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text(Language.wirelessAdbPort) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(Language.wirelessAdbDeviceName) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = pairingCode,
                onValueChange = { pairingCode = it },
                label = { Text(Language.wirelessAdbPairingCode) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            if (status.isNotEmpty()) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (status.startsWith("Error")) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                )
            }

            if (status.contains("failed", ignoreCase = true) ||
                status.contains("cannot", ignoreCase = true) ||
                status.startsWith("Error")
            ) {
                Text(
                    text = Language.wirelessAdbConnectHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val portNum = port.toIntOrNull() ?: WirelessAdbTarget.DEFAULT_PORT
            val hostValid = host.isNotBlank()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { onConnect(host, portNum, name) },
                    enabled = hostValid && !loading,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(Language.wirelessAdbConnect)
                }

                Button(
                    onClick = { onPair(host, portNum, pairingCode) },
                    enabled = hostValid && pairingCode.isNotBlank() && !loading,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(Language.wirelessAdbPair)
                }
            }

            OutlinedButton(
                onClick = { onDisconnect(host, portNum) },
                enabled = hostValid && !loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(Language.wirelessAdbDisconnect)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Saves without connecting, e.g. to fix the IP of a device that is not reachable yet.
                OutlinedButton(
                    onClick = { onSave(host, portNum, name) },
                    enabled = hostValid && !loading,
                ) {
                    Text(Language.save)
                }

                if (isEditing) {
                    OutlinedButton(onClick = onRemove, enabled = !loading) {
                        Text(Language.delete, color = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                OutlinedButton(onClick = onDismiss) {
                    Text(Language.close)
                }
            }
        }
    }
}
