package com.rafaelswitala.mediguard.alarm

import android.content.Context
import com.rafaelswitala.mediguard.domain.model.IntakeHistory
import com.rafaelswitala.mediguard.domain.model.IntakeStatus
import com.rafaelswitala.mediguard.domain.model.MedicationSchedule
import com.rafaelswitala.mediguard.domain.reminder.ReminderPlanFactory
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Persistiert übersprungene Fälligkeiten als MISSED, statt sie still zu verwerfen.
 */
object MissedOccurrenceRecorder {
    suspend fun recordIfNeeded(
        context: Context,
        medicationId: Long,
        scheduleId: Long,
        scheduledTime: Long,
        schedule: MedicationSchedule
    ) = withContext(Dispatchers.IO) {
        if (scheduledTime <= 0L) return@withContext
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            AlarmReceiverEntryPoint::class.java
        )
        if (entryPoint.intakeHistoryRepository().hasOccurrence(scheduleId, scheduledTime)) {
            return@withContext
        }

        val medication = entryPoint.medicationRepository().getMedicationById(medicationId)
            ?: return@withContext
        val windowEnd = ReminderPlanFactory.windowEndMillis(
            schedule,
            scheduledTime,
            entryPoint.dayPeriodSettingsProvider().current()
        )
        entryPoint.intakeHistoryRepository().recordIntake(
            IntakeHistory(
                medicationId = medication.id,
                scheduleId = scheduleId,
                scheduledTime = scheduledTime,
                windowEndTime = windowEnd,
                dosage = medication.dosage,
                dosageUnit = medication.dosageUnit,
                medicationName = medication.name,
                status = IntakeStatus.MISSED
            )
        )
    }
}
