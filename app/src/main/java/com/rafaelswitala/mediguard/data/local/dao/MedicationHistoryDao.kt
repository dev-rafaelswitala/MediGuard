package com.rafaelswitala.mediguard.data.local.dao

/**
 * Datenzugriff: Einnahmeverlauf speichern und abrufen.
 */

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.rafaelswitala.mediguard.data.local.entity.IntakeHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IntakeHistoryDao {
    @Insert
    suspend fun insert(intakeHistory: IntakeHistoryEntity): Long

    @Update
    suspend fun update(intakeHistory: IntakeHistoryEntity)

    @Delete
    suspend fun delete(intakeHistory: IntakeHistoryEntity)

    @Query("SELECT * FROM intake_history WHERE id = :id")
    suspend fun getIntakeHistoryById(id: Long): IntakeHistoryEntity?

    @Query("SELECT * FROM intake_history WHERE notificationId = :notificationId")
    suspend fun getIntakeHistoryByNotificationId(notificationId: Int): List<IntakeHistoryEntity>

    @Query(
        """
        SELECT * FROM intake_history
        WHERE status IN ('PENDING', 'SNOOZED')
          AND windowEndTime IS NOT NULL
          AND windowEndTime < :now
        """
    )
    suspend fun getExpiredWindows(now: Long): List<IntakeHistoryEntity>

    @Query("SELECT * FROM intake_history WHERE medicationId = :medicationId ORDER BY scheduledTime DESC")
    fun getIntakeHistoryByMedicationId(medicationId: Long): Flow<List<IntakeHistoryEntity>>

    @Query("SELECT * FROM intake_history ORDER BY scheduledTime DESC")
    fun getAllIntakeHistory(): Flow<List<IntakeHistoryEntity>>

    @Query("SELECT * FROM intake_history WHERE scheduledTime >= :from AND scheduledTime <= :to ORDER BY scheduledTime DESC")
    fun getIntakeHistoryInRange(from: Long, to: Long): Flow<List<IntakeHistoryEntity>>

    @Query("SELECT * FROM intake_history WHERE status = :status ORDER BY scheduledTime DESC")
    fun getIntakeHistoryByStatus(status: String): Flow<List<IntakeHistoryEntity>>

    @Query(
        "SELECT * FROM intake_history WHERE medicationId = :medicationId AND status = :status " +
            "ORDER BY scheduledTime DESC"
    )
    fun getIntakeHistoryByMedicationAndStatus(
        medicationId: Long,
        status: String
    ): Flow<List<IntakeHistoryEntity>>

    @Query("UPDATE intake_history SET status = :status, actualIntakeTime = :intakeTime WHERE id = :id")
    suspend fun updateIntakeStatus(id: Long, status: String, intakeTime: Long?)

    @Query(
        """
        UPDATE intake_history SET
            status = :status,
            actualIntakeTime = :intakeTime,
            note = :note,
            editedAt = :editedAt,
            wasEditedLate = :wasEditedLate
        WHERE id = :id
        """
    )
    suspend fun updateIntakeDetails(
        id: Long,
        status: String,
        intakeTime: Long?,
        note: String?,
        editedAt: Long?,
        wasEditedLate: Boolean
    )

    @Query(
        """
        UPDATE intake_history SET status = 'MISSED'
        WHERE id IN (:ids) AND status IN ('PENDING', 'SNOOZED')
        """
    )
    suspend fun markMissed(ids: List<Long>)

    @Query("UPDATE intake_history SET notificationId = :notificationId WHERE id IN (:ids)")
    suspend fun setNotificationId(ids: List<Long>, notificationId: Int)
}
