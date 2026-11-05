package com.rafaelswitala.mediguard.viewmodel

/**
 * Zeigt Einnahmeverlauf mit Monatsnavigation, Filtern und nachträglicher Bearbeitung.
 */

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafaelswitala.mediguard.alarm.AlarmRingingService
import com.rafaelswitala.mediguard.domain.model.IntakeHistory
import com.rafaelswitala.mediguard.domain.model.IntakeStatus
import com.rafaelswitala.mediguard.domain.model.Medication
import com.rafaelswitala.mediguard.domain.model.MedicationPlanType
import com.rafaelswitala.mediguard.domain.repository.IntakeHistoryRepository
import com.rafaelswitala.mediguard.domain.repository.MedicationRepository
import com.rafaelswitala.mediguard.domain.usecases.ConfirmMedicationIntakeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intakeHistoryRepository: IntakeHistoryRepository,
    private val medicationRepository: MedicationRepository,
    private val confirmIntakeUseCase: ConfirmMedicationIntakeUseCase
) : ViewModel() {

    private val _uiState = mutableStateOf(HistoryUiState())
    val uiState: androidx.compose.runtime.State<HistoryUiState> = _uiState

    private val _selectedYearMonth = mutableStateOf(currentYearMonth())
    val selectedYearMonth: androidx.compose.runtime.State<YearMonthSelection> = _selectedYearMonth

    private val _filterMedicationId = mutableStateOf<Long?>(null)
    val filterMedicationId: androidx.compose.runtime.State<Long?> = _filterMedicationId

    private val _filterStatus = mutableStateOf<IntakeStatus?>(null)
    val filterStatus: androidx.compose.runtime.State<IntakeStatus?> = _filterStatus

    private val _onlyEditedLate = mutableStateOf(false)
    val onlyEditedLate: androidx.compose.runtime.State<Boolean> = _onlyEditedLate

    val allIntakeHistory: StateFlow<List<IntakeHistory>> = intakeHistoryRepository
        .getAllIntakeHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val medications: StateFlow<List<Medication>> = medicationRepository
        .getAllMedications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun filteredEntries(all: List<IntakeHistory>): List<IntakeHistory> {
        val ym = _selectedYearMonth.value
        val range = monthRangeMillis(ym.year, ym.month)
        return all.filter { entry ->
            entry.scheduledTime in range.first until range.second &&
                (_filterMedicationId.value == null || entry.medicationId == _filterMedicationId.value) &&
                (_filterStatus.value == null || entry.status == _filterStatus.value) &&
                (!_onlyEditedLate.value || entry.wasEditedLate)
        }.sortedByDescending { it.scheduledTime }
    }

    fun adherenceFor(entries: List<IntakeHistory>): Int {
        if (entries.isEmpty()) return 0
        return (entries.count { it.isTaken() } * 100) / entries.size
    }

    fun previousMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _selectedYearMonth.value.year)
            set(Calendar.MONTH, _selectedYearMonth.value.month - 1)
            add(Calendar.MONTH, -1)
        }
        _selectedYearMonth.value = YearMonthSelection(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1
        )
    }

    fun nextMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _selectedYearMonth.value.year)
            set(Calendar.MONTH, _selectedYearMonth.value.month - 1)
            add(Calendar.MONTH, 1)
        }
        val next = YearMonthSelection(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1
        )
        val current = currentYearMonth()
        if (next.year > current.year || (next.year == current.year && next.month > current.month)) {
            return
        }
        _selectedYearMonth.value = next
    }

    fun setFilterMedication(id: Long?) {
        _filterMedicationId.value = id
    }

    fun setFilterStatus(status: IntakeStatus?) {
        _filterStatus.value = status
    }

    fun setOnlyEditedLate(value: Boolean) {
        _onlyEditedLate.value = value
    }

    fun confirmIntake(intakeHistoryId: Long) {
        viewModelScope.launch {
            try {
                AlarmRingingService.stop(context)
                confirmIntakeUseCase(intakeHistoryId)
                _uiState.value = _uiState.value.copy(successMessage = "Intake confirmed")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to confirm intake: ${e.message}"
                )
            }
        }
    }

    fun confirmIntakes(intakeHistoryIds: List<Long>) {
        viewModelScope.launch {
            try {
                AlarmRingingService.stop(context)
                intakeHistoryIds.forEach { intakeHistoryId ->
                    confirmIntakeUseCase(intakeHistoryId)
                }
                _uiState.value = _uiState.value.copy(successMessage = "Intake confirmed")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to confirm intake: ${e.message}"
                )
            }
        }
    }

    fun editIntake(
        intakeHistoryId: Long,
        status: IntakeStatus,
        intakeTime: Long?,
        note: String?
    ) {
        viewModelScope.launch {
            try {
                val previous = intakeHistoryRepository.getIntakeHistoryById(intakeHistoryId)
                if (status == IntakeStatus.CONFIRMED &&
                    previous?.status != IntakeStatus.CONFIRMED
                ) {
                    confirmIntakeUseCase(
                        intakeHistoryId = intakeHistoryId,
                        intakeTime = intakeTime ?: System.currentTimeMillis()
                    )
                }
                intakeHistoryRepository.editIntake(
                    intakeHistoryId = intakeHistoryId,
                    status = status,
                    intakeTime = intakeTime,
                    note = note,
                    wasEditedLate = true
                )
                _uiState.value = _uiState.value.copy(successMessage = "Intake updated")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to edit intake: ${e.message}"
                )
            }
        }
    }

    fun takePrnNow(medication: Medication) {
        if (medication.planType != MedicationPlanType.PRN) return
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                intakeHistoryRepository.recordIntake(
                    IntakeHistory(
                        medicationId = medication.id,
                        scheduledTime = now,
                        actualIntakeTime = now,
                        dosage = medication.dosage,
                        dosageUnit = medication.dosageUnit,
                        medicationName = medication.name,
                        status = IntakeStatus.CONFIRMED
                    )
                )
                medicationRepository.decrementAfterIntake(medication.id)
                _uiState.value = _uiState.value.copy(successMessage = "PRN intake recorded")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to record PRN intake: ${e.message}"
                )
            }
        }
    }

    fun stopAlarm() {
        AlarmRingingService.stop(context)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }

    private fun currentYearMonth(): YearMonthSelection {
        val cal = Calendar.getInstance()
        return YearMonthSelection(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1
        )
    }

    private fun monthRangeMillis(year: Int, month: Int): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        val end = Calendar.getInstance().apply {
            timeInMillis = start
            add(Calendar.MONTH, 1)
        }.timeInMillis
        return start to end
    }
}

data class YearMonthSelection(val year: Int, val month: Int)

data class HistoryUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
