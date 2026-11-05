package com.rafaelswitala.mediguard.data.local.entity

/**
 * Datenbanktabelle für Erinnerungszeiten inkl. Slot-Gruppen-ID.
 */

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_schedules",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["medicationId"])]
)
data class MedicationScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val scheduleType: String,
    val scheduleData: String,
    val reminderMinutes: Int = 15,
    val snoozeOptions: String = "5,10,15,30,60",
    val isEnabled: Boolean = true,
    val intakeGroupId: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)
