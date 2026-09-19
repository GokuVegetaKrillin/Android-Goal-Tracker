package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GoalProgressCalculator
import com.example.service.TimerState
import com.example.ui.GoalWithProgress

@Composable
fun GoalCard(
    goalWithProgress: GoalWithProgress,
    timerState: TimerState,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onToggleFavorite: () -> Unit,
    onManualAdd: () -> Unit,
    onViewStats: () -> Unit,
    onViewNotes: () -> Unit,
    onEdit: () -> Unit,
    onReset: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onMarkComplete: () -> Unit,
    onMarkIncomplete: () -> Unit,
    onDelete: () -> Unit,
    onCreateCertificate: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val goal = goalWithProgress.goal
    val progress = goalWithProgress.progress
    val isCurrentTimer = timerState.goalId == goal.id
    val isRunning = isCurrentTimer && timerState.isRunning && !timerState.isPaused
    var menuExpanded by remember { mutableStateOf(false) }

    val accentColor = remember(goal.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(goal.colorHex))
        } catch (e: Exception) {
            Color(0xFF8B5CF6)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("goal_card_${goal.id}"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewStats() }
        ) {
            // Left color accent bar matching screenshot
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(112.dp)
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // Top row: Goal title & Progress ring
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = goal.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (goal.isFavorite) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Favorite",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Subtitle line (e.g. "57 sec left until target" or "Reach 10 hrs every day")
                        Text(
                            text = if (goal.isCompleted) {
                                "Completed • Target reached"
                            } else {
                                "${progress.formattedCurrentStatus} (${goal.recurrenceType.displayName})"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // If average success is available, show subtle tag
                        if (progress.averageSuccessPercentage != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Avg. success: ${progress.averageSuccessPercentage}%" +
                                        if (goal.isCompleted) " (locked)" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (progress.averageSuccessPercentage >= 75) {
                                    Color(0xFF10B981)
                                } else {
                                    MaterialTheme.colorScheme.tertiary
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Progress circular ring with % inside (matching screenshot)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(44.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { progress.progressRatio.toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.size(44.dp),
                            color = accentColor,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            strokeWidth = 3.5.dp,
                        )
                        Text(
                            text = "${progress.progressPercentage}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom actions row: Stats, Manual add / log, Ticker, Play/Pause, Overflow Menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left group: Stats button, Manual time add button, Notes button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onViewStats,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("stats_button_${goal.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Statistics",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onManualAdd,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("manual_time_button_${goal.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Add Time Log",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onViewNotes,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Notes,
                                contentDescription = "Notes",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Right group: Clock / Play-Pause / More menu
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Live elapsed clock if running
                        AnimatedVisibility(visible = isCurrentTimer) {
                            Text(
                                text = GoalProgressCalculator.formatTimerClock(timerState.elapsedSeconds),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = accentColor,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }

                        // Play/Pause button
                        if (!goal.isCompleted && !goal.isArchived) {
                            IconButton(
                                onClick = {
                                    if (isCurrentTimer) {
                                        if (isRunning) onPauseTimer() else onResumeTimer()
                                    } else {
                                        onStartTimer()
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("play_pause_button_${goal.id}")
                            ) {
                                Icon(
                                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isRunning) "Pause" else "Play",
                                    tint = accentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // If completed, show certificate action shortcut
                        if (goal.isCompleted && onCreateCertificate != null) {
                            IconButton(
                                onClick = onCreateCertificate,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = "Certificate",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // More overflow menu
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (goal.isFavorite) "Remove Favorite" else "Add to Favorites") },
                                    leadingIcon = {
                                        Icon(
                                            if (goal.isFavorite) Icons.Default.StarBorder else Icons.Default.Star,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onToggleFavorite()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Edit Goal") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onEdit()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(if (goal.isCompleted) "Mark as Incomplete" else "Mark as Complete") },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        if (goal.isCompleted) onMarkIncomplete() else onMarkComplete()
                                    }
                                )

                                if (goal.isCompleted && onCreateCertificate != null) {
                                    DropdownMenuItem(
                                        text = { Text("Create Certificate") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.WorkspacePremium,
                                                contentDescription = null,
                                                tint = Color(0xFFF59E0B)
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onCreateCertificate()
                                        }
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text(if (goal.isArchived) "Unarchive Goal" else "Archive Goal") },
                                    leadingIcon = {
                                        Icon(
                                            if (goal.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        if (goal.isArchived) onUnarchive() else onArchive()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Reset Progress") },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onReset()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Delete Goal", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
