package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GoalSortOrder
import com.example.data.StartScreen
import com.example.data.model.GoalEntity
import com.example.ui.components.ActiveTimerBar
import com.example.ui.components.GoalCard
import com.example.ui.dialogs.AddEditGoalDialog
import com.example.ui.dialogs.CertificateDialog
import com.example.ui.dialogs.GoalNotesDialog
import com.example.ui.dialogs.GoalSessionsDialog
import com.example.ui.dialogs.GoalStatsDialog
import com.example.ui.dialogs.ManualTimeDialog
import com.example.ui.dialogs.SettingsDialog

enum class ScreenTab(val title: String) {
    ALL_GOALS("Goals"),
    FAVORITE_GOALS("Favorites"),
    COMPLETED_GOALS("Completed"),
    ARCHIVED_GOALS("Archived")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: GoalViewModel) {
    val startScreenPref by viewModel.startScreen.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()

    val activeGoals by viewModel.activeGoalsWithProgress.collectAsState()
    val favoriteGoals by viewModel.favoriteGoalsWithProgress.collectAsState()
    val completedGoals by viewModel.completedGoalsWithProgress.collectAsState()
    val archivedGoals by viewModel.archivedGoalsWithProgress.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val isHeaderExpanded by viewModel.isHeaderExpanded.collectAsState()

    // Determine initial tab based on user preference
    var selectedTabIndex by remember {
        mutableIntStateOf(
            if (startScreenPref == StartScreen.FAVORITE_GOALS) 1 else 0
        )
    }

    // Request notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Dialog state holders
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<GoalEntity?>(null) }
    var goalForManualTime by remember { mutableStateOf<GoalEntity?>(null) }
    var goalForStats by remember { mutableStateOf<GoalWithProgress?>(null) }
    var goalForNotes by remember { mutableStateOf<GoalEntity?>(null) }
    var goalForSessions by remember { mutableStateOf<GoalEntity?>(null) }
    var goalForCertificate by remember { mutableStateOf<GoalEntity?>(null) }
    var goalToDelete by remember { mutableStateOf<GoalEntity?>(null) }
    var goalToReset by remember { mutableStateOf<GoalEntity?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Goal Tracker",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddGoalDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_goal_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal", modifier = Modifier.size(28.dp))
            }
        },
        bottomBar = {
            Column {
                // Active timer bar floating right above the navigation bar
                ActiveTimerBar(
                    timerState = timerState,
                    onPause = { viewModel.pauseTimer() },
                    onResume = { viewModel.resumeTimer() },
                    onStop = { viewModel.stopTimer() },
                    onBarClick = {
                        val activeGoalWithProgress = activeGoals.find { it.goal.id == timerState.goalId }
                        if (activeGoalWithProgress != null) {
                            goalForStats = activeGoalWithProgress
                        }
                    }
                )

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    val tabs = listOf(
                        Triple(0, ScreenTab.ALL_GOALS.title, Icons.Default.ListAlt),
                        Triple(1, ScreenTab.FAVORITE_GOALS.title, Icons.Default.Star),
                        Triple(2, ScreenTab.COMPLETED_GOALS.title, Icons.Default.EmojiEvents),
                        Triple(3, ScreenTab.ARCHIVED_GOALS.title, Icons.Default.Archive)
                    )

                    tabs.forEach { (index, title, icon) ->
                        NavigationBarItem(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            icon = { Icon(icon, contentDescription = title) },
                            label = { Text(title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_tab_${title.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Collapsible Header Section for Search and Sort Tools
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    // Header Bar with Toggle button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "Search: \"$searchQuery\"" else "Search & Sort",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${sortOrder.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { viewModel.setHeaderExpanded(!isHeaderExpanded) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("toggle_header_tools_button")
                        ) {
                            Icon(
                                imageVector = if (isHeaderExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isHeaderExpanded) "Collapse Tools" else "Expand Tools",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Collapsible Content
                    AnimatedVisibility(
                        visible = isHeaderExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 8.dp)
                        ) {
                            // Search bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search title, description, notes, memos...") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { viewModel.setSearchQuery("") },
                                            modifier = Modifier.testTag("clear_search_button")
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Clear search",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_bar_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Sort options row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sort:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(GoalSortOrder.values()) { order ->
                                        val isSelected = sortOrder == order
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.setSortOrder(order) },
                                            label = { Text(order.title, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            modifier = Modifier.testTag("sort_chip_${order.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val currentList = when (selectedTabIndex) {
                    0 -> activeGoals
                    1 -> favoriteGoals
                    2 -> completedGoals
                    3 -> archivedGoals
                    else -> activeGoals
                }

                if (currentList.isEmpty()) {
                    val emptyMessage = if (searchQuery.isNotBlank()) {
                        "No goals match \"$searchQuery\" in this section."
                    } else {
                        when (selectedTabIndex) {
                            0 -> "No goals yet.\nTap the + button to create your first goal!"
                            1 -> "No favorite goals.\nTap the star icon on any goal to pin it here."
                            2 -> "No completed goals yet.\nKeep going to reach your milestones and claim certificates!"
                            3 -> "No archived goals.\nCompleted or inactive goals can be archived from their options menu."
                            else -> "No goals available."
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (searchQuery.isNotBlank()) {
                                    Icons.Default.Search
                                } else {
                                    when (selectedTabIndex) {
                                        1 -> Icons.Default.Star
                                        2 -> Icons.Default.EmojiEvents
                                        3 -> Icons.Default.Archive
                                        else -> Icons.Default.ListAlt
                                    }
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = emptyMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            if (searchQuery.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Text("Clear Search")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 84.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(currentList, key = { it.goal.id }) { item ->
                            GoalCard(
                                goalWithProgress = item,
                                timerState = timerState,
                                onStartTimer = { viewModel.startTimer(item.goal) },
                                onPauseTimer = { viewModel.pauseTimer() },
                                onResumeTimer = { viewModel.resumeTimer() },
                                onToggleFavorite = { viewModel.toggleFavorite(item.goal) },
                                onManualAdd = { goalForManualTime = item.goal },
                                onViewStats = { goalForStats = item },
                                onViewSessions = { goalForSessions = item.goal },
                                onViewNotes = { goalForNotes = item.goal },
                                onEdit = { goalToEdit = item.goal },
                                onReset = { goalToReset = item.goal },
                                onArchive = { viewModel.archiveGoal(item.goal) },
                                onUnarchive = { viewModel.unarchiveGoal(item.goal) },
                                onMarkComplete = { viewModel.markGoalComplete(item.goal) },
                                onMarkIncomplete = { viewModel.markGoalIncomplete(item.goal) },
                                onDelete = { goalToDelete = item.goal },
                                onCreateCertificate = { goalForCertificate = item.goal }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Goal Dialog: load remembered defaults from last created goal
    if (showAddGoalDialog) {
        val defaults = remember { viewModel.getGoalCreationDefaults() }
        AddEditGoalDialog(
            initialGoal = null,
            defaults = defaults,
            onDismiss = { showAddGoalDialog = false },
            onSave = { name, desc, colorHex, targetSec, recurrence, interval, category ->
                viewModel.createGoal(name, desc, colorHex, targetSec, recurrence, interval, category)
                showAddGoalDialog = false
            }
        )
    }

    // Edit Goal Dialog
    goalToEdit?.let { goal ->
        AddEditGoalDialog(
            initialGoal = goal,
            defaults = null,
            onDismiss = { goalToEdit = null },
            onSave = { name, desc, colorHex, targetSec, recurrence, interval, category ->
                viewModel.updateGoal(
                    goal.copy(
                        name = name,
                        description = desc,
                        colorHex = colorHex,
                        targetSeconds = targetSec,
                        recurrenceType = recurrence,
                        recurrenceDaysInterval = interval,
                        category = category
                    )
                )
                goalToEdit = null
            }
        )
    }

    // Manual Time Dialog
    goalForManualTime?.let { goal ->
        ManualTimeDialog(
            goal = goal,
            onDismiss = { goalForManualTime = null },
            onSave = { seconds, note ->
                viewModel.addManualTime(goal.id, seconds, note)
                goalForManualTime = null
            }
        )
    }

    // Goal Stats Dialog
    goalForStats?.let { goalWithProgress ->
        GoalStatsDialog(
            goalWithProgress = goalWithProgress,
            logs = allLogs.filter { it.goalId == goalWithProgress.goal.id },
            onViewSessions = {
                goalForSessions = goalWithProgress.goal
                goalForStats = null
            },
            onDismiss = { goalForStats = null }
        )
    }

    // Goal Sessions Dialog
    goalForSessions?.let { goal ->
        val sessions = allLogs.filter { it.goalId == goal.id }
        GoalSessionsDialog(
            goal = goal,
            sessions = sessions,
            onUpdateSession = { updatedSession ->
                viewModel.updateTimeLog(updatedSession)
            },
            onDeleteSession = { logId, goalId ->
                viewModel.deleteTimeLog(logId, goalId)
            },
            onDismiss = { goalForSessions = null }
        )
    }

    // Goal Notes Dialog
    goalForNotes?.let { goal ->
        val notes by viewModel.getNotesForGoal(goal.id).collectAsState(initial = emptyList())
        GoalNotesDialog(
            goal = goal,
            notes = notes,
            onAddNote = { content -> viewModel.addNote(goal.id, content) },
            onUpdateNote = { note -> viewModel.updateNote(note) },
            onDeleteNote = { noteId -> viewModel.deleteNote(noteId) },
            onDismiss = { goalForNotes = null }
        )
    }

    // Certificate Dialog
    goalForCertificate?.let { goal ->
        val goalLogs = allLogs.filter { it.goalId == goal.id }
        val totalSec = goalLogs.sumOf { it.durationSeconds }
        CertificateDialog(
            goal = goal,
            totalLoggedSeconds = totalSec,
            onDismiss = { goalForCertificate = null }
        )
    }

    // Delete Confirmation Dialog
    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("Delete Goal?") },
            text = { Text("Are you sure you want to delete '${goal.name}' and all its logged history?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGoal(goal)
                        goalToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Confirmation Dialog
    goalToReset?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToReset = null },
            title = { Text("Reset Goal Progress?") },
            text = { Text("This will erase all recorded time sessions for '${goal.name}' and restart the goal tracking.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetGoal(goal.id)
                        goalToReset = null
                    }
                ) {
                    Text("Reset Progress")
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToReset = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentStartScreen = startScreenPref,
            onStartScreenChange = { screen ->
                viewModel.setStartScreen(screen)
            },
            onExportJson = { viewModel.exportJson() },
            onImportJson = { json, callback -> viewModel.importJson(json, callback) },
            onDismiss = { showSettingsDialog = false }
        )
    }
}
