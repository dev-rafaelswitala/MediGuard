package com.rafaelswitala.mediguard.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rafaelswitala.mediguard.data.local.entity.HealthAllergyEntity
import com.rafaelswitala.mediguard.data.local.entity.HealthContactEntity
import com.rafaelswitala.mediguard.data.local.entity.HealthConditionEntity
import com.rafaelswitala.mediguard.data.local.entity.HealthProfileEntity
import com.rafaelswitala.mediguard.data.local.entity.MedicalDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthProfileDao {
    @Query("SELECT * FROM health_profile WHERE id = 1")
    fun observeProfile(): Flow<HealthProfileEntity?>

    @Query("SELECT * FROM health_profile WHERE id = 1")
    suspend fun getProfile(): HealthProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: HealthProfileEntity)

    @Query("DELETE FROM health_profile")
    suspend fun clear()
}

@Dao
interface HealthAllergyDao {
    @Query("SELECT * FROM health_allergies ORDER BY name ASC")
    fun observeAll(): Flow<List<HealthAllergyEntity>>

    @Insert
    suspend fun insert(allergy: HealthAllergyEntity): Long

    @Update
    suspend fun update(allergy: HealthAllergyEntity)

    @Delete
    suspend fun delete(allergy: HealthAllergyEntity)

    @Query("DELETE FROM health_allergies")
    suspend fun clear()
}

@Dao
interface HealthConditionDao {
    @Query("SELECT * FROM health_conditions ORDER BY type, name COLLATE NOCASE")
    fun observeAll(): Flow<List<HealthConditionEntity>>

    @Insert
    suspend fun insert(condition: HealthConditionEntity): Long

    @Delete
    suspend fun delete(condition: HealthConditionEntity)

    @Query("DELETE FROM health_conditions")
    suspend fun clear()
}

@Dao
interface HealthContactDao {
    @Query("SELECT * FROM health_contacts ORDER BY isEmergency DESC, name ASC")
    fun observeAll(): Flow<List<HealthContactEntity>>

    @Insert
    suspend fun insert(contact: HealthContactEntity): Long

    @Update
    suspend fun update(contact: HealthContactEntity)

    @Delete
    suspend fun delete(contact: HealthContactEntity)

    @Query("DELETE FROM health_contacts")
    suspend fun clear()
}

@Dao
interface MedicalDocumentDao {
    @Query("SELECT * FROM medical_documents ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MedicalDocumentEntity>>

    @Query("SELECT * FROM medical_documents WHERE id = :id")
    suspend fun getById(id: Long): MedicalDocumentEntity?

    @Insert
    suspend fun insert(document: MedicalDocumentEntity): Long

    @Update
    suspend fun update(document: MedicalDocumentEntity)

    @Delete
    suspend fun delete(document: MedicalDocumentEntity)

    @Query("DELETE FROM medical_documents")
    suspend fun clear()
}
