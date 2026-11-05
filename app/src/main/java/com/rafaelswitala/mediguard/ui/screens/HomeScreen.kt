package com.rafaelswitala.mediguard.ui.screens

/**
 * Dashboard mit fälligen Medikamenten, verpassten Einnahmen, Bedarf und Bestandswarnungen.
 */

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rafaelswitala.mediguard.R
import com.rafaelswitala.mediguard.domain.model.IntakeHistory
import com.rafaelswitala.mediguard.domain.model.IntakeStatus
import com.rafaelswitala.mediguard.domain.model.Medication
import com.rafaelswitala.mediguard.domain.model.MedicationPlanType
import com.rafaelswitala.mediguard.domain.reminder.UpcomingIntake
import com.rafaelswitala.mediguard.ui.components.MediGuardBottomBar
import com.rafaelswitala.mediguard.ui.components.MediGuardTab
import com.rafaelswitala.mediguard.ui.localization.LocalAppStrings
import com.rafaelswitala.mediguard.ui.theme.Warning
import com.rafaelswitala.mediguard.viewmodel.HistoryViewModel
import com.rafaelswitala.mediguard.viewmodel.MedicationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MedicationViewModel,
    historyViewModel: HistoryViewModel,
    onNavigateToMedications: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val medications by viewModel.activeMedications.collectAsState()
    val lowSupplyMedications by viewModel.lowSupplyMedications.collectAsState()
    val upcomingIntakes by viewModel.upcomingIntakes.collectAsState()
    val intakeHistory by historyViewModel.allIntakeHistory.collectAsState()
    val pendingGroups = intakeHistory
        .filter { it.status == IntakeStatus.PENDING || it.status == IntakeStatus.SNOOZED }
        .groupBy { it.scheduledTime }
        .toSortedMap()
        .values
        .toList()
    val missedGroups = intakeHistory
        .filter { it.status == IntakeStatus.MISSED }
        .groupBy { it.scheduledTime }
        .toSortedMap(compareByDescending { it })
        .values
        .toList()
    val prnMedications = medications.filter { it.planType == MedicationPlanType.PRN }
    var medicationToRestock by remember { mutableStateOf<Medication?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.appName) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            MediGuardBottomBar(
                selected = MediGuardTab.HOME,
                onHome = {},
                onMedications = onNavigateToMedications,
                onHistory = onNavigateToHistory,
                onProfile = onNavigateToProfile,
                onMore = onNavigateToSettings
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
            ) {
                item { HeaderCard() }

                if (missedGroups.isNotEmpty()) {
                    item {
                        Text(
                            strings.missedSection,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    items(missedGroups, key = { group -> "missed-${group.joinToString("-") { it.id.toString() }}" }) { group ->
                        PendingIntakeCard(
                            entries = group,
                            primaryLabel = strings.lateTake,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            onTaken = { historyViewModel.confirmIntakes(group.map { it.id }) },
                            onStopAlarm = historyViewModel::stopAlarm
                        )
                    }
                }

                item {
                    Text(
                        strings.dueNow,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                if (pendingGroups.isEmpty()) {
                    item { NoDueMedicationCard() }
                } else {
                    items(pendingGroups, key = { group -> "pending-${group.joinToString("-") { it.id.toString() }}" }) { group ->
                        PendingIntakeCard(
                            entries = group,
                            primaryLabel = strings.markGroupTaken,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            onTaken = { historyViewModel.confirmIntakes(group.map { it.id }) },
                            onStopAlarm = historyViewModel::stopAlarm
                        )
                    }
                }

                if (prnMedications.isNotEmpty()) {
                    item {
                        Text(
                            strings.prnSection,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    items(prnMedications, key = { "prn-${it.id}" }) { medication ->
                        PrnMedicationCard(
                            medication = medication,
                            onTakeNow = { historyViewModel.takePrnNow(medication) }
                        )
                    }
                }

                if (lowSupplyMedications.isNotEmpty()) {
                    item {
                        Text(
                            strings.lowSupplyTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    items(lowSupplyMedications, key = { "supply-${it.id}" }) { medication ->
                        LowSupplyCard(
                            medication = medication,
                            onRestock = { medicationToRestock = medication }
                        )
                    }
                }

                if (upcomingIntakes.isNotEmpty()) {
                    item {
                        Text(
                            strings.nextIntakes,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    items(upcomingIntakes, key = { "next-${it.medication.id}-${it.triggerAtMillis}" }) { intake ->
                        NextIntakeCard(intake = intake)
                    }
                }

                if (medications.isEmpty()) {
                    item { EmptyState() }
                }
            }
        }
    }

    medicationToRestock?.let { medication ->
        RestockDialog(
            medication = medication,
            onDismiss = { medicationToRestock = null },
            onConfirm = { addedAmount ->
                viewModel.addStock(medication.id, addedAmount)
                medicationToRestock = null
            }
        )
    }
}

@Composable
private fun HeaderCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.app_logo_transparent),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(88.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun EmptyState() {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(strings.noReminders, style = MaterialTheme.typography.headlineSmall)
            Text(
                strings.noRemindersBody,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun NoDueMedicationCard() {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            strings.noDueMedication,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun PendingIntakeCard(
    entries: List<IntakeHistory>,
    primaryLabel: String,
    containerColor: Color,
    onTaken: () -> Unit,
    onStopAlarm: () -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (entries.size == 1) entries.first().medicationName else "${entries.size} ${strings.medications}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            entries.forEach { entry ->
                Text(
                    "${entry.medicationName}: ${entry.dosage} ${entry.dosageUnit}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                "${strings.scheduled}: ${formatHomeDate(entries.first().scheduledTime)}",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onTaken) { Text(primaryLabel) }
                OutlinedButton(onClick = onStopAlarm) { Text(strings.stopAlarm) }
            }
        }
    }
}

@Composable
private fun PrnMedicationCard(
    medication: Medication,
    onTakeNow: () -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(medication.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${medication.dosage} ${medication.dosageUnit}", style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = onTakeNow) { Text(strings.takeNow) }
        }
    }
}

@Composable
private fun LowSupplyCard(
    medication: Medication,
    onRestock: () -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.24f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${medication.name} ${strings.lowSupplyBody}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(medication.stockLabel(strings), style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = onRestock) { Text(strings.restock) }
        }
    }
}

@Composable
private fun NextIntakeCard(intake: UpcomingIntake) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(intake.medication.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${intake.medication.dosage} ${intake.medication.dosageUnit}", style = MaterialTheme.typography.bodyMedium)
            Text("${strings.scheduled}: ${intake.formattedTime()}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatHomeDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
private fun RestockDialog(
    medication: Medication,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val strings = LocalAppStrings.current
    var stockText by remember(medication.id) { mutableStateOf("") }
    val stock = parseDecimalAmount(stockText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.restock}: ${medication.name}") },
        text = {
            OutlinedTextField(
                value = stockText,
                onValueChange = { stockText = filterDecimalInput(it) },
                label = { Text(strings.newStock) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { stock?.let(onConfirm) }, enabled = stock != null) {
                Text(strings.saveStock)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

private fun filterDecimalInput(value: String): String {
    var hasSeparator = false
    return value.filter { char ->
        when {
            char.isDigit() -> true
            (char == ',' || char == '.') && !hasSeparator -> {
                hasSeparator = true
                true
            }
            else -> false
        }
    }
}

private fun parseDecimalAmount(value: String): Double? =
    value.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }
