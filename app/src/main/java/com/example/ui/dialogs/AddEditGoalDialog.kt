package com.example.ui.dialogs

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GoalProgressCalculator
import com.example.data.model.GoalEntity
import com.example.data.model.RecurrenceType
import com.example.ui.theme.GoalColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditGoalDialog(
    initialGoal: GoalEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        description: String,
        colorHex: String,
        targetSeconds: Long,
        recurrenceType: RecurrenceType,
        recurrenceDaysInterval: Int,
        category: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialGoal?.name ?: "") }
    var description by remember { mutableStateOf(initialGoal?.description ?: "") }
    var selectedColorHex by remember { mutableStateOf(initialGoal?.colorHex ?: "#8B5CF6") }

    // Target duration in hours and minutes
    val initialSeconds = initialGoal?.targetSeconds ?: 3600L
    var targetHoursText by remember { mutableStateOf((initialSeconds / 3600).toString()) }
    var targetMinutesText by remember { mutableStateOf(((initialSeconds % 3600) / 60).toString()) }

    var recurrenceType by remember { mutableStateOf(initialGoal?.recurrenceType ?: RecurrenceType.DAILY) }
    var customDaysInterval by remember { mutableIntStateOf(initialGoal?.recurrenceDaysInterval ?: 2) }
    var customDaysText by remember { mutableStateOf((initialGoal?.recurrenceDaysInterval ?: 2).toString()) }
    var category by remember { mutableStateOf(initialGoal?.category ?: "") }

    var nameError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (initialGoal == null) "New activity" else "Edit activity",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("dialog_close_button")) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val trimmed = name.trim()
                                if (trimmed.isEmpty()) {
                                    nameError = true
                                    return@IconButton
                                }
                                val hours = targetHoursText.toLongOrNull() ?: 0L
                                val minutes = targetMinutesText.toLongOrNull() ?: 0L
                                val totalSeconds = (hours * 3600L + minutes * 60L).coerceAtLeast(60L) // At least 1 minute

                                val interval = customDaysText.toIntOrNull()?.coerceAtLeast(1) ?: customDaysInterval

                                onSave(
                                    trimmed,
                                    description.trim(),
                                    selectedColorHex,
                                    totalSeconds,
                                    recurrenceType,
                                    interval,
                                    category.trim()
                                )
                            },
                            modifier = Modifier.testTag("dialog_save_button")
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Save",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Name Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Title,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (it.isNotBlank()) nameError = false
                        },
                        label = { Text("Name") },
                        placeholder = { Text("e.g. beginner piano, reading") },
                        isError = nameError,
                        supportingText = if (nameError) {
                            { Text("Name cannot be empty", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("goal_name_input")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Description Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        placeholder = { Text("Tap to set description / notes") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("goal_description_input")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Color Selection Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Color",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(GoalColors) { (hex, colorName) ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = selectedColorHex.equals(hex, ignoreCase = true)

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorHex = hex }
                                        .testTag("color_choice_$colorName"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = colorName,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Goal Target Section
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Time Target",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Duration inputs: Hours and Minutes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = targetHoursText,
                                onValueChange = { targetHoursText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Hours") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("target_hours_input")
                            )

                            OutlinedTextField(
                                value = targetMinutesText,
                                onValueChange = { targetMinutesText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Minutes") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("target_minutes_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick preset chips for arbitrary time targets
                        Text(
                            text = "Quick presets:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                "1m" to Pair("0", "1"),
                                "15m" to Pair("0", "15"),
                                "30m" to Pair("0", "30"),
                                "1h" to Pair("1", "0"),
                                "2h" to Pair("2", "0"),
                                "5h" to Pair("5", "0"),
                                "10h" to Pair("10", "0"),
                                "100h" to Pair("100", "0")
                            ).forEach { (label, pair) ->
                                FilterChip(
                                    selected = targetHoursText == pair.first && targetMinutesText == pair.second,
                                    onClick = {
                                        targetHoursText = pair.first
                                        targetMinutesText = pair.second
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Recurrence Selection
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recurrence",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            RecurrenceType.values().forEach { type ->
                                val isSelected = recurrenceType == type
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { recurrenceType = type },
                                    label = { Text(type.displayName) },
                                    modifier = Modifier.testTag("recurrence_chip_${type.name}")
                                )
                            }
                        }

                        // If "Every X days", show interval input
                        if (recurrenceType == RecurrenceType.CUSTOM_DAYS) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = customDaysText,
                                onValueChange = { customDaysText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Number of days (X)") },
                                placeholder = { Text("e.g. 2, 3, 5") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_days_input")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Category Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category (Optional)") },
                        placeholder = { Text("e.g. Study, Music, Fitness") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("goal_category_input")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
