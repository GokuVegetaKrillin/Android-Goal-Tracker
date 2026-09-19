package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.GoalEntity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualTimeDialog(
    goal: GoalEntity,
    onDismiss: () -> Unit,
    onSave: (seconds: Long, note: String) -> Unit
) {
    var hoursText by remember { mutableStateOf("0") }
    var minutesText by remember { mutableStateOf("15") }
    var noteText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text(
                    text = "Add Time to ${goal.name}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Manually record time spent on this activity without using the stopwatch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { hoursText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_hours_input")
                    )

                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_minutes_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick add chips
                Text(
                    text = "Quick add presets:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "+5m" to Pair("0", "5"),
                        "+15m" to Pair("0", "15"),
                        "+30m" to Pair("0", "30"),
                        "+45m" to Pair("0", "45"),
                        "+1h" to Pair("1", "0"),
                        "+2h" to Pair("2", "0")
                    ).forEach { (label, pair) ->
                        FilterChip(
                            selected = hoursText == pair.first && minutesText == pair.second,
                            onClick = {
                                hoursText = pair.first
                                minutesText = pair.second
                            },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Session Note (Optional)") },
                    placeholder = { Text("e.g. Practiced Hanon exercises") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = hoursText.toLongOrNull() ?: 0L
                    val m = minutesText.toLongOrNull() ?: 0L
                    val totalSec = h * 3600L + m * 60L
                    if (totalSec > 0) {
                        onSave(totalSec, noteText.trim())
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_manual_time_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.padding(start = 4.dp))
                Text("Add Time")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
