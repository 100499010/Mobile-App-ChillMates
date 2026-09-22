package es.uc3m.android.chillmates.chores

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.chillmates.LargeSectionHeader
import es.uc3m.android.chillmates.PageContentWrapper
import es.uc3m.android.chillmates.R
import es.uc3m.android.chillmates.model.Chore
import es.uc3m.android.chillmates.model.ChoreAssignment
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Composable
fun ChoresPage(
    navController: NavController,
    viewModel: ChoresViewModel = viewModel()
) {
    val chores by viewModel.chores.collectAsState()
    val flatmates by viewModel.flatmates.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val context = LocalContext.current

    var showAddChoreDialog by remember { mutableStateOf(false) }
    var showReconfigureDialog by remember { mutableStateOf(false) }
    var choreToSwap by remember { mutableStateOf<Chore?>(null) }
    var reminderTargetChore by remember { mutableStateOf<Chore?>(null) }
    var showReminderDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showReminderDialog = reminderTargetChore != null
        } else {
            reminderTargetChore = null
        }
    }

    var weekOffset by remember { mutableIntStateOf(0) }

    val displayedChores = remember(chores, flatmates, weekOffset) {
        val currentWeekKey = weekKeyForOffset(0)
        val targetWeekKey = weekKeyForOffset(weekOffset)
        val mates = flatmates.filter { it != "Everyone" }
        chores.map { chore ->
            when {
                weekOffset == 0 -> {
                    val assignment = chore.assignments[targetWeekKey]
                    val assignee = assignment?.assignee ?: "Everyone"
                    ChoreWeekUi(
                        chore = chore,
                        assignee = assignee,
                        done = assignment?.done ?: false,
                        mode = ChoreWeekMode.CURRENT,
                        hasRecord = chore.assignments.containsKey(targetWeekKey)
                    )
                }
                weekOffset < 0 -> {
                    val assignment = chore.assignments[targetWeekKey]
                    ChoreWeekUi(
                        chore = chore,
                        assignee = assignment?.assignee ?: "No record",
                        done = assignment?.done ?: false,
                        mode = ChoreWeekMode.PAST,
                        hasRecord = assignment != null
                    )
                }
                else -> {
                    val latestWeekKey = chore.assignments.keys.maxOrNull()
                    val baseAssignee = latestWeekKey?.let { chore.assignments[it]?.assignee } ?: "Everyone"
                    val tentativeAssignee = if (baseAssignee == "Everyone" || mates.isEmpty()) {
                        baseAssignee
                    } else {
                        val currentIndex = mates.indexOf(baseAssignee)
                        if (currentIndex == -1) {
                            baseAssignee
                        } else {
                            val size = mates.size
                            val shiftedIndex = ((currentIndex + weekOffset) % size + size) % size
                            mates[shiftedIndex]
                        }
                    }
                    ChoreWeekUi(
                        chore = chore,
                        assignee = tentativeAssignee,
                        done = false,
                        mode = ChoreWeekMode.FUTURE,
                        hasRecord = false
                    )
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LargeSectionHeader(title = stringResource(R.string.chores_title))

        PageContentWrapper {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { weekOffset-- }) {
                                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous")
                            }

                            Text(
                                text = when {
                                    weekOffset == 0 -> "Current Week"
                                    weekOffset == 1 -> "Next Week"
                                    weekOffset == -1 -> "Last Week"
                                    weekOffset > 1 -> "In $weekOffset weeks"
                                    else -> "${-weekOffset} weeks ago"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (weekOffset == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            IconButton(onClick = { weekOffset++ }) {
                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next")
                            }
                        }
                    }

                    items(
                        items = displayedChores,
                        key = { choreUi -> choreUi.chore.id }
                    ) { choreUi ->
                        ChoreCard(
                            chore = choreUi.chore,
                            displayedAssignee = choreUi.assignee,
                            displayedDone = choreUi.done,
                            mode = choreUi.mode,
                            hasRecord = choreUi.hasRecord,
                            currentUserName = currentUserName,
                            onUpdateChore = { choreId, done ->
                                viewModel.updateChoreStatus(choreId, done)
                            },
                            onSwapRequest = { choreToSwap = it },
                            onSendReminder = { chore ->
                                reminderTargetChore = chore
                                if (
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    showReminderDialog = true
                                }
                            }
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 2.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showReconfigureDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.cd_reconfigure),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_reconfigure_chores))
                        }

                        Button(
                            onClick = { showAddChoreDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.cd_add_chore),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_add_new_chore))
                        }
                    }
                }
            }
        }
    }

    if (showAddChoreDialog) {
        AddChoreDialog(
            flatmates = flatmates,
            onDismiss = { showAddChoreDialog = false },
            onAdd = { title, assignee ->
                viewModel.addChore(title, assignee)
                showAddChoreDialog = false
            }
        )
    }

    choreToSwap?.let { chore ->
        SwapChoreDialog(
            chore = chore,
            flatmates = flatmates,
            onDismiss = { choreToSwap = null },
            onConfirmSwap = { newAssignee ->
                viewModel.swapChore(chore.id, newAssignee)
                choreToSwap = null
            }
        )
    }

    if (showReconfigureDialog) {
        ReconfigureChoresDialog(
            currentChores = chores,
            flatmates = flatmates,
            currentWeekKey = weekKeyForOffset(0),
            onDismiss = { showReconfigureDialog = false },
            onSave = { updatedList ->
                viewModel.saveReconfiguredChores(updatedList)
                showReconfigureDialog = false
            },
            onDelete = { choreId ->
                viewModel.deleteChore(choreId)
            }
        )
    }

    if (showReminderDialog) {
        ReminderTimeDialog(
            onDismiss = {
                showReminderDialog = false
                reminderTargetChore = null
            },
            onSelectDelayMinutes = { minutes ->
                reminderTargetChore?.let { chore ->
                    val currentWeekKey = weekKeyForOffset(0)
                    val assignee = chore.assignments[currentWeekKey]?.assignee ?: "Everyone"
                    val title = "Chore Reminder"
                    val message = "$assignee, don't forget to ${chore.title}!"
                    scheduleChoreReminder(
                        context = context,
                        requestCode = chore.id.hashCode(),
                        title = title,
                        message = message,
                        delayMs = TimeUnit.MINUTES.toMillis(minutes.toLong())
                    )
                }
                showReminderDialog = false
                reminderTargetChore = null
            }
        )
    }
}

@Composable
private fun ReminderTimeDialog(
    onDismiss: () -> Unit,
    onSelectDelayMinutes: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chore Reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("When should we remind them?")
                TextButton(onClick = { onSelectDelayMinutes(10) }) {
                    Text("In 10 minutes")
                }
                TextButton(onClick = { onSelectDelayMinutes(30) }) {
                    Text("In 30 minutes")
                }
                TextButton(onClick = { onSelectDelayMinutes(60) }) {
                    Text("In 1 hour")
                }
                TextButton(onClick = { onSelectDelayMinutes(180) }) {
                    Text("In 3 hours")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddChoreDialog(flatmates: List<String>, onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var assignee by remember { mutableStateOf(flatmates.firstOrNull() ?: "Me") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_add_chore_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.label_chore_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlatmateDropdown(selected = assignee, flatmates = flatmates, onSelected = { assignee = it })
            }
        },
        confirmButton = { Button(onClick = { if (title.isNotBlank()) onAdd(title, assignee) }) { Text(stringResource(R.string.btn_add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } }
    )
}

@Composable
fun SwapChoreDialog(chore: Chore, flatmates: List<String>, onDismiss: () -> Unit, onConfirmSwap: (String) -> Unit) {
    val currentWeekKey = weekKeyForOffset(0)
    val currentAssignee = chore.assignments[currentWeekKey]?.assignee ?: "Everyone"
    var selectedAssignee by remember { mutableStateOf(flatmates.firstOrNull { it != currentAssignee } ?: currentAssignee) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_swap_chore_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.dialog_swap_chore_text, chore.title))
                FlatmateDropdown(selected = selectedAssignee, flatmates = flatmates, onSelected = { selectedAssignee = it })
            }
        },
        confirmButton = { Button(onClick = { onConfirmSwap(selectedAssignee) }) { Text(stringResource(R.string.btn_swap)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } }
    )
}

@Composable
fun ReconfigureChoresDialog(
    currentChores: List<Chore>,
    flatmates: List<String>,
    currentWeekKey: String,
    onDismiss: () -> Unit,
    onSave: (List<Chore>) -> Unit,
    onDelete: (String) -> Unit
) {
    val tempChores = remember(currentChores) {
        mutableStateListOf<Chore>().apply {
            addAll(currentChores.map { chore ->
                val actualAssignee = chore.assignments[currentWeekKey]?.assignee ?: "Everyone"
                if (chore.assignments.containsKey(currentWeekKey)) {
                    chore
                } else {
                    val newAssignments = chore.assignments.toMutableMap()
                    newAssignments[currentWeekKey] = ChoreAssignment(assignee = actualAssignee, done = false)
                    chore.copy(assignments = newAssignments)
                }
            })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.btn_reconfigure_chores)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                itemsIndexed(tempChores) { index, chore ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = chore.title, fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                onDelete(chore.id)
                                tempChores.removeAt(index)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val currentAssignee = chore.assignments[currentWeekKey]?.assignee ?: "Everyone"
                        FlatmateDropdown(
                            selected = currentAssignee,
                            flatmates = flatmates,
                            onSelected = { newAssignee ->
                                val newAssignments = chore.assignments.toMutableMap()
                                val currentDone = chore.assignments[currentWeekKey]?.done ?: false
                                newAssignments[currentWeekKey] = ChoreAssignment(assignee = newAssignee, done = currentDone)
                                tempChores[index] = chore.copy(assignments = newAssignments)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(tempChores.toList()) }) { Text(stringResource(R.string.btn_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } }
    )
}

@Composable
fun FlatmateDropdown(selected: String, flatmates: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Transparent)
                .clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            flatmates.forEach { mate ->
                DropdownMenuItem(
                    text = { Text(mate) },
                    onClick = {
                        onSelected(mate)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ChoreCard(
    chore: Chore,
    displayedAssignee: String,
    displayedDone: Boolean,
    mode: ChoreWeekMode,
    hasRecord: Boolean,
    currentUserName: String,
    onUpdateChore: (String, Boolean) -> Unit,
    onSwapRequest: (Chore) -> Unit,
    onSendReminder: (Chore) -> Unit
) {
    val isAssignedToMe = displayedAssignee == currentUserName || displayedAssignee == "Me"
    val isEveryone = displayedAssignee == "Everyone"

    val canComplete = isAssignedToMe || isEveryone

    val cardColors = when (mode) {
        ChoreWeekMode.CURRENT -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        else -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    }
    val cardBorder = if (mode == ChoreWeekMode.CURRENT) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors,
        border = cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chore.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (displayedDone) TextDecoration.LineThrough else null
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.label_assigned_to, displayedAssignee),
                    style = MaterialTheme.typography.bodySmall
                )
                when (mode) {
                    ChoreWeekMode.PAST -> {
                        val statusText = if (hasRecord) {
                            if (displayedDone) stringResource(R.string.chore_status_completed)
                            else stringResource(R.string.chore_status_not_completed)
                        } else {
                            stringResource(R.string.chore_status_no_record)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> Unit
                }
            }

            if (mode == ChoreWeekMode.CURRENT) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canComplete) {
                        Checkbox(
                            checked = displayedDone,
                            onCheckedChange = { isChecked ->
                                onUpdateChore(chore.id, isChecked)
                            }
                        )
                    } else {
                        if (!displayedDone) {
                            IconButton(onClick = { onSendReminder(chore) }) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Remind",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Button(
                                onClick = { onSwapRequest(chore) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Swap")
                            }
                        } else {
                            Checkbox(
                                checked = true,
                                onCheckedChange = null,
                                enabled = false
                            )
                        }
                    }
                }
            } else {
                Checkbox(
                    checked = displayedDone,
                    onCheckedChange = null,
                    enabled = false
                )
            }
        }
    }
}

private data class ChoreWeekUi(
    val chore: Chore,
    val assignee: String,
    val done: Boolean,
    val mode: ChoreWeekMode,
    val hasRecord: Boolean
)

enum class ChoreWeekMode {
    CURRENT,
    PAST,
    FUTURE
}

private fun weekKeyForOffset(weekOffset: Int): String {
    val calendar = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        minimalDaysInFirstWeek = 4
        add(Calendar.WEEK_OF_YEAR, weekOffset)
    }
    val week = calendar.get(Calendar.WEEK_OF_YEAR)
    val year = calendar.get(Calendar.YEAR)
    return String.format("%04d-W%02d", year, week)
}

private fun scheduleChoreReminder(
    context: Context,
    requestCode: Int,
    title: String,
    message: String,
    delayMs: Long
) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val triggerAtMillis = System.currentTimeMillis() + delayMs
    val intent = Intent(context, ChoreReminderReceiver::class.java).apply {
        putExtra(ChoreReminderReceiver.EXTRA_TITLE, title)
        putExtra(ChoreReminderReceiver.EXTRA_MESSAGE, message)
        putExtra(ChoreReminderReceiver.EXTRA_NOTIFICATION_ID, requestCode)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true
    if (canExact) {
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    } else {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }
}