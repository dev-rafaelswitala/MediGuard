package com.rafaelswitala.mediguard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rafaelswitala.mediguard.data.local.dao.HealthAllergyDao
import com.rafaelswitala.mediguard.data.local.dao.HealthContactDao
import com.rafaelswitala.mediguard.data.local.dao.HealthConditionDao
import com.rafaelswitala.mediguard.data.local.dao.HealthProfileDao
import com.rafaelswitala.mediguard.data.local.dao.IntakeHistoryDao
import com.rafaelswitala.mediguard.data.local.dao.MedicalDocumentDao
import com.rafaelswitala.mediguard.data.local.dao.MedicationDao
import com.rafaelswitala.mediguard.data.local.dao.MedicationScheduleDao
import com.rafaelswitala.mediguard.data.local.entity.HealthAllergyEntity
import com.rafaelswitala.mediguard.data.local.entity.HealthContactEntity
import com.rafaelswitala.mediguard.data.local.entity.HealthConditionEntity
import com.rafaelswitala.mediguard.data.local.entity.HealthProfileEntity
import com.rafaelswitala.mediguard.data.local.entity.IntakeHistoryEntity
import com.rafaelswitala.mediguard.data.local.entity.MedicalDocumentEntity
import com.rafaelswitala.mediguard.data.local.entity.MedicationEntity
import com.rafaelswitala.mediguard.data.local.entity.MedicationScheduleEntity

/**
 * Lokale Room-Datenbank V8: Medikamente, Zeitpläne, Verlauf und Profilakte.
 * Profil-Schemawechsel werden bewusst destruktiv migriert.
 */
@Database(
    entities = [
        MedicationEntity::class,
        MedicationScheduleEntity::class,
        IntakeHistoryEntity::class,
        HealthProfileEntity::class,
        HealthAllergyEntity::class,
        HealthContactEntity::class,
        HealthConditionEntity::class,
        MedicalDocumentEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationScheduleDao(): MedicationScheduleDao
    abstract fun intakeHistoryDao(): IntakeHistoryDao
    abstract fun healthProfileDao(): HealthProfileDao
    abstract fun healthAllergyDao(): HealthAllergyDao
    abstract fun healthContactDao(): HealthContactDao
    abstract fun healthConditionDao(): HealthConditionDao
    abstract fun medicalDocumentDao(): MedicalDocumentDao

    companion object {
        const val DATABASE_NAME = "mediguard_db"
    }
}
