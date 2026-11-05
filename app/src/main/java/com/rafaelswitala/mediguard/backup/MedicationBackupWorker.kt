package com.rafaelswitala.mediguard.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rafaelswitala.mediguard.data.settings.AppPreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class MedicationBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val dependencies = EntryPointAccessors.fromApplication(
            applicationContext,
            BackupWorkerEntryPoint::class.java
        )
        val preferences = dependencies.preferences()
        val settings = preferences.settings.first()
        if (!settings.backupEnabled) return Result.success()
        val period = BackupPeriodCalculator.duePeriod(settings.backupFrequency)
            ?: return Result.success()
        val lastSuccessDate = settings.backupLastSuccessAt?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }
        if (lastSuccessDate == period.to) return Result.success()

        dependencies.exporter().export(
            fromDate = period.from,
            toDate = period.to,
            medicationIds = null,
            fileName = period.fileName
        )
        preferences.setBackupLastSuccessAt(System.currentTimeMillis())
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        const val UNIQUE_WORK_NAME = "medication_pdf_backup"

        fun updateSchedule(context: Context, enabled: Boolean) {
            val workManager = WorkManager.getInstance(context)
            if (!enabled) {
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<MedicationBackupWorker>(12, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackupWorkerEntryPoint {
    fun exporter(): MedicationPdfExporter
    fun preferences(): AppPreferencesRepository
}
