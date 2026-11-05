package com.rafaelswitala.mediguard.ui.screens

/**
 * Medikamentenliste mit Auswahlmodus, Massenauswahl, Löschen und Gruppierungsvorschlägen.
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rafaelswitala.mediguard.domain.model.DayPeriod
import com.rafaelswitala.mediguard.domain.model.FrequencyType
import com.rafaelswitala.mediguard.domain.model.Medication
import com.rafaelswitala.mediguard.domain.model.MedicationPlanType
import com.rafaelswitala.mediguard.domain.model.MedicationSchedule
import com.rafaelswitala.mediguard.domain.model.ScheduleDataCodec
import com.rafaelswitala.mediguard.domain.model.formatMedicationAmount
import com.rafaelswitala.mediguard.ui.components.MedicationCard
import com.rafaelswitala.mediguard.ui.localization.AppStrings
import com.rafaelswitala.mediguard.ui.localization.LocalAppLanguage
import com.rafaelswitala.mediguard.ui.localization.LocalAppStrings
import com.rafaelswitala.mediguard.viewmodel.MedicationViewModel

/**
 * Datei für die Medikamentenübersicht.
 * Enthält Liste, Detaildialog, Mehrfachauswahl und Gruppierungsabfrage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsScreen(
    viewModel: MedicationViewModel,
    onNavigateToAdd: () -> Unit = {},
    onNavigateHome: () -> Unit = {},
    onNavigateHistory: () -> Unit = {},
    onNavigateProfile: () -> Unit = {},
    onNavigateSettings: () -> Unit = {},
    onNavigateToEdit: (Long) -> Unit = {},
    onNavigateToPlan: (Long) -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    val medications by viewModel.activeMedications.collectAsState()
    val allSchedules by viewModel.allActiveSchedules.collectAsState()
    var showDeleteSelectionDialog by remember { mutableStateOf(false) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var medicationToViewId by remember { mutableStateOf<Long?>(null) }
    val selectedMedicationIds = remember { mutableStateListOf<Long>() }
    val selectionMode = selectedMedicationIds.isNotEmpty()
    val selectedMedications = medications.filter { it.id in selectedMedicationIds }
    val schedulesByMedication = remember(allSchedules) { allSchedules.groupBy { it.medicationId } }
    val firstScheduleGroupByMed = remember(allSchedules) {
        allSchedules
            .groupBy { it.medicationId }
            .mapValues { (_, schedules) -> schedules.mapNotNull { it.intakeGroupId }.firstOrNull() }
    }

    val viewingMedication = medications.firstOrNull { it.id == medicationToViewId }
    val viewingSchedules = allSchedules.filter { it.medicationId == medicationToViewId }
    val selectedSchedules = selectedMedicationIds.associateWith { schedulesByMedication[it].orEmpty() }
    val selectedGroupIds = selectedSchedules.values.flatten().mapNotNull { it.intakeGroupId }.distinct()
    val allSelectedPlanned = selectedSchedules.isNotEmpty() && selectedSchedules.values.all { it.isNotEmpty() }
    val allSelectedUngrouped = selectedSchedules.values.flatten().all { it.intakeGroupId == null }
    val sameGroupSelection = selectedMedicationIds.isNotEmpty() &&
        selectedGroupIds.size == 1 &&
        selectedSchedules.values.all { schedules ->
            schedules.isNotEmpty() && schedules.all { it.intakeGroupId == selectedGroupIds.first() }
        }
    val canCreateGroup = selectedMedicationIds.size >= 2 && allSelectedPlanned && allSelectedUngrouped
    val selectedGroupedMedicationCount = selectedSchedules.values.count { schedules ->
        schedules.any { it.intakeGroupId != null }
    }
    val canJoinGroup = selectedMedicationIds.size >= 2 && allSelectedPlanned &&
        selectedGroupIds.size == 1 && selectedGroupedMedicationCount == 1 &&
        selectedSchedules.values.any { schedules -> schedules.all { it.intakeGroupId == null } }

    LaunchedEffect(medications) {
        val currentIds = medications.map { it.id }.toSet()
        selectedMedicationIds.removeAll { it !in currentIds }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectionMode) {
                            "${selectedMedicationIds.size} ${strings.selectedCount}"
                        } else {
                            strings.medications
                        }
                    )
                },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = { selectedMedicationIds.clear() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.cancel
                            )
                        }
                    }
                },
                actions = {
                    if (selectionMode) {
                        IconButton(
                            onClick = {
                                val id = selectedMedicationIds.single()
                                viewModel.loadMedicationForEdit(id)
                                onNavigateToEdit(id)
                            },
                            enabled = selectedMedicationIds.size == 1
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = strings.edit)
                        }
                        IconButton(onClick = { showDeleteSelectionDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = strings.deleteSelected
                            )
                        }
                        if (sameGroupSelection) {
                            IconButton(onClick = {
                                viewModel.ungroupAndRemoveSchedules(selectedMedicationIds.toList())
                                selectedMedicationIds.clear()
                            }) {
                                Icon(Icons.Default.LinkOff, contentDescription = strings.ungroup)
                            }
                        } else if (selectedMedicationIds.size >= 2) {
                            IconButton(
                                enabled = canCreateGroup || canJoinGroup,
                                onClick = {
                                    if (canJoinGroup) {
                                        val groupId = selectedGroupIds.single()
                                        val leader = allSchedules.first { it.intakeGroupId == groupId }
                                        val ungrouped = selectedMedicationIds.filter { id ->
                                            selectedSchedules[id].orEmpty().all { it.intakeGroupId == null }
                                        }
                                        viewModel.addMedicationsToGroup(ungrouped, groupId, leader)
                                        selectedMedicationIds.clear()
                                    } else {
                                        showGroupDialog = true
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Link, contentDescription = strings.remindTogether)
                            }
                        }
                    } else {
                        IconButton(onClick = onNavigateToAdd) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = strings.addMedication
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            com.rafaelswitala.mediguard.ui.components.MediGuardBottomBar(
                selected = com.rafaelswitala.mediguard.ui.components.MediGuardTab.MEDICATIONS,
                onHome = onNavigateHome,
                onMedications = {},
                onHistory = onNavigateHistory,
                onProfile = onNavigateProfile,
                onMore = onNavigateSettings
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (medications.isEmpty()) {
                EmptyMedicationsState(onAdd = onNavigateToAdd)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
                ) {
                    items(medications, key = { it.id }) { medication ->
                        MedicationCard(
                            medication = medication,
                            scheduleGroupId = firstScheduleGroupByMed[medication.id],
                            scheduleSummary = schedulesByMedication[medication.id]
                                ?.takeIf { it.isNotEmpty() }
                                ?.let { formatSchedulesSummary(it, strings, language) },
                            isPrn = medication.planType == MedicationPlanType.PRN,
                            onPlanClick = onNavigateToPlan,
                            onView = { medicationToViewId = medication.id },
                            onLongPress = { id ->
                                if (id !in selectedMedicationIds) {
                                    selectedMedicationIds.add(id)
                                }
                            },
                            selectionMode = selectionMode,
                            selected = medication.id in selectedMedicationIds,
                            onSelectionChange = { checked ->
                                if (checked && medication.id !in selectedMedicationIds) {
                                    selectedMedicationIds.add(medication.id)
                                } else if (!checked) {
                                    selectedMedicationIds.remove(medication.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteSelectionDialog) {
        DeleteSelectedMedicationsDialog(
            medications = selectedMedications,
            onDismiss = { showDeleteSelectionDialog = false },
            onConfirm = {
                viewModel.deleteMedications(selectedMedicationIds.toList())
                selectedMedicationIds.clear()
                showDeleteSelectionDialog = false
            }
        )
    }

    if (medicationToViewId != null && viewingMedication?.id == medicationToViewId) {
        MedicationDetailsDialog(
            medication = viewingMedication,
            schedules = viewingSchedules,
            onDismiss = { medicationToViewId = null }
        )
    }

    if (showGroupDialog) {
        GroupScheduleDialog(
            onDismiss = { showGroupDialog = false },
            onConfirm = { template ->
                viewModel.createGroup(selectedMedicationIds.toList(), template)
                selectedMedicationIds.clear()
                showGroupDialog = false
            }
        )
    }
}

@Composable
private fun EmptyMedicationsState(onAdd: () -> Unit) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Button(onClick = onAdd) {
            Text(strings.addMedication)
        }
    }
}

@Composable
private fun GroupScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (MedicationSchedule) -> Unit
) {
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    var type by remember { mutableStateOf(FrequencyType.EXACT_TIME) }
    var time by remember { mutableStateOf("08:00") }
    var period by remember { mutableStateOf(DayPeriod.MORNING) }
    val parsedTime = parseGroupTime(time)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.remindTogether) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        FrequencyType.EXACT_TIME to strings.daily,
                        FrequencyType.DAY_PERIOD to strings.dayPeriod
                    ).forEach { (value, label) ->
                        if (type == value) Button(onClick = { type = value }) { Text(label) }
                        else androidx.compose.material3.OutlinedButton(onClick = { type = value }) { Text(label) }
                    }
                }
                if (type == FrequencyType.EXACT_TIME) {
                    androidx.compose.material3.OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text(strings.pickUnifiedTime) }
                    )
                } else {
                    DayPeriod.entries.forEach { value ->
                        androidx.compose.material3.FilterChip(
                            selected = period == value,
                            onClick = { period = value },
                            label = { Text(value.label(language)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = type == FrequencyType.DAY_PERIOD || parsedTime != null,
                onClick = {
                    val data = if (type == FrequencyType.DAY_PERIOD) {
                        ScheduleDataCodec.dayPeriod(period)
                    } else {
                        ScheduleDataCodec.exactTime(parsedTime!!.first, parsedTime.second)
                    }
                    onConfirm(MedicationSchedule(medicationId = 0, scheduleType = type, scheduleData = data))
                }
            ) {
                Text(strings.groupTogetherConfirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.groupTogetherDecline)
            }
        }
    )
}

@Composable
private fun DeleteMedicationDialog(
    medication: Medication,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.confirmDeleteTitle) },
        text = { Text("${strings.confirmDeleteBody}\n\n${medication.name}") },
        confirmButton = {
            Button(onClick = onConfirm) { Text(strings.delete) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
private fun DeleteSelectedMedicationsDialog(
    medications: List<Medication>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.confirmDeleteSelectedTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(strings.confirmDeleteSelectedBody)
                medications.forEach { medication ->
                    Text("- ${medication.name}")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = medications.isNotEmpty()
            ) {
                Text(strings.delete)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
private fun MedicationDetailsDialog(
    medication: Medication?,
    schedules: List<MedicationSchedule>,
    onDismiss: () -> Unit
) {
    if (medication == null) return
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    "${strings.details}: ${medication.name}",
                    modifier = Modifier.weight(1f)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailLine(strings.dosage, "${medication.dosage} ${medication.dosageUnit}")
                if (medication.doseQuantity > 1.0) {
                    DetailLine(strings.doseQuantity, formatMedicationAmount(medication.doseQuantity))
                }
                if (medication.description.isNotBlank()) {
                    DetailLine(strings.notes, medication.description)
                }
                medication.durationDays?.let { days ->
                    DetailLine(strings.durationDays, days.toString())
                }
                medication.treatmentLimitDoses?.let { limit ->
                    DetailLine(strings.treatmentLimitDoses, limit.toString())
                }
                if (medication.remainingDoses != null || medication.remainingVolumeMl != null) {
                    DetailLine(strings.stock, medication.stockLabel(strings))
                }
                Text(
                    strings.schedules,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (schedules.isEmpty()) {
                    Text(strings.noSchedules, style = MaterialTheme.typography.bodyMedium)
                } else {
                    schedules.forEach { schedule ->
                        Text(
                            formatSchedule(schedule, strings, language),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(strings.close) }
        }
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatSchedule(
    schedule: MedicationSchedule,
    strings: AppStrings,
    language: com.rafaelswitala.mediguard.data.settings.AppLanguage
): String =
    when (schedule.scheduleType) {
        FrequencyType.EXACT_TIME -> {
            val hour = ScheduleDataCodec.readInt(schedule.scheduleData, "hour", 8)
            val minute = ScheduleDataCodec.readInt(schedule.scheduleData, "minute", 0)
            "${strings.exactTime}: ${formatScheduleTime(hour, minute)}"
        }
        FrequencyType.TIME_RANGE -> {
            val startHour = ScheduleDataCodec.readInt(schedule.scheduleData, "startHour", 8)
            val startMinute = ScheduleDataCodec.readInt(schedule.scheduleData, "startMinute", 0)
            val endHour = ScheduleDataCodec.readInt(schedule.scheduleData, "endHour", 10)
            val endMinute = ScheduleDataCodec.readInt(schedule.scheduleData, "endMinute", 0)
            "${strings.timeRange}: ${formatScheduleTime(startHour, startMinute)} - ${formatScheduleTime(endHour, endMinute)}"
        }
        FrequencyType.DAY_PERIOD -> "${strings.dayPeriod}: ${
            ScheduleDataCodec.readPeriod(schedule.scheduleData).label(language)
        }"
        FrequencyType.INTERVAL -> {
            val hours = ScheduleDataCodec.readInt(schedule.scheduleData, "intervalHours", 6)
            val startHour = ScheduleDataCodec.readInt(schedule.scheduleData, "startHour", 8)
            val startMinute = ScheduleDataCodec.readInt(schedule.scheduleData, "startMinute", 0)
            "${strings.interval}: $hours h, ${strings.startTime} ${formatScheduleTime(startHour, startMinute)}"
        }
        FrequencyType.WEEKLY -> {
            val hour = ScheduleDataCodec.readInt(schedule.scheduleData, "hour", 8)
            val minute = ScheduleDataCodec.readInt(schedule.scheduleData, "minute", 0)
            "${strings.weekly}: ${
                ScheduleDataCodec.readDays(schedule.scheduleData).joinToString(", ") { it.label(language) }
            } ${formatScheduleTime(hour, minute)}"
        }
        FrequencyType.ONE_SHOT -> {
            val trigger = ScheduleDataCodec.readLong(schedule.scheduleData, "triggerAtMillis", 0L)
            val formatted = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(trigger))
            "${strings.planOneShot}: $formatted"
        }
    }

private fun formatSchedulesSummary(
    schedules: List<MedicationSchedule>,
    strings: AppStrings,
    language: com.rafaelswitala.mediguard.data.settings.AppLanguage
): String {
    val groups = schedules.groupBy { it.scheduleType }
    val lines = groups.map { (type, allItems) ->
        val items = allItems.take(4)
        when (type) {
            FrequencyType.EXACT_TIME -> "${strings.daily}: " + items.joinToString(", ") {
                formatScheduleTime(
                    ScheduleDataCodec.readInt(it.scheduleData, "hour", 8),
                    ScheduleDataCodec.readInt(it.scheduleData, "minute", 0)
                )
            }
            FrequencyType.WEEKLY -> "${strings.weekly}: " + items.flatMap { schedule ->
                val time = formatScheduleTime(
                    ScheduleDataCodec.readInt(schedule.scheduleData, "hour", 8),
                    ScheduleDataCodec.readInt(schedule.scheduleData, "minute", 0)
                )
                ScheduleDataCodec.readDays(schedule.scheduleData).map { "${it.label(language).take(2)} $time" }
            }.joinToString(", ")
            FrequencyType.DAY_PERIOD -> "${strings.dayPeriod}: " + items.joinToString(", ") {
                ScheduleDataCodec.readPeriod(it.scheduleData).label(language)
            }
            FrequencyType.INTERVAL, FrequencyType.TIME_RANGE, FrequencyType.ONE_SHOT ->
                items.joinToString(", ") { formatSchedule(it, strings, language) }
        }
    }
    val shown = lines.take(3)
    val shownTypes = groups.keys.take(3)
    val shownCount = shownTypes.sumOf { groups.getValue(it).take(4).size }
    val remaining = schedules.size - shownCount
    return buildList {
        addAll(shown)
        if (remaining > 0) add("+$remaining ${strings.moreSchedules}")
    }.joinToString("\n")
}

private fun formatScheduleTime(hour: Int, minute: Int): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

private fun parseGroupTime(value: String): Pair<Int, Int>? {
    val parts = value.trim().split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull()
    val minute = parts[1].toIntOrNull()
    return if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
        hour to minute
    } else {
        null
    }
}
