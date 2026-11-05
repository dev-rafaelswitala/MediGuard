package com.rafaelswitala.mediguard.viewmodel

/**
 * Verwaltet die Medikamentenliste, Bestand, Gruppierungsvorschläge und Erinnerungszeiten.
 * Synchronisiert mit Datenbank und Alarm-Scheduler bei Änderungen.
 */

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafaelswitala.mediguard.alarm.AlarmScheduler
import com.rafaelswitala.mediguard.data.settings.DayPeriodSettingsProvider
import com.rafaelswitala.mediguard.domain.grouping.MedicationGroupingService
import com.rafaelswitala.mediguard.domain.model.Medication
import com.rafaelswitala.mediguard.domain.model.MedicationSchedule
import com.rafaelswitala.mediguard.domain.reminder.NextIntakeCalculator
import com.rafaelswitala.mediguard.domain.reminder.UpcomingIntake
import com.rafaelswitala.mediguard.domain.repository.MedicationRepository
import com.rafaelswitala.mediguard.domain.repository.MedicationScheduleRepository
import com.rafaelswitala.mediguard.domain.usecases.AddMedicationUseCase
import com.rafaelswitala.mediguard.domain.usecases.ScheduleMedicationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

/**
 * Datei für Medikamentenlisten, Bestand und Gruppierung.
 * Das ViewModel verbindet UI-Aktionen mit Repositories und Alarmplanung.
 */
@HiltViewModel
class MedicationViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val scheduleRepository: MedicationScheduleRepository,
    private val addMedicationUseCase: AddMedicationUseCase,
    private val scheduleMedicationUseCase: ScheduleMedicationUseCase,
    private val alarmScheduler: AlarmScheduler,
    private val dayPeriodSettingsProvider: DayPeriodSettingsProvider
) : ViewModel() {

    private val _uiState = mutableStateOf(MedicationUiState())
    val uiState: androidx.compose.runtime.State<MedicationUiState> = _uiState

    private val _editingMedication = mutableStateOf<Medication?>(null)
    val editingMedication: androidx.compose.runtime.State<Medication?> = _editingMedication

    private val _editingSchedules = mutableStateOf<List<MedicationSchedule>>(emptyList())
    val editingSchedules: androidx.compose.runtime.State<List<MedicationSchedule>> = _editingSchedules
    private var suppressGroupCleanup = false

    val activeMedications: StateFlow<List<Medication>> = medicationRepository
        .getAllActiveMedications()
        .map(::sortMedicationsByName)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allMedications: StateFlow<List<Medication>> = medicationRepository
        .getAllMedications()
        .map(::sortMedicationsByName)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allActiveSchedules: StateFlow<List<MedicationSchedule>> = scheduleRepository
        .getAllActiveSchedules()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val lowSupplyMedications: StateFlow<List<Medication>> = medicationRepository
        .getAllActiveMedications()
        .map { medications -> medications.filter { it.needsSupplyRestock() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val upcomingIntakes: StateFlow<List<UpcomingIntake>> = combine(
        medicationRepository.getAllActiveMedications(),
        scheduleRepository.getAllActiveSchedules(),
        dayPeriodSettingsProvider.settings
    ) { medications, schedules, daySettings ->
        val byMedication = schedules.groupBy { it.medicationId }
        NextIntakeCalculator.upcoming(
            medications = medications,
            schedulesByMedication = byMedication,
            dayPeriodSettings = daySettings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            scheduleRepository.getAllActiveSchedules().collect { schedules ->
                if (!suppressGroupCleanup) runGroupCleanup(schedules)
            }
        }
    }

    fun addMedication(medication: Medication) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val medicationId = addMedicationUseCase(medication)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Medication and reminders saved",
                    lastAddedMedicationId = medicationId
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to save medication: ${e.message}"
                )
            }
        }
    }

    fun updateMedication(medication: Medication) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                medicationRepository.updateMedication(medication)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Medication updated"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update medication: ${e.message}"
                )
            }
        }
    }

    fun loadMedicationForEdit(medicationId: Long?) {
        if (medicationId == null) {
            _editingMedication.value = null
            _editingSchedules.value = emptyList()
            return
        }

        viewModelScope.launch {
            _editingMedication.value = medicationRepository.getMedicationById(medicationId)
            _editingSchedules.value = scheduleRepository.getSchedulesByMedicationId(medicationId).first()
        }
    }

    fun deleteMedication(medicationId: Long) {
        deleteMedications(listOf(medicationId))
    }

    fun deleteMedications(medicationIds: Collection<Long>) {
        viewModelScope.launch {
            try {
                medicationIds.distinct().forEach { medicationId ->
                    scheduleRepository.getSchedulesByMedicationId(medicationId)
                        .first()
                        .forEach { alarmScheduler.cancelAlarm(it.id) }
                    medicationRepository.deleteMedication(medicationId)
                }
                _uiState.value = _uiState.value.copy(successMessage = "Medication deleted")
                runGroupCleanup(scheduleRepository.getAllActiveSchedules().first())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete medication: ${e.message}"
                )
            }
        }
    }

    fun addStock(medicationId: Long, addedAmount: Double) {
        viewModelScope.launch {
            try {
                val medication = medicationRepository.getMedicationById(medicationId) ?: return@launch
                if (medication.medicationFormType.usesVolume()) {
                    medicationRepository.addRemainingVolume(medicationId, addedAmount.toDouble())
                } else {
                    medicationRepository.addRemainingDoses(medicationId, addedAmount)
                }
                _uiState.value = _uiState.value.copy(successMessage = "Stock updated")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to update stock: ${e.message}"
                )
            }
        }
    }

    fun replaceSchedules(medicationId: Long, schedules: List<MedicationSchedule>, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                replaceSchedulesInternal(medicationId, schedules)
                _uiState.value = _uiState.value.copy(successMessage = "Schedule saved")
                onComplete()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to save schedule: ${e.message}")
            }
        }
    }

    fun createGroup(
        medicationIds: Collection<Long>,
        scheduleTemplate: MedicationSchedule
    ) {
        viewModelScope.launch {
            suppressGroupCleanup = true
            try {
                val allSchedules = scheduleRepository.getAllActiveSchedules().first()
                val groupId = MedicationGroupingService.nextAvailableGroupId(allSchedules)
                medicationIds.distinct().forEach { medicationId ->
                    replaceSchedulesInternal(
                        medicationId,
                        listOf(scheduleTemplate.copy(medicationId = medicationId, intakeGroupId = null))
                    )
                }
                scheduleRepository.getAllActiveSchedules().first()
                    .filter { it.medicationId in medicationIds }
                    .forEach { scheduleRepository.updateIntakeGroup(it.id, groupId) }
            } finally {
                suppressGroupCleanup = false
                runGroupCleanup(scheduleRepository.getAllActiveSchedules().first())
            }
        }
    }

    fun addMedicationsToGroup(
        medicationIds: Collection<Long>,
        groupId: Int,
        groupTemplate: MedicationSchedule
    ) {
        viewModelScope.launch {
            medicationIds.distinct().forEach { medicationId ->
                replaceSchedulesInternal(
                    medicationId,
                    listOf(groupTemplate.copy(id = 0, medicationId = medicationId, intakeGroupId = groupId))
                )
            }
        }
    }

    fun ungroupAndRemoveSchedules(medicationIds: Collection<Long>) {
        viewModelScope.launch {
            medicationIds.distinct().forEach { medicationId ->
                replaceSchedulesInternal(medicationId, emptyList())
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }

    private suspend fun persistSchedules(medicationId: Long, scheduleDrafts: List<MedicationSchedule>) {
        scheduleDrafts.forEach { draft ->
            val schedule = draft.copy(medicationId = medicationId)
            val scheduleId = scheduleMedicationUseCase(schedule)
            alarmScheduler.scheduleAlarm(schedule.copy(id = scheduleId))
        }
    }

    private suspend fun replaceSchedulesInternal(
        medicationId: Long,
        schedules: List<MedicationSchedule>
    ) {
        scheduleRepository.getSchedulesByMedicationId(medicationId)
            .first()
            .forEach { alarmScheduler.cancelAlarm(it.id) }
        scheduleRepository.deleteSchedulesByMedicationId(medicationId)
        persistSchedules(medicationId, schedules.map { it.copy(id = 0) })
    }

    private suspend fun runGroupCleanup(schedules: List<MedicationSchedule>) {
        MedicationGroupingService.cleanupGroups(schedules).forEach { (id, groupId) ->
            scheduleRepository.updateIntakeGroup(id, groupId)
        }
    }

}

private fun sortMedicationsByName(medications: List<Medication>): List<Medication> {
    val collator = Collator.getInstance(Locale.getDefault()).apply {
        strength = Collator.PRIMARY
    }
    return medications.sortedWith { first, second -> collator.compare(first.name, second.name) }
}

data class MedicationUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val lastAddedMedicationId: Long? = null
)
