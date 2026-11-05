package com.rafaelswitala.mediguard.ui.screens

/**
 * Design/Sprache-Auswahl, Benachrichtigungsmodus (Stumm/Sound/Alarm), Klingelton und Tageszeiten.
 */

import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rafaelswitala.mediguard.data.settings.AppLanguage
import com.rafaelswitala.mediguard.data.settings.AppAlertMode
import com.rafaelswitala.mediguard.data.settings.AppThemeMode
import com.rafaelswitala.mediguard.data.settings.BackupFrequency
import com.rafaelswitala.mediguard.domain.model.DayPeriod
import com.rafaelswitala.mediguard.domain.model.Medication
import com.rafaelswitala.mediguard.domain.settings.DayPeriodSettings
import com.rafaelswitala.mediguard.domain.settings.TimeRangeMinutes
import com.rafaelswitala.mediguard.ui.components.ExpandableSettingsSection
import com.rafaelswitala.mediguard.ui.components.StandardSectionCard
import com.rafaelswitala.mediguard.ui.localization.LocalAppLanguage
import com.rafaelswitala.mediguard.ui.localization.LocalAppStrings
import com.rafaelswitala.mediguard.viewmodel.SettingsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Datei für die Einstellungen.
 * Hier werden Sprache, Design, Alarmart, Klingelton und Tageszeitbereiche gepflegt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateHome: () -> Unit = {},
    onNavigateMedications: () -> Unit = {},
    onNavigateHistory: () -> Unit = {},
    onNavigateProfile: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val medications by viewModel.medications.collectAsState()
    val operationMessage by viewModel.operationMessage.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteProfile by remember { mutableStateOf(false) }
    var deleteMedications by remember { mutableStateOf(false) }
    val ringtonePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult

        val pickedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        val title = pickedUri
            ?.let { uri -> RingtoneManager.getRingtone(context, uri)?.getTitle(context) }
            ?: strings.defaultSound
        viewModel.setRingtone(pickedUri?.toString(), if (pickedUri == null) null else title)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settings) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        },
        bottomBar = {
            com.rafaelswitala.mediguard.ui.components.MediGuardBottomBar(
                selected = com.rafaelswitala.mediguard.ui.components.MediGuardTab.MORE,
                onHome = onNavigateHome,
                onMedications = onNavigateMedications,
                onHistory = onNavigateHistory,
                onProfile = onNavigateProfile,
                onMore = {}
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StandardSectionCard(title = strings.theme) {
                ButtonRow(
                    items = listOf(
                        AppThemeMode.LIGHT to strings.light,
                        AppThemeMode.DARK to strings.dark,
                        AppThemeMode.SYSTEM to strings.system
                    ),
                    selected = settings.themeMode,
                    onSelected = viewModel::setThemeMode
                )
            }

            StandardSectionCard(title = strings.language) {
                ButtonRow(
                    items = listOf(
                        AppLanguage.EN to strings.english,
                        AppLanguage.DE to strings.german
                    ),
                    selected = settings.language,
                    onSelected = viewModel::setLanguage
                )
            }

            StandardSectionCard(title = strings.alertMode) {
                ButtonRow(
                    items = listOf(
                        AppAlertMode.SILENT_NOTIFICATION to strings.alertModeSilentNotification,
                        AppAlertMode.SOUND_NOTIFICATION to strings.alertModeSoundNotification,
                        AppAlertMode.ALARM to strings.alertModeAlarm,
                    ),
                    selected = settings.alertMode,
                    onSelected = viewModel::setAlertMode
                )
            }

            StandardSectionCard(title = strings.alarmSound) {
                Text(
                    "${strings.selectedSound}: ${settings.ringtoneTitle ?: strings.defaultSound}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = {
                        ringtonePicker.launch(
                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_TYPE,
                                    RingtoneManager.TYPE_ALARM or
                                        RingtoneManager.TYPE_NOTIFICATION or
                                        RingtoneManager.TYPE_RINGTONE
                                )
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                settings.ringtoneUri?.let { uri ->
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(uri))
                                }
                            }
                        )
                    }
                ) {
                    Text(strings.chooseSound)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                StandardSectionCard(title = strings.exactAlarmPermission) {
                    Text(strings.exactAlarmPermissionBody, style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                )
                            }
                        },
                        enabled = !alarmManager.canScheduleExactAlarms()
                    ) {
                        Text(strings.allowExactAlarms)
                    }
                }
            }

            DayPeriodSettingsSection(
                settings = settings.dayPeriodSettings,
                strings = strings,
                language = LocalAppLanguage.current,
                onSave = viewModel::setDayPeriodSettings
            )

            ExpandableSettingsSection(title = strings.deleteDataSection) {
                CheckRow(
                    label = strings.allData,
                    checked = deleteProfile && deleteMedications,
                    onCheckedChange = { selected ->
                        deleteProfile = selected
                        deleteMedications = selected
                    }
                )
                CheckRow(
                    label = strings.profileData,
                    checked = deleteProfile,
                    onCheckedChange = { deleteProfile = it }
                )
                CheckRow(
                    label = strings.medicationData,
                    checked = deleteMedications,
                    onCheckedChange = { deleteMedications = it }
                )
                Button(
                    onClick = { showDeleteDialog = true },
                    enabled = deleteProfile || deleteMedications,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(strings.deleteSelectedData)
                }
            }

            BackupSettingsSection(
                settingsEnabled = settings.backupEnabled,
                frequency = settings.backupFrequency,
                medications = medications,
                strings = strings,
                onBackupChanged = viewModel::setBackup,
                onExport = viewModel::exportPdf
            )

            operationMessage?.let { message ->
                val display = when (message) {
                    "pdf_created" -> strings.pdfCreated
                    "data_deleted" -> strings.dataDeleted
                    else -> message
                }
                Text(display, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(strings.deleteDataSection) },
            text = { Text(strings.deleteDataConfirm) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteData(deleteProfile, deleteMedications) {
                            deleteProfile = false
                            deleteMedications = false
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun BackupSettingsSection(
    settingsEnabled: Boolean,
    frequency: BackupFrequency,
    medications: List<Medication>,
    strings: com.rafaelswitala.mediguard.ui.localization.AppStrings,
    onBackupChanged: (Boolean, BackupFrequency) -> Unit,
    onExport: (LocalDate, LocalDate, Set<Long>?, (Boolean) -> Unit) -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/uuuu") }
    val today = remember { LocalDate.now() }
    var fromText by remember { mutableStateOf(today.withDayOfMonth(1).format(formatter)) }
    var toText by remember { mutableStateOf(today.format(formatter)) }
    var allMedications by remember { mutableStateOf(true) }
    var selectedMedicationIds by remember { mutableStateOf(emptySet<Long>()) }
    var dateError by remember { mutableStateOf(false) }

    ExpandableSettingsSection(title = strings.backupPdf) {
        Text(strings.regularBackupQuestion, fontWeight = FontWeight.SemiBold)
        ButtonRow(
            items = listOf(true to strings.yes, false to strings.no),
            selected = settingsEnabled,
            onSelected = { enabled ->
                onBackupChanged(
                    enabled,
                    if (enabled && frequency == BackupFrequency.NONE) BackupFrequency.WEEKLY else frequency
                )
            }
        )
        if (settingsEnabled) {
            Text(strings.backupFrequency, fontWeight = FontWeight.SemiBold)
            ButtonRow(
                items = listOf(
                    BackupFrequency.WEEKLY to strings.weeklySunday,
                    BackupFrequency.MONTHLY to strings.monthlyLastDay,
                    BackupFrequency.QUARTERLY to strings.quarterlyEnd,
                    BackupFrequency.SEMIANNUAL to strings.semiannualEnd
                ),
                selected = frequency,
                onSelected = { onBackupChanged(true, it) }
            )
        }

        Text(strings.manualPdfExport, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = fromText,
            onValueChange = { fromText = it.filter { char -> char.isDigit() || char == '/' }.take(10) },
            label = { Text(strings.fromDate) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = toText,
            onValueChange = { toText = it.filter { char -> char.isDigit() || char == '/' }.take(10) },
            label = { Text(strings.toDate) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        CheckRow(
            label = strings.allMedications,
            checked = allMedications,
            onCheckedChange = { allMedications = it }
        )
        if (!allMedications) {
            Text(strings.selectMedications, style = MaterialTheme.typography.bodySmall)
            medications.forEach { medication ->
                CheckRow(
                    label = medication.name,
                    checked = medication.id in selectedMedicationIds,
                    onCheckedChange = { selected ->
                        selectedMedicationIds = if (selected) {
                            selectedMedicationIds + medication.id
                        } else {
                            selectedMedicationIds - medication.id
                        }
                    }
                )
            }
        }
        if (dateError) {
            Text(strings.invalidDateRange, color = MaterialTheme.colorScheme.error)
        }
        Button(
            enabled = allMedications || selectedMedicationIds.isNotEmpty(),
            onClick = {
                val from = tryParseDate(fromText, formatter)
                val to = tryParseDate(toText, formatter)
                if (from == null || to == null || to.isBefore(from)) {
                    dateError = true
                } else {
                    dateError = false
                    onExport(
                        from,
                        to,
                        if (allMedications) null else selectedMedicationIds
                    ) { success -> dateError = !success }
                }
            }
        ) {
            Text(strings.generatePdf)
        }
    }
}

@Composable
private fun CheckRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

private fun tryParseDate(value: String, formatter: DateTimeFormatter): LocalDate? =
    try {
        LocalDate.parse(value, formatter)
    } catch (_: DateTimeParseException) {
        null
    }

@Composable
private fun DayPeriodSettingsSection(
    settings: DayPeriodSettings,
    strings: com.rafaelswitala.mediguard.ui.localization.AppStrings,
    language: AppLanguage,
    onSave: (DayPeriodSettings, (Boolean) -> Unit) -> Unit
) {
    var draftTexts by remember(settings) {
        mutableStateOf(
            DayPeriod.entries.associateWith { period ->
                val range = settings.rangeFor(period)
                TimeRangeTextDraft(
                    start = minutesToTime(range.startMinutes),
                    end = minutesToTime(range.endMinutes)
                )
            }
        )
    }
    var gapMessage by remember { mutableStateOf<String?>(null) }

    ExpandableSettingsSection(title = strings.dayPeriodSettings) {
        Text(strings.dayPeriodSettingsHint, style = MaterialTheme.typography.bodySmall)
        DayPeriod.entries.forEach { period ->
            val range = draftTexts.getValue(period)
            Text(period.label(language), fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = range.start,
                    onValueChange = { value ->
                        draftTexts = draftTexts + (period to range.copy(start = filterTimeInput(value)))
                    },
                    label = { Text(strings.from) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = range.end,
                    onValueChange = { value ->
                        draftTexts = draftTexts + (period to range.copy(end = filterTimeInput(value)))
                    },
                    label = { Text(strings.to) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
        gapMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            val draft = draftTexts.toDayPeriodSettingsOrNull()
            if (draft == null) {
                gapMessage = strings.invalidTime
                return@Button
            }
            val validation = draft.validate()
            if (!validation.isValid) {
                gapMessage = "${strings.dayPeriodGapError} ${validation.gaps.joinToString(", ")}"
            } else {
                gapMessage = null
                onSave(draft) { ok ->
                    if (!ok) gapMessage = strings.dayPeriodGapError
                }
            }
        }) {
            Text(strings.save)
        }
    }
}

private data class TimeRangeTextDraft(
    val start: String,
    val end: String
)

private fun Map<DayPeriod, TimeRangeTextDraft>.toDayPeriodSettingsOrNull(): DayPeriodSettings? {
    fun parsedRange(period: DayPeriod): TimeRangeMinutes? {
        val draft = this[period] ?: return null
        val start = timeToMinutes(draft.start) ?: return null
        val end = timeToMinutes(draft.end) ?: return null
        return TimeRangeMinutes(start, end)
    }

    return DayPeriodSettings(
        morning = parsedRange(DayPeriod.MORNING) ?: return null,
        noon = parsedRange(DayPeriod.NOON) ?: return null,
        afternoon = parsedRange(DayPeriod.AFTERNOON) ?: return null,
        evening = parsedRange(DayPeriod.EVENING) ?: return null,
        night = parsedRange(DayPeriod.NIGHT) ?: return null
    )
}

private fun DayPeriodSettings.copyPeriod(period: DayPeriod, range: TimeRangeMinutes): DayPeriodSettings =
    when (period) {
        DayPeriod.MORNING -> copy(morning = range)
        DayPeriod.NOON -> copy(noon = range)
        DayPeriod.AFTERNOON -> copy(afternoon = range)
        DayPeriod.EVENING -> copy(evening = range)
        DayPeriod.NIGHT -> copy(night = range)
    }

private fun minutesToTime(minutes: Int): String {
    val h = (minutes / 60) % 24
    val m = minutes % 60
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
}

private fun filterTimeInput(value: String): String =
    value.filter { it.isDigit() || it == ':' }.take(5)

private fun timeToMinutes(value: String): Int? {
    val parts = value.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    return if (h in 0..23 && m in 0..59) h * 60 + m else null
}

@Composable
private fun <T> ButtonRow(
    items: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (value, label) ->
            if (value == selected) {
                Button(onClick = { onSelected(value) }) {
                    Text(label)
                }
            } else {
                OutlinedButton(onClick = { onSelected(value) }) {
                    Text(label)
                }
            }
        }
    }
}
