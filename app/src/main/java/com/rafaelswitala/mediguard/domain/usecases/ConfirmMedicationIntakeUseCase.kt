package com.rafaelswitala.mediguard.domain.usecases

/**
 * Bestätigt die Einnahme eines Medikaments und verringert den Bestand.
 */

import com.rafaelswitala.mediguard.alarm.AlarmScheduler
import com.rafaelswitala.mediguard.domain.model.IntakeStatus
import com.rafaelswitala.mediguard.domain.repository.IntakeHistoryRepository
import com.rafaelswitala.mediguard.domain.repository.MedicationRepository
import com.rafaelswitala.mediguard.notification.MedicationNotificationManager
import javax.inject.Inject

class ConfirmMedicationIntakeUseCase @Inject constructor(
    private val intakeHistoryRepository: IntakeHistoryRepository,
    private val medicationRepository: MedicationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val notificationManager: MedicationNotificationManager
) {
    suspend operator fun invoke(
        intakeHistoryId: Long,
        intakeTime: Long = System.currentTimeMillis()
    ) {
        val history = intakeHistoryRepository.getIntakeHistoryById(intakeHistoryId) ?: return
        if (history.status == IntakeStatus.CONFIRMED && history.actualIntakeTime != null) return

        intakeHistoryRepository.updateIntakeStatus(
            intakeHistoryId,
            IntakeStatus.CONFIRMED.name,
            intakeTime
        )
        medicationRepository.decrementAfterIntake(history.medicationId)

        history.notificationId?.let { notificationId ->
            val hasOpenIntakes = intakeHistoryRepository
                .getIntakeHistoryByNotificationId(notificationId)
                .any { it.status == IntakeStatus.PENDING || it.status == IntakeStatus.SNOOZED }
            if (!hasOpenIntakes) {
                alarmScheduler.cancelWindowEnd(notificationId)
                notificationManager.cancelNotification(notificationId)
            }
        }
    }
}
