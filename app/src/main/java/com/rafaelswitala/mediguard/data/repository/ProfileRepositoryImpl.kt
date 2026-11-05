package com.rafaelswitala.mediguard.data.repository

import android.content.Context
import android.net.Uri
import com.rafaelswitala.mediguard.data.local.dao.HealthAllergyDao
import com.rafaelswitala.mediguard.data.local.dao.HealthContactDao
import com.rafaelswitala.mediguard.data.local.dao.HealthConditionDao
import com.rafaelswitala.mediguard.data.local.dao.HealthProfileDao
import com.rafaelswitala.mediguard.data.local.dao.MedicalDocumentDao
import com.rafaelswitala.mediguard.data.local.entity.HealthAllergyEntity
import com.rafaelswitala.mediguard.data.local.entity.HealthContactEntity
import com.rafaelswitala.mediguard.data.local.entity.HealthConditionEntity
import com.rafaelswitala.mediguard.data.local.entity.HealthProfileEntity
import com.rafaelswitala.mediguard.data.local.entity.MedicalDocumentEntity
import com.rafaelswitala.mediguard.domain.model.ContactCategory
import com.rafaelswitala.mediguard.domain.model.DocumentCategory
import com.rafaelswitala.mediguard.domain.model.HealthAllergy
import com.rafaelswitala.mediguard.domain.model.HealthContact
import com.rafaelswitala.mediguard.domain.model.HealthCondition
import com.rafaelswitala.mediguard.domain.model.HealthConditionType
import com.rafaelswitala.mediguard.domain.model.HealthProfile
import com.rafaelswitala.mediguard.domain.model.MedicalDocument
import com.rafaelswitala.mediguard.domain.repository.ProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileDao: HealthProfileDao,
    private val allergyDao: HealthAllergyDao,
    private val contactDao: HealthContactDao,
    private val conditionDao: HealthConditionDao,
    private val documentDao: MedicalDocumentDao
) : ProfileRepository {

    private val documentsDir: File by lazy {
        File(context.filesDir, "documents").also { it.mkdirs() }
    }

    override fun observeProfile(): Flow<HealthProfile> =
        profileDao.observeProfile().map { it?.toDomain() ?: HealthProfile() }

    override suspend fun saveProfile(profile: HealthProfile) {
        profileDao.upsert(profile.toEntity().copy(updatedAt = System.currentTimeMillis()))
    }

    override fun observeAllergies(): Flow<List<HealthAllergy>> =
        allergyDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addAllergy(allergy: HealthAllergy): Long =
        allergyDao.insert(allergy.toEntity())

    override suspend fun deleteAllergy(allergy: HealthAllergy) {
        allergyDao.delete(allergy.toEntity())
    }

    override fun observeConditions(): Flow<List<HealthCondition>> =
        conditionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addCondition(condition: HealthCondition): Long =
        conditionDao.insert(condition.toEntity())

    override suspend fun deleteCondition(condition: HealthCondition) {
        conditionDao.delete(condition.toEntity())
    }

    override fun observeContacts(): Flow<List<HealthContact>> =
        contactDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addContact(contact: HealthContact): Long =
        contactDao.insert(contact.toEntity())

    override suspend fun updateContact(contact: HealthContact) {
        contactDao.update(contact.toEntity())
    }

    override suspend fun deleteContact(contact: HealthContact) {
        contactDao.delete(contact.toEntity())
    }

    override fun observeDocuments(): Flow<List<MedicalDocument>> =
        documentDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun importDocument(
        sourceUri: Uri,
        title: String,
        category: String,
        documentDateMillis: Long?,
        notes: String?
    ): Long {
        val extension = context.contentResolver.getType(sourceUri)
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
            ?: "pdf"
        val storedName = "${UUID.randomUUID()}.$extension"
        val target = File(documentsDir, storedName)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not read document")

        return documentDao.insert(
            MedicalDocumentEntity(
                title = title.ifBlank { storedName },
                category = category,
                fileName = storedName,
                mimeType = context.contentResolver.getType(sourceUri) ?: "application/pdf",
                documentDateMillis = documentDateMillis,
                notes = notes
            )
        )
    }

    override suspend fun insertGeneratedDocument(
        file: File,
        title: String,
        documentDateMillis: Long
    ): Long {
        require(file.parentFile?.canonicalFile == documentsDir.canonicalFile) {
            "Generated document must be stored in the app documents directory"
        }
        return documentDao.insert(
            MedicalDocumentEntity(
                title = title,
                category = DocumentCategory.OTHER.name,
                fileName = file.name,
                mimeType = "application/pdf",
                documentDateMillis = documentDateMillis,
                notes = null
            )
        )
    }

    override suspend fun deleteDocument(document: MedicalDocument) {
        documentFile(document).delete()
        documentDao.delete(document.toEntity())
    }

    override fun documentFile(document: MedicalDocument): File =
        File(documentsDir, document.fileName)

    override suspend fun clearAll() {
        documentsDir.listFiles()?.forEach { it.delete() }
        documentDao.clear()
        contactDao.clear()
        allergyDao.clear()
        conditionDao.clear()
        profileDao.clear()
    }

    private fun HealthProfile.toEntity() = HealthProfileEntity(
        id = id,
        fullName = fullName,
        addressLine = addressLine,
        postalCode = postalCode,
        city = city,
        phone = phone,
        email = email,
        bloodType = bloodType,
        heightCm = heightCm,
        weightKg = weightKg,
        allergiesNotes = allergiesNotes,
        chronicConditions = chronicConditions,
        implantsNotes = implantsNotes,
        pregnancyNotes = pregnancyNotes,
        smokingAlcoholNotes = smokingAlcoholNotes,
        vaccinationNotes = vaccinationNotes,
        lastCheckupNotes = lastCheckupNotes,
        insuranceName = insuranceName,
        insuranceNumber = insuranceNumber,
        emergencyNotes = emergencyNotes,
        organDonor = organDonor,
        preferredPharmacy = preferredPharmacy,
        preferredPharmacyAddress = preferredPharmacyAddress,
        preferredPharmacyPhone = preferredPharmacyPhone,
        pregnant = pregnant,
        smoking = smoking,
        cigarettesPerDay = cigarettesPerDay,
        healthInsuranceName = healthInsuranceName ?: insuranceName,
        healthInsuranceNumber = healthInsuranceNumber ?: insuranceNumber,
        additionalInsurance = additionalInsurance,
        careInsurance = careInsurance,
        insuranceNotes = insuranceNotes,
        aidsNotes = aidsNotes,
        lastBloodPressure = lastBloodPressure,
        lastBloodSugar = lastBloodSugar,
        otherNotes = otherNotes,
        updatedAt = updatedAt
    )

    private fun HealthProfileEntity.toDomain() = HealthProfile(
        id = id,
        fullName = fullName,
        addressLine = addressLine,
        postalCode = postalCode,
        city = city,
        phone = phone,
        email = email,
        bloodType = bloodType,
        heightCm = heightCm,
        weightKg = weightKg,
        allergiesNotes = allergiesNotes,
        chronicConditions = chronicConditions,
        implantsNotes = implantsNotes,
        pregnancyNotes = pregnancyNotes,
        smokingAlcoholNotes = smokingAlcoholNotes,
        vaccinationNotes = vaccinationNotes,
        lastCheckupNotes = lastCheckupNotes,
        insuranceName = insuranceName,
        insuranceNumber = insuranceNumber,
        emergencyNotes = emergencyNotes,
        organDonor = organDonor,
        preferredPharmacy = preferredPharmacy,
        preferredPharmacyAddress = preferredPharmacyAddress,
        preferredPharmacyPhone = preferredPharmacyPhone,
        pregnant = pregnant,
        smoking = smoking,
        cigarettesPerDay = cigarettesPerDay,
        healthInsuranceName = healthInsuranceName ?: insuranceName,
        healthInsuranceNumber = healthInsuranceNumber ?: insuranceNumber,
        additionalInsurance = additionalInsurance,
        careInsurance = careInsurance,
        insuranceNotes = insuranceNotes,
        aidsNotes = aidsNotes,
        lastBloodPressure = lastBloodPressure,
        lastBloodSugar = lastBloodSugar,
        otherNotes = otherNotes,
        updatedAt = updatedAt
    )

    private fun HealthAllergy.toEntity() = HealthAllergyEntity(
        id = id,
        name = name,
        severity = severity,
        notes = notes,
        createdAt = createdAt
    )

    private fun HealthAllergyEntity.toDomain() = HealthAllergy(
        id = id,
        name = name,
        severity = severity,
        notes = notes,
        createdAt = createdAt
    )

    private fun HealthCondition.toEntity() = HealthConditionEntity(
        id = id,
        name = name,
        type = type.name,
        notes = notes,
        createdAt = createdAt
    )

    private fun HealthConditionEntity.toDomain() = HealthCondition(
        id = id,
        name = name,
        type = runCatching { HealthConditionType.valueOf(type) }.getOrDefault(HealthConditionType.CHRONIC),
        notes = notes,
        createdAt = createdAt
    )

    private fun HealthContact.toEntity() = HealthContactEntity(
        id = id,
        name = name,
        category = category.name,
        specialty = specialty,
        phone = phone,
        email = email,
        address = address,
        notes = notes,
        isEmergency = isEmergency,
        createdAt = createdAt
    )

    private fun HealthContactEntity.toDomain() = HealthContact(
        id = id,
        name = name,
        category = runCatching { ContactCategory.valueOf(category) }.getOrDefault(ContactCategory.OTHER),
        specialty = specialty,
        phone = phone,
        email = email,
        address = address,
        notes = notes,
        isEmergency = isEmergency,
        createdAt = createdAt
    )

    private fun MedicalDocument.toEntity() = MedicalDocumentEntity(
        id = id,
        title = title,
        category = category.name,
        fileName = fileName,
        mimeType = mimeType,
        documentDateMillis = documentDateMillis,
        createdAt = createdAt,
        notes = notes
    )

    private fun MedicalDocumentEntity.toDomain() = MedicalDocument(
        id = id,
        title = title,
        category = runCatching { DocumentCategory.valueOf(category) }.getOrDefault(DocumentCategory.OTHER),
        fileName = fileName,
        mimeType = mimeType,
        documentDateMillis = documentDateMillis,
        createdAt = createdAt,
        notes = notes
    )
}
