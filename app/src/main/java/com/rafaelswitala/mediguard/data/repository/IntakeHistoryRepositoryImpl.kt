package com.rafaelswitala.mediguard.data.repository

/**
 * Implementierung des IntakeHistoryRepository.
 */

import com.rafaelswitala.mediguard.data.local.dao.IntakeHistoryDao
import com.rafaelswitala.mediguard.data.local.entity.IntakeHistoryEntity
import com.rafaelswitala.mediguard.domain.model.IntakeHistory
import com.rafaelswitala.mediguard.domain.model.IntakeStatus
import com.rafaelswitala.mediguard.domain.repository.IntakeHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class IntakeHistoryRepositoryImpl @Inject constructor(
    private val intakeHistoryDao: IntakeHistoryDao
) : IntakeHistoryRepository {

    override suspend fun recordIntake(intakeHistory: IntakeHistory): Long =
        intakeHistoryDao.insert(intakeHistory.toEntity())

    override suspend fun updateIntakeStatus(intakeHistoryId: Long, status: String, intakeTime: Long?) {
        intakeHistoryDao.updateIntakeStatus(intakeHistoryId, status, intakeTime)
    }

    override suspend fun updateIntakeDetails(intakeHistory: IntakeHistory) {
        intakeHistoryDao.update(intakeHistory.toEntity())
    }

    override suspend fun editIntake(
        intakeHistoryId: Long,
        status: IntakeStatus,
        intakeTime: Long?,
        note: String?,
        wasEditedLate: Boolean
    ) {
        intakeHistoryDao.updateIntakeDetails(
            id = intakeHistoryId,
            status = status.name,
            intakeTime = intakeTime,
            note = note,
            editedAt = System.currentTimeMillis(),
            wasEditedLate = wasEditedLate
        )
    }

    override suspend fun markMissed(ids: List<Long>) {
        if (ids.isNotEmpty()) intakeHistoryDao.markMissed(ids)
    }

    override suspend fun setNotificationId(ids: List<Long>, notificationId: Int) {
        if (ids.isNotEmpty()) intakeHistoryDao.setNotificationId(ids, notificationId)
    }

    override suspend fun getIntakeHistoryById(id: Long): IntakeHistory? =
        intakeHistoryDao.getIntakeHistoryById(id)?.toDomain()

    override suspend fun getIntakeHistoryByNotificationId(notificationId: Int): List<IntakeHistory> =
        intakeHistoryDao.getIntakeHistoryByNotificationId(notificationId).map { it.toDomain() }

    override suspend fun getExpiredWindows(now: Long): List<IntakeHistory> =
        intakeHistoryDao.getExpiredWindows(now).map { it.toDomain() }

    override fun getIntakeHistoryByMedicationId(medicationId: Long): Flow<List<IntakeHistory>> =
        intakeHistoryDao.getIntakeHistoryByMedicationId(medicationId).map { it.map { e -> e.toDomain() } }

    override fun getAllIntakeHistory(): Flow<List<IntakeHistory>> =
        intakeHistoryDao.getAllIntakeHistory().map { it.map { e -> e.toDomain() } }

    override fun getIntakeHistoryInRange(from: Long, to: Long): Flow<List<IntakeHistory>> =
        intakeHistoryDao.getIntakeHistoryInRange(from, to).map { it.map { e -> e.toDomain() } }

    override fun getIntakeHistoryByStatus(status: String): Flow<List<IntakeHistory>> =
        intakeHistoryDao.getIntakeHistoryByStatus(status).map { it.map { e -> e.toDomain() } }

    override fun getIntakeHistoryByMedicationAndStatus(
        medicationId: Long,
        status: String
    ): Flow<List<IntakeHistory>> =
        intakeHistoryDao.getIntakeHistoryByMedicationAndStatus(medicationId, status)
            .map { it.map { e -> e.toDomain() } }

    private fun IntakeHistory.toEntity(): IntakeHistoryEntity = IntakeHistoryEntity(
        id = id,
        medicationId = medicationId,
        scheduleId = scheduleId,
        scheduledTime = scheduledTime,
        windowEndTime = windowEndTime,
        actualIntakeTime = actualIntakeTime,
        dosage = dosage,
        dosageUnit = dosageUnit,
        medicationName = medicationName,
        status = status.name,
        note = note,
        editedAt = editedAt,
        wasEditedLate = wasEditedLate,
        notificationId = notificationId
    )

    private fun IntakeHistoryEntity.toDomain(): IntakeHistory = IntakeHistory(
        id = id,
        medicationId = medicationId,
        scheduleId = scheduleId,
        scheduledTime = scheduledTime,
        windowEndTime = windowEndTime,
        actualIntakeTime = actualIntakeTime,
        dosage = dosage,
        dosageUnit = dosageUnit,
        medicationName = medicationName,
        status = runCatching { IntakeStatus.valueOf(status) }.getOrDefault(IntakeStatus.PENDING),
        note = note,
        editedAt = editedAt,
        wasEditedLate = wasEditedLate,
        notificationId = notificationId
    )
}
