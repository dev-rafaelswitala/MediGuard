package com.rafaelswitala.mediguard.ui.screens

/**
 * Einnahmeverlauf mit Monatsnavigation, Filtern und nachträglicher Bearbeitung.
 */

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rafaelswitala.mediguard.domain.model.IntakeHistory
import com.rafaelswitala.mediguard.domain.model.IntakeStatus
import com.rafaelswitala.mediguard.ui.components.MediGuardBottomBar
import com.rafaelswitala.mediguard.ui.components.MediGuardTab
import com.rafaelswitala.mediguard.ui.components.StandardSectionCard
import com.rafaelswitala.mediguard.ui.localization.LocalAppStrings
import com.rafaelswitala.mediguard.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateHome: () -> Unit = {},
    onNavigateMedications: () -> Unit = {},
    onNavigateProfile: () -> Unit = {},
    onNavigateSettings: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val intakeHistory by viewModel.allIntakeHistory.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val yearMonth by viewModel.selectedYearMonth
    val filterMedicationId by viewModel.filterMedicationId
    val filterStatus by viewModel.filterStatus
    val onlyEditedLate by viewModel.onlyEditedLate
    val filtered = viewModel.filteredEntries(intakeHistory)
    val adherence = viewModel.adherenceFor(filtered)
    var editingEntry by remember { mutableStateOf<IntakeHistory?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.history) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            MediGuardBottomBar(
                selected = MediGuardTab.HISTORY,
                onHome = onNavigateHome,
                onMedications = onNavigateMedications,
                onHistory = {},
                onProfile = onNavigateProfile,
                onMore = onNavigateSettings
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::previousMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = strings.previousMonth)
                }
                Text(
                    formatYearMonth(yearMonth.year, yearMonth.month),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = viewModel::nextMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = strings.nextMonth)
                }
            }

            Text(
                "${strings.adherence}: $adherence%",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = filterMedicationId == null,
                    onClick = { viewModel.setFilterMedication(null) },
                    label = { Text("${strings.filterMedication}: ${strings.filterAll}") }
                )
                medications.forEach { med ->
                    FilterChip(
                        selected = filterMedicationId == med.id,
                        onClick = { viewModel.setFilterMedication(med.id) },
                        label = { Text(med.name) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = filterStatus == null,
                    onClick = { viewModel.setFilterStatus(null) },
                    label = { Text("${strings.filterStatus}: ${strings.filterAll}") }
                )
                IntakeStatus.entries.forEach { status ->
                    FilterChip(
                        selected = filterStatus == status,
                        onClick = { viewModel.setFilterStatus(status) },
                        label = { Text(status.label(strings)) }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = onlyEditedLate,
                    onCheckedChange = viewModel::setOnlyEditedLate
                )
                Text(strings.onlyEditedLate)
            }

            if (filtered.isEmpty()) {
                Text(
                    strings.noHistory,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { entry ->
                        StandardSectionCard(modifier = Modifier.padding(8.dp)) {
                            Text(entry.medicationName, style = MaterialTheme.typography.headlineSmall)
                            Text("${entry.dosage} ${entry.dosageUnit}", style = MaterialTheme.typography.bodyMedium)
                            Text("${strings.status}: ${entry.status.label(strings)}", style = MaterialTheme.typography.bodySmall)
                            Text("${strings.scheduled}: ${formatDate(entry.scheduledTime)}", style = MaterialTheme.typography.bodySmall)
                            entry.actualIntakeTime?.let { actual ->
                                Text("${strings.taken}: ${formatDate(actual)}", style = MaterialTheme.typography.bodySmall)
                            }
                            entry.note?.takeIf { it.isNotBlank() }?.let { note ->
                                Text("${strings.intakeNote}: $note", style = MaterialTheme.typography.bodySmall)
                            }
                            if (entry.wasEditedLate) {
                                Text(
                                    strings.editedLateBadge,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (entry.status == IntakeStatus.PENDING ||
                                    entry.status == IntakeStatus.SNOOZED ||
                                    entry.status == IntakeStatus.MISSED
                                ) {
                                    Button(onClick = { viewModel.confirmIntake(entry.id) }) {
                                        Text(strings.takenNow)
                                    }
                                }
                                OutlinedButton(onClick = { editingEntry = entry }) {
                                    Text(strings.edit)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingEntry?.let { entry ->
        EditIntakeDialog(
            entry = entry,
            onDismiss = { editingEntry = null },
            onSave = { status, time, note ->
                viewModel.editIntake(entry.id, status, time, note)
                editingEntry = null
            }
        )
    }
}

@Composable
private fun EditIntakeDialog(
    entry: IntakeHistory,
    onDismiss: () -> Unit,
    onSave: (IntakeStatus, Long?, String?) -> Unit
) {
    val strings = LocalAppStrings.current
    var status by remember { mutableStateOf(entry.status) }
    var note by remember { mutableStateOf(entry.note.orEmpty()) }
    var useNow by remember { mutableStateOf(true) }
    var timeText by remember {
        mutableStateOf(
            entry.actualIntakeTime?.let { formatDate(it) }.orEmpty()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editIntake) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entry.medicationName)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IntakeStatus.entries.forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s.label(strings)) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = useNow, onCheckedChange = { useNow = it })
                    Text(strings.useNow)
                }
                if (!useNow) {
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = { timeText = it },
                        label = { Text(strings.actualTime) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.intakeNote) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val time = when {
                    status != IntakeStatus.CONFIRMED -> null
                    useNow -> System.currentTimeMillis()
                    else -> parseFlexibleDate(timeText) ?: System.currentTimeMillis()
                }
                onSave(status, time, note.trim().ifBlank { null })
            }) { Text(strings.saveEdit) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatYearMonth(year: Int, month: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
}

private fun parseFlexibleDate(value: String): Long? =
    runCatching {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse(value.trim())?.time
    }.getOrNull()
