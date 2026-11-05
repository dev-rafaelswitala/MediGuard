package com.rafaelswitala.mediguard.domain.model

/**
 * Domain-Modelle für das Gesundheitsprofil (Profil-Tab).
 */

data class HealthProfile(
    val id: Long = 1,
    val fullName: String? = null,
    val addressLine: String? = null,
    val postalCode: String? = null,
    val city: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val bloodType: String? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val allergiesNotes: String? = null,
    val chronicConditions: String? = null,
    val implantsNotes: String? = null,
    val pregnancyNotes: String? = null,
    val smokingAlcoholNotes: String? = null,
    val vaccinationNotes: String? = null,
    val lastCheckupNotes: String? = null,
    val insuranceName: String? = null,
    val insuranceNumber: String? = null,
    val emergencyNotes: String? = null,
    val organDonor: Boolean? = null,
    val preferredPharmacy: String? = null,
    val preferredPharmacyAddress: String? = null,
    val preferredPharmacyPhone: String? = null,
    val pregnant: Boolean? = null,
    val smoking: Boolean? = null,
    val cigarettesPerDay: Int? = null,
    val healthInsuranceName: String? = null,
    val healthInsuranceNumber: String? = null,
    val additionalInsurance: String? = null,
    val careInsurance: String? = null,
    val insuranceNotes: String? = null,
    val aidsNotes: String? = null,
    val lastBloodPressure: String? = null,
    val lastBloodSugar: String? = null,
    val otherNotes: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun bmi(): Double? {
        val h = heightCm ?: return null
        val w = weightKg ?: return null
        if (h <= 0.0) return null
        val meters = h / 100.0
        return w / (meters * meters)
    }
}

data class HealthAllergy(
    val id: Long = 0,
    val name: String,
    val severity: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ContactCategory {
    DOCTOR,
    PHARMACY,
    FAMILY,
    OTHER;

    fun label(strings: com.rafaelswitala.mediguard.ui.localization.AppStrings): String = when (this) {
        DOCTOR -> strings.categoryDoctor
        PHARMACY -> strings.categoryPharmacy
        FAMILY -> strings.categoryFamily
        OTHER -> strings.categoryOther
    }
}

enum class HealthConditionType { CHRONIC, IMPLANT }

data class HealthCondition(
    val id: Long = 0,
    val name: String,
    val type: HealthConditionType,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class HealthContact(
    val id: Long = 0,
    val name: String,
    val category: ContactCategory = ContactCategory.DOCTOR,
    val specialty: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val isEmergency: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class DocumentCategory {
    LAB,
    LETTER,
    IMAGING,
    OTHER;

    fun label(strings: com.rafaelswitala.mediguard.ui.localization.AppStrings): String = when (this) {
        LAB -> strings.docCategoryLab
        LETTER -> strings.docCategoryLetter
        IMAGING -> strings.docCategoryImaging
        OTHER -> strings.docCategoryOther
    }
}

data class MedicalDocument(
    val id: Long = 0,
    val title: String,
    val category: DocumentCategory = DocumentCategory.OTHER,
    val fileName: String,
    val mimeType: String = "application/pdf",
    val documentDateMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String? = null
)
