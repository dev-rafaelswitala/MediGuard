package com.rafaelswitala.mediguard.viewmodel

/**
 * Verwaltet Design (Hell/Dunkel), Sprache, Benachrichtigungsmodus, Klingelton und Tageszeiten.
 * Speichert Einstellungen in DataStore.
 */

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafaelswitala.mediguard.data.settings.AppLanguage
import com.rafaelswitala.mediguard.data.settings.AppAlertMode
import com.rafaelswitala.mediguard.alarm.AlarmScheduler
import com.rafaelswitala.mediguard.backup.MedicationBackupWorker
import com.rafaelswitala.mediguard.backup.MedicationPdfExporter
import com.rafaelswitala.mediguard.data.settings.AppPreferencesRepository
import com.rafaelswitala.mediguard.data.settings.AppSettings
import com.rafaelswitala.mediguard.data.settings.AppThemeMode
import com.rafaelswitala.mediguard.data.settings.BackupFrequency
import com.rafaelswitala.mediguard.domain.model.FrequencyType
import com.rafaelswitala.mediguard.domain.model.Medication
import com.rafaelswitala.mediguard.domain.repository.MedicationRepository
import com.rafaelswitala.mediguard.domain.repository.MedicationScheduleRepository
import com.rafaelswitala.mediguard.domain.repository.ProfileRepository
import com.rafaelswitala.mediguard.domain.settings.DayPeriodSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val scheduleRepository: MedicationScheduleRepository,
    private val medicationRepository: MedicationRepository,
    private val profileRepository: ProfileRepository,
    private val alarmScheduler: AlarmScheduler,
    private val pdfExporter: MedicationPdfExporter
) : ViewModel() {
    val settings: StateFlow<AppSettings> = appPreferencesRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )
    val medications: StateFlow<List<Medication>> = medicationRepository.getAllMedications().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val operationMessage = MutableStateFlow<String?>(null)

    fun setThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch {
            appPreferencesRepository.setThemeMode(themeMode)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            appPreferencesRepository.setLanguage(language)
        }
    }

    fun setAlertMode(alertMode: AppAlertMode) {
        viewModelScope.launch {
            appPreferencesRepository.setAlertMode(alertMode)
        }
    }

    fun setRingtone(uri: String?, title: String?) {
        viewModelScope.launch {
            appPreferencesRepository.setRingtone(uri, title)
        }
    }

    fun setDayPeriodSettings(dayPeriodSettings: DayPeriodSettings, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val validation = dayPeriodSettings.validate()
            if (!validation.isValid) {
                onResult(false)
                return@launch
            }
            appPreferencesRepository.setDayPeriodSettings(dayPeriodSettings)
            scheduleRepository.getAllActiveSchedules().first()
                .filter { it.scheduleType == FrequencyType.DAY_PERIOD }
                .forEach { alarmScheduler.scheduleAlarm(it) }
            onResult(true)
        }
    }

    fun setBackup(enabled: Boolean, frequency: BackupFrequency) {
        viewModelScope.launch {
            val effectiveFrequency = if (enabled && frequency == BackupFrequency.NONE) {
                BackupFrequency.WEEKLY
            } else {
                frequency
            }
            appPreferencesRepository.setBackupSettings(enabled, effectiveFrequency)
            MedicationBackupWorker.updateSchedule(context, enabled)
        }
    }

    fun exportPdf(
        fromDate: LocalDate,
        toDate: LocalDate,
        medicationIds: Set<Long>?,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching {
                pdfExporter.export(fromDate, toDate, medicationIds)
            }.onSuccess {
                operationMessage.value = "pdf_created"
                onResult(true)
            }.onFailure {
                operationMessage.value = it.message
                onResult(false)
            }
        }
    }

    fun deleteData(deleteProfile: Boolean, deleteMedications: Boolean, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                if (deleteMedications) {
                    medicationRepository.getAllMedications().first().forEach { medication ->
                        scheduleRepository.getSchedulesByMedicationId(medication.id).first()
                            .forEach { alarmScheduler.cancelAlarm(it.id) }
                        medicationRepository.deleteMedication(medication.id)
                    }
                }
                if (deleteProfile) profileRepository.clearAll()
            }.onSuccess {
                operationMessage.value = "data_deleted"
                onComplete()
            }.onFailure {
                operationMessage.value = it.message
            }
        }
    }

    fun clearOperationMessage() {
        operationMessage.value = null
    }
}
