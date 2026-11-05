package com.rafaelswitala.mediguard.domain.usecases

import com.rafaelswitala.mediguard.alarm.AlarmScheduler
import com.rafaelswitala.mediguard.domain.repository.IntakeHistoryRepository
import com.rafaelswitala.mediguard.notification.MedicationNotificationManager
import javax.inject.Inject

/**
 * Schließt überfällige Einnahmefenster nach Prozess- oder Gerätestarts zuverlässig ab.
 */
class MarkExpiredWindowsUseCase @Inject constructor(
    private val intakeHistoryRepository: IntakeHistoryRepository,
    private val alarmScheduler: AlarmScheduler,
    private val notificationManager: MedicationNotificationManager
) {
    suspend operator fun invoke(now: Long = System.currentTimeMillis()) {
        val expired = intakeHistoryRepository.getExpiredWindows(now)
        if (expired.isEmpty()) return

        intakeHistoryRepository.markMissed(expired.map { it.id })
        expired.mapNotNull { it.notificationId }.distinct().forEach { notificationId ->
            alarmScheduler.cancelWindowEnd(notificationId)
            notificationManager.cancelNotification(notificationId)
        }
    }
}
