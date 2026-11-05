package com.rafaelswitala.mediguard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_profile")
data class HealthProfileEntity(
    @PrimaryKey
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
)

@Entity(tableName = "health_conditions")
data class HealthConditionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "health_allergies")
data class HealthAllergyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val severity: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "health_contacts")
data class HealthContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String = "DOCTOR",
    val specialty: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val isEmergency: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "medical_documents")
data class MedicalDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String = "OTHER",
    val fileName: String,
    val mimeType: String = "application/pdf",
    val documentDateMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String? = null
)
