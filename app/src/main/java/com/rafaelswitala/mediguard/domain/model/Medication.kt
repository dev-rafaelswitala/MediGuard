package com.rafaelswitala.mediguard.domain.model

import java.math.BigDecimal

/**
 * Medikamentenmodell inkl. Planart (Dauer, einmalig, Bedarf).
 */
data class Medication(
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val dosageUnit: String = "mg",
    val description: String = "",
    val treatmentType: TreatmentType = TreatmentType.ONGOING,
    val planType: MedicationPlanType = MedicationPlanType.RECURRING,
    val durationDays: Int? = null,
    val treatmentLimitDoses: Int? = null,
    val remainingDoses: Double? = null,
    val remainingVolumeMl: Double? = null,
    val dosePerIntakeMl: Double? = null,
    val doseQuantity: Double = 1.0,
    val medicationFormType: MedicationFormType = MedicationFormType.TABLET,
    val intakeGroupId: Int? = null,
    @Deprecated("Use schedule.intakeGroupId for slot grouping")
    val groupName: String? = null,
    val supplyAlertEnabled: Boolean = true,
    val supplyAlertThreshold: Int = 7,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    fun needsSupplyRestock(): Boolean {
        if (!supplyAlertEnabled) return false
        return if (medicationFormType.usesVolume()) {
            remainingVolumeMl != null && remainingVolumeMl <= supplyAlertThreshold.toDouble()
        } else {
            remainingDoses != null && remainingDoses <= supplyAlertThreshold.toDouble()
        }
    }

    fun stockLabel(strings: com.rafaelswitala.mediguard.ui.localization.AppStrings): String =
        if (medicationFormType.usesVolume()) {
            "${strings.remainingVolume}: ${formatMedicationAmount(remainingVolumeMl ?: 0.0)} ml"
        } else {
            "${strings.tabletsLeft}: ${formatMedicationAmount(remainingDoses ?: 0.0)}"
        }
}

fun formatMedicationAmount(value: Double): String =
    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

enum class TreatmentType {
    ONGOING,
    LIMITED
}

enum class MedicationPlanType {
    RECURRING,
    ONE_SHOT,
    PRN;

    fun label(strings: com.rafaelswitala.mediguard.ui.localization.AppStrings): String = when (this) {
        RECURRING -> strings.planRecurring
        ONE_SHOT -> strings.planOneShot
        PRN -> strings.planPrn
    }
}
