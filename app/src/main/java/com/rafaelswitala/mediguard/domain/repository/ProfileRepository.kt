package com.rafaelswitala.mediguard.domain.repository

import android.net.Uri
import com.rafaelswitala.mediguard.domain.model.HealthAllergy
import com.rafaelswitala.mediguard.domain.model.HealthContact
import com.rafaelswitala.mediguard.domain.model.HealthCondition
import com.rafaelswitala.mediguard.domain.model.HealthProfile
import com.rafaelswitala.mediguard.domain.model.MedicalDocument
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ProfileRepository {
    fun observeProfile(): Flow<HealthProfile>
    suspend fun saveProfile(profile: HealthProfile)
    fun observeAllergies(): Flow<List<HealthAllergy>>
    suspend fun addAllergy(allergy: HealthAllergy): Long
    suspend fun deleteAllergy(allergy: HealthAllergy)
    fun observeConditions(): Flow<List<HealthCondition>>
    suspend fun addCondition(condition: HealthCondition): Long
    suspend fun deleteCondition(condition: HealthCondition)
    fun observeContacts(): Flow<List<HealthContact>>
    suspend fun addContact(contact: HealthContact): Long
    suspend fun updateContact(contact: HealthContact)
    suspend fun deleteContact(contact: HealthContact)
    fun observeDocuments(): Flow<List<MedicalDocument>>
    suspend fun importDocument(
        sourceUri: Uri,
        title: String,
        category: String,
        documentDateMillis: Long?,
        notes: String?
    ): Long
    suspend fun deleteDocument(document: MedicalDocument)
    fun documentFile(document: MedicalDocument): File
    suspend fun clearAll()
}
