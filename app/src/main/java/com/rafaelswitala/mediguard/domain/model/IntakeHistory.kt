package com.rafaelswitala.mediguard.domain.model

/**
 * Speichert die Einnahmebestätigung und den Status einer Medikamenteneinnahme.
 */

import com.rafaelswitala.mediguard.ui.localization.AppStrings

data class IntakeHistory(
    val id: Long = 0,
    val medicationId: Long,
    val scheduleId: Long? = null,
    val scheduledTime: Long,
    val windowEndTime: Long? = null,
    val actualIntakeTime: Long? = null,
    val dosage: String,
    val dosageUnit: String,
    val medicationName: String,
    val status: IntakeStatus = IntakeStatus.PENDING,
    val note: String? = null,
    val editedAt: Long? = null,
    val wasEditedLate: Boolean = false,
    val notificationId: Int? = null
) {
    fun isTaken(): Boolean = actualIntakeTime != null && status == IntakeStatus.CONFIRMED

    fun isOpenForLateConfirm(now: Long = System.currentTimeMillis()): Boolean =
        status == IntakeStatus.PENDING ||
            status == IntakeStatus.SNOOZED ||
            status == IntakeStatus.MISSED ||
            (status == IntakeStatus.CONFIRMED && wasEditedLate)
}

enum class IntakeStatus {
    PENDING,
    CONFIRMED,
    MISSED,
    SNOOZED;

    fun label(strings: AppStrings): String = when (this) {
        PENDING -> strings.statusPending
        CONFIRMED -> strings.statusConfirmed
        MISSED -> strings.statusMissed
        SNOOZED -> strings.statusSnoozed
    }
}
