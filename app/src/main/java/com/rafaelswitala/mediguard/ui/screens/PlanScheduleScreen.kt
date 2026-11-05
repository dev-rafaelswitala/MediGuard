package com.rafaelswitala.mediguard.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rafaelswitala.mediguard.domain.model.DayOfWeekEnum
import com.rafaelswitala.mediguard.domain.model.DayPeriod
import com.rafaelswitala.mediguard.domain.model.FrequencyType
import com.rafaelswitala.mediguard.domain.model.MedicationPlanType
import com.rafaelswitala.mediguard.domain.model.MedicationSchedule
import com.rafaelswitala.mediguard.domain.model.ScheduleDataCodec
import com.rafaelswitala.mediguard.ui.localization.LocalAppLanguage
import com.rafaelswitala.mediguard.ui.localization.LocalAppStrings
import com.rafaelswitala.mediguard.viewmodel.MedicationViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScheduleScreen(
    viewModel: MedicationViewModel,
    medicationId: Long,
    onNavigateBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    val medication by viewModel.editingMedication
    val existingSchedules by viewModel.editingSchedules
    var selectedType by remember { mutableStateOf(FrequencyType.EXACT_TIME) }
    val exactTimes = remember { mutableStateListOf("08:00") }
    val selectedPeriods = remember { mutableStateListOf(DayPeriod.MORNING) }
    val selectedDays = remember { mutableStateListOf(DayOfWeekEnum.MONDAY) }
    var weeklyTimes by remember {
        mutableStateOf(DayOfWeekEnum.entries.associateWith { "08:00" })
    }
    var intervalHours by remember { mutableStateOf("6") }
    var intervalStart by remember { mutableStateOf("08:00") }
    var oneShotDate by remember { mutableStateOf("") }
    var oneShotTime by remember { mutableStateOf("08:00") }
    var initialized by remember(medicationId) { mutableStateOf(false) }

    LaunchedEffect(medicationId) { viewModel.loadMedicationForEdit(medicationId) }
    LaunchedEffect(medication, existingSchedules) {
        if (initialized || medication?.id != medicationId) return@LaunchedEffect
        if (medication?.planType == MedicationPlanType.ONE_SHOT) {
            selectedType = FrequencyType.ONE_SHOT
        }
        existingSchedules.firstOrNull()?.let { first ->
            selectedType = first.scheduleType
            when (first.scheduleType) {
                FrequencyType.EXACT_TIME -> {
                    exactTimes.clear()
                    exactTimes.addAll(existingSchedules.filter { it.scheduleType == FrequencyType.EXACT_TIME }.map {
                        plannerTime(
                            ScheduleDataCodec.readInt(it.scheduleData, "hour", 8),
                            ScheduleDataCodec.readInt(it.scheduleData, "minute", 0)
                        )
                    })
                }
                FrequencyType.DAY_PERIOD -> {
                    selectedPeriods.clear()
                    selectedPeriods.addAll(existingSchedules.map { ScheduleDataCodec.readPeriod(it.scheduleData) })
                }
                FrequencyType.INTERVAL -> {
                    intervalHours = ScheduleDataCodec.readInt(first.scheduleData, "intervalHours", 6).toString()
                    intervalStart = plannerTime(
                        ScheduleDataCodec.readInt(first.scheduleData, "startHour", 8),
                        ScheduleDataCodec.readInt(first.scheduleData, "startMinute", 0)
                    )
                }
                FrequencyType.WEEKLY -> {
                    selectedDays.clear()
                    val weekly = existingSchedules.filter { it.scheduleType == FrequencyType.WEEKLY }
                    val times = weeklyTimes.toMutableMap()
                    weekly.forEach { schedule ->
                        val time = plannerTime(
                            ScheduleDataCodec.readInt(schedule.scheduleData, "hour", 8),
                            ScheduleDataCodec.readInt(schedule.scheduleData, "minute", 0)
                        )
                        ScheduleDataCodec.readDays(schedule.scheduleData).forEach { day ->
                            if (day !in selectedDays) selectedDays.add(day)
                            times[day] = time
                        }
                    }
                    weeklyTimes = times
                }
                FrequencyType.ONE_SHOT -> {
                    val trigger = ScheduleDataCodec.readLong(first.scheduleData, "triggerAtMillis", 0L)
                    if (trigger > 0) {
                        oneShotDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(trigger)
                        oneShotTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(trigger)
                    }
                    Unit
                }
                FrequencyType.TIME_RANGE -> selectedType = FrequencyType.EXACT_TIME
            }
        }
        initialized = true
    }

    val drafts = plannerDrafts(
        selectedType, exactTimes, selectedPeriods, selectedDays, intervalHours,
        intervalStart, weeklyTimes, oneShotDate, oneShotTime
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.planSchedule) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.cancel)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Button(
                onClick = { viewModel.replaceSchedules(medicationId, drafts, onNavigateBack) },
                enabled = drafts.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)
            ) { Text(strings.save) }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (medication?.planType != MedicationPlanType.ONE_SHOT) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        FrequencyType.EXACT_TIME to strings.exactTime,
                        FrequencyType.DAY_PERIOD to strings.dayPeriod,
                        FrequencyType.INTERVAL to strings.interval,
                        FrequencyType.WEEKLY to strings.weekly
                    ).forEach { (type, label) ->
                        FilterChip(selected = selectedType == type, onClick = { selectedType = type }, label = { Text(label) })
                    }
                }
            }

            when (selectedType) {
                FrequencyType.EXACT_TIME -> {
                    exactTimes.forEachIndexed { index, value ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = value,
                                onValueChange = { exactTimes[index] = it },
                                label = { Text(strings.oneShotTime) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            if (exactTimes.size > 1) {
                                IconButton(onClick = { exactTimes.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = strings.remove)
                                }
                            }
                        }
                    }
                    Button(onClick = { exactTimes.add("08:00") }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(strings.addTime, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                FrequencyType.DAY_PERIOD -> {
                    Text(strings.everySelectedPeriodIntake, style = MaterialTheme.typography.bodySmall)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayPeriod.entries.forEach { period ->
                            FilterChip(
                                selected = period in selectedPeriods,
                                onClick = {
                                    if (period in selectedPeriods) selectedPeriods.remove(period) else selectedPeriods.add(period)
                                },
                                label = { Text(period.label(language)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                FrequencyType.INTERVAL -> {
                    OutlinedTextField(
                        value = intervalHours,
                        onValueChange = { intervalHours = it.filter(Char::isDigit) },
                        label = { Text(strings.intervalHours) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = intervalStart,
                        onValueChange = { intervalStart = it },
                        label = { Text(strings.startTime) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                FrequencyType.WEEKLY -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DayOfWeekEnum.entries.forEach { day ->
                            val checked = day in selectedDays
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        if (it && day !in selectedDays) selectedDays.add(day)
                                        else if (!it) selectedDays.remove(day)
                                    }
                                )
                                Text(day.label(language), modifier = Modifier.weight(1f))
                                OutlinedTextField(
                                    value = weeklyTimes.getValue(day),
                                    onValueChange = { weeklyTimes = weeklyTimes + (day to it) },
                                    label = { Text(strings.oneShotTime) },
                                    enabled = checked,
                                    singleLine = true,
                                    modifier = Modifier.weight(0.75f)
                                )
                            }
                        }
                    }
                }
                FrequencyType.ONE_SHOT -> {
                    OutlinedTextField(
                        value = oneShotDate,
                        onValueChange = { oneShotDate = it },
                        label = { Text("${strings.oneShotDate} (dd/MM/yyyy)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = oneShotTime,
                        onValueChange = { oneShotTime = it },
                        label = { Text(strings.oneShotTime) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                FrequencyType.TIME_RANGE -> Unit
            }
            if (drafts.isEmpty()) {
                Text(strings.scheduleRequired, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun plannerDrafts(
    type: FrequencyType,
    exactTimes: List<String>,
    periods: List<DayPeriod>,
    days: List<DayOfWeekEnum>,
    intervalHours: String,
    intervalStart: String,
    weeklyTimes: Map<DayOfWeekEnum, String>,
    oneShotDate: String,
    oneShotTime: String
): List<MedicationSchedule> = when (type) {
    FrequencyType.EXACT_TIME -> exactTimes.mapNotNull(::plannerParseTime).map {
        MedicationSchedule(0, 0, type, ScheduleDataCodec.exactTime(it.first, it.second), intakeGroupId = null)
    }.takeIf { it.size == exactTimes.size } ?: emptyList()
    FrequencyType.DAY_PERIOD -> periods.map {
        MedicationSchedule(0, 0, type, ScheduleDataCodec.dayPeriod(it), intakeGroupId = null)
    }
    FrequencyType.INTERVAL -> {
        val time = plannerParseTime(intervalStart)
        val hours = intervalHours.toIntOrNull()
        if (time != null && hours != null && hours > 0) listOf(
            MedicationSchedule(0, 0, type, ScheduleDataCodec.interval(hours, time.first, time.second), intakeGroupId = null)
        ) else emptyList()
    }
    FrequencyType.WEEKLY -> {
        val parsed = days.mapNotNull { day -> plannerParseTime(weeklyTimes[day].orEmpty())?.let { day to it } }
        if (days.isNotEmpty() && parsed.size == days.size) parsed.map { (day, time) ->
            MedicationSchedule(
                0, 0, type,
                ScheduleDataCodec.weekly(listOf(day), time.first, time.second),
                intakeGroupId = null
            )
        } else emptyList()
    }
    FrequencyType.ONE_SHOT -> {
        val date = runCatching {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }.parse(oneShotDate)
        }.getOrNull()
        val time = plannerParseTime(oneShotTime)
        if (date != null && time != null) {
            val trigger = Calendar.getInstance().apply {
                timeInMillis = date.time
                set(Calendar.HOUR_OF_DAY, time.first)
                set(Calendar.MINUTE, time.second)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            listOf(MedicationSchedule(0, 0, type, ScheduleDataCodec.oneShot(trigger), intakeGroupId = null))
        } else emptyList()
    }
    FrequencyType.TIME_RANGE -> emptyList()
}

private fun plannerParseTime(value: String): Pair<Int, Int>? {
    val parts = value.trim().split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull()
    val minute = parts[1].toIntOrNull()
    return if (hour != null && minute != null && hour in 0..23 && minute in 0..59) hour to minute else null
}

private fun plannerTime(hour: Int, minute: Int): String =
    "%02d:%02d".format(hour, minute)
