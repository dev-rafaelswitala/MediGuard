package com.rafaelswitala.mediguard.data.local.entity

/**
 * Datenbanktabelle für den Einnahmeverlauf mit Status und Zeitstempeln.
 */

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "intake_history",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["medicationId"]), Index(value = ["scheduledTime"]), Index(value = ["status"])]
)
data class IntakeHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val scheduleId: Long? = null,
    val scheduledTime: Long,
    val windowEndTime: Long? = null,
    val actualIntakeTime: Long? = null,
    val dosage: String,
    val dosageUnit: String,
    val medicationName: String,
    val status: String = "PENDING",
    val note: String? = null,
    val editedAt: Long? = null,
    val wasEditedLate: Boolean = false,
    val notificationId: Int? = null
)
