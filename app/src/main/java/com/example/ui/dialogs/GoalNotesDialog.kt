package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.GoalEntity
import com.example.data.model.GoalNoteEntity
import com.example.util.ClipboardHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GoalNotesDialog(
    goal: GoalEntity,
    notes: List<GoalNoteEntity>,
    onAddNote: (content: String) -> Unit,
    onUpdateNote: (note: GoalNoteEntity) -> Unit,
    onDeleteNote: (id: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var newNoteText by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault()) }

    var noteToEdit by remember { mutableStateOf<GoalNoteEntity?>(null) }
    var noteToDelete by remember { mutableStateOf<GoalNoteEntity?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Notes: ${goal.name}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // "Copy All" button
                    if (notes.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val allNotesText = buildString {
                                    appendLine("Notes for Goal: ${goal.name}")
                                    appendLine("--------------------------------")
                                    notes.forEach { n ->
                                        appendLine("[${dateFormat.format(Date(n.timestamp))}]")
                                        appendLine(n.content)
                                        appendLine()
                                    }
                                }
                                ClipboardHelper.copyToClipboard(context, "All Notes", allNotesText.trimEnd())
                            },
                            modifier = Modifier.size(32.dp).testTag("copy_all_notes_button")
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy All Notes",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // New note input row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newNoteText,
                        onValueChange = { newNoteText = it },
                        placeholder = { Text("Write a note about this goal...") },
                        maxLines = 3,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_note_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            val trimmed = newNoteText.trim()
                            if (trimmed.isNotEmpty()) {
                                onAddNote(trimmed)
                                newNoteText = ""
                            }
                        },
                        modifier = Modifier.testTag("submit_note_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Add Note",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (notes.isEmpty()) {
                    Text(
                        text = "No notes added yet. Record milestones, reflections, or reminders here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(280.dp)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = note.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = dateFormat.format(Date(note.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Copy note button
                                        IconButton(
                                            onClick = {
                                                ClipboardHelper.copyToClipboard(context, "Note", note.content)
                                            },
                                            modifier = Modifier.size(28.dp).testTag("copy_note_${note.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = "Copy Note",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Edit note button
                                        IconButton(
                                            onClick = { noteToEdit = note },
                                            modifier = Modifier.size(28.dp).testTag("edit_note_${note.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit Note",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Delete note button (triggers confirmation)
                                        IconButton(
                                            onClick = { noteToDelete = note },
                                            modifier = Modifier.size(28.dp).testTag("delete_note_${note.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete Note",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("notes_dialog_close_button")) {
                Text("Close")
            }
        }
    )

    // Edit Note Dialog
    noteToEdit?.let { note ->
        var editContent by remember { mutableStateOf(note.content) }
        AlertDialog(
            onDismissRequest = { noteToEdit = null },
            title = { Text("Edit Note", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                OutlinedTextField(
                    value = editContent,
                    onValueChange = { editContent = it },
                    label = { Text("Note Content") },
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("edit_note_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editContent.trim()
                        if (trimmed.isNotEmpty()) {
                            onUpdateNote(note.copy(content = trimmed))
                        }
                        noteToEdit = null
                    },
                    modifier = Modifier.testTag("save_note_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Note Confirmation Dialog
    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Note?") },
            text = { Text("Are you sure you want to delete this note: \"${note.content.take(60)}${if (note.content.length > 60) "..." else ""}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteNote(note.id)
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_note_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
