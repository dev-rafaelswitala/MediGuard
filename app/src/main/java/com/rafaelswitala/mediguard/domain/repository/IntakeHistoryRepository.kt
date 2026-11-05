package com.rafaelswitala.mediguard.domain.repository

import com.rafaelswitala.mediguard.domain.model.IntakeHistory
import com.rafaelswitala.mediguard.domain.model.IntakeStatus
import kotlinx.coroutines.flow.Flow

interface IntakeHistoryRepository {
    suspend fun recordIntake(intakeHistory: IntakeHistory): Long
    suspend fun updateIntakeStatus(intakeHistoryId: Long, status: String, intakeTime: Long?)
    suspend fun updateIntakeDetails(intakeHistory: IntakeHistory)
    suspend fun editIntake(
        intakeHistoryId: Long,
        status: IntakeStatus,
        intakeTime: Long?,
        note: String?,
        wasEditedLate: Boolean
    )
    suspend fun markMissed(ids: List<Long>)
    suspend fun setNotificationId(ids: List<Long>, notificationId: Int)
    suspend fun getIntakeHistoryById(id: Long): IntakeHistory?
    suspend fun getIntakeHistoryByNotificationId(notificationId: Int): List<IntakeHistory>
    suspend fun getExpiredWindows(now: Long): List<IntakeHistory>
    suspend fun hasOccurrence(scheduleId: Long, scheduledTime: Long): Boolean
    fun getIntakeHistoryByMedicationId(medicationId: Long): Flow<List<IntakeHistory>>
    fun getAllIntakeHistory(): Flow<List<IntakeHistory>>
    fun getIntakeHistoryInRange(from: Long, to: Long): Flow<List<IntakeHistory>>
    fun getIntakeHistoryByStatus(status: String): Flow<List<IntakeHistory>>
    fun getIntakeHistoryByMedicationAndStatus(medicationId: Long, status: String): Flow<List<IntakeHistory>>
}
