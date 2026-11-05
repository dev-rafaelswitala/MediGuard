package com.rafaelswitala.mediguard.alarm

/**
 * Empfängt wiederkehrende Weckrufe und verarbeitet Medikamentengruppen.
 * Triggert Benachrichtigungen und ordnet ähnliche Zeitpläne zu Gruppen.
 */

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rafaelswitala.mediguard.data.datastore.DirectBootAlarmStore
import com.rafaelswitala.mediguard.data.settings.DayPeriodSettingsProvider
import com.rafaelswitala.mediguard.domain.model.IntakeHistory
import com.rafaelswitala.mediguard.domain.model.IntakeStatus
import com.rafaelswitala.mediguard.domain.model.Medication
import com.rafaelswitala.mediguard.domain.model.MedicationSchedule
import com.rafaelswitala.mediguard.domain.reminder.ReminderPlanFactory
import com.rafaelswitala.mediguard.domain.repository.IntakeHistoryRepository
import com.rafaelswitala.mediguard.domain.repository.MedicationRepository
import com.rafaelswitala.mediguard.domain.repository.MedicationScheduleRepository
import com.rafaelswitala.mediguard.notification.MedicationNotificationManager
import com.rafaelswitala.mediguard.notification.ReminderNotificationItem
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Receives reminder alarm broadcasts.
 * Recurring alarms are grouped by schedule.intakeGroupId (slot-level).
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleAlarm(context, intent)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleAlarm(context: Context, intent: Intent) {
        when (intent.action) {
            ReminderActions.ACTION_ONE_TIME_REMINDER -> handleOneTimeReminder(context, intent)
            ReminderActions.ACTION_WINDOW_END -> handleWindowEnd(context, intent)
            else -> handleRecurringReminder(context, intent)
        }
    }

    private suspend fun handleWindowEnd(context: Context, intent: Intent) {
        val historyIds = ReminderActions.idsFromCsv(
            intent.getStringExtra(ReminderActions.EXTRA_HISTORY_IDS)
        )
        val notificationId = intent.getIntExtra(ReminderActions.EXTRA_NOTIFICATION_ID, 0)
        if (historyIds.isEmpty()) return

        val entryPoint = entryPoint(context)
        val toMiss = historyIds.mapNotNull { id ->
            entryPoint.intakeHistoryRepository().getIntakeHistoryById(id)
        }.filter {
            it.status == IntakeStatus.PENDING || it.status == IntakeStatus.SNOOZED
        }.map { it.id }

        if (toMiss.isNotEmpty()) {
            entryPoint.intakeHistoryRepository().markMissed(toMiss)
        }
        if (notificationId != 0) {
            entryPoint.notificationManager().cancelNotification(notificationId)
        }
    }

    private suspend fun handleRecurringReminder(context: Context, intent: Intent) {
        val directBootAlarmStore = DirectBootAlarmStore(context.applicationContext)
        val scheduleId = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_SCHEDULE_ID, -1L)
        val medicationId = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_MEDICATION_ID, -1L)
        val scheduledTime = intent.getLongExtra(
            AndroidAlarmScheduler.EXTRA_SCHEDULED_TIME,
            System.currentTimeMillis()
        )

        if (scheduleId <= 0L || medicationId <= 0L) return

        val storedAlarm = directBootAlarmStore.getAlarmSchedule(scheduleId)
        if (storedAlarm != null && storedAlarm.nextTriggerAtMillis > scheduledTime + GROUP_WINDOW_MS) {
            return
        }

        val dueAlarms = directBootAlarmStore.getAllAlarmSchedules()
            .first()
            .filter { abs(it.nextTriggerAtMillis - scheduledTime) <= GROUP_WINDOW_MS }
            .ifEmpty {
                listOf(
                    storedAlarm ?: DirectBootAlarmStore.AlarmScheduleData(
                        scheduleId = scheduleId,
                        medicationId = medicationId,
                        nextTriggerAtMillis = scheduledTime,
                        scheduleType = "",
                        scheduleData = "",
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }

        if (!isCredentialStorageUnlocked(context)) {
            val leaderScheduleId = dueAlarms.minOf { it.scheduleId }
            if (scheduleId != leaderScheduleId) return

            val notificationId = leaderScheduleId.toInt()
            directBootAlarmStore.savePendingLockedAlarm(
                notificationId = leaderScheduleId,
                scheduledTime = scheduledTime,
                scheduleIds = dueAlarms.map { it.scheduleId }
            )
            DirectBootReminderNotifier.showPrivacyReminder(
                context = context,
                directBootAlarmStore = directBootAlarmStore,
                notificationId = notificationId,
                scheduledTime = scheduledTime
            )
            dueAlarms.forEach { alarmData ->
                if (alarmData.scheduleType.isNotBlank()) {
                    DirectBootAlarmScheduler.scheduleStoredAlarm(
                        context = context,
                        directBootAlarmStore = directBootAlarmStore,
                        alarmData = alarmData
                    )
                }
            }
        } else {
            val entryPoint = entryPoint(context)
            val dueMedicationAlarms = dueAlarms.mapNotNull { alarmData ->
                val medication = entryPoint.medicationRepository().getMedicationById(alarmData.medicationId)
                    ?: return@mapNotNull null
                val schedule = entryPoint.scheduleRepository().getScheduleById(alarmData.scheduleId)
                DueMedicationAlarm(alarmData, medication, schedule)
            }
            val currentAlarm = dueMedicationAlarms.firstOrNull { it.alarmData.scheduleId == scheduleId }
                ?: return
            val currentGroup = currentAlarm.schedule?.intakeGroupId
            val notificationAlarms = if (currentGroup == null) {
                listOf(currentAlarm)
            } else {
                dueMedicationAlarms.filter { it.schedule?.intakeGroupId == currentGroup }
            }

            val leaderScheduleId = notificationAlarms.minOf { it.alarmData.scheduleId }
            if (scheduleId != leaderScheduleId) return

            val notificationId = leaderScheduleId.toInt()
            showUnlockedNotification(
                entryPoint = entryPoint,
                dueAlarms = notificationAlarms,
                scheduledTime = scheduledTime,
                notificationId = notificationId
            )

            notificationAlarms.forEach { dueMedicationAlarm ->
                val alarmData = dueMedicationAlarm.alarmData
                if (alarmData.scheduleType.isNotBlank()) {
                    entryPoint.alarmScheduler().scheduleStoredAlarm(alarmData)
                }
            }
        }
    }

    private suspend fun handleOneTimeReminder(context: Context, intent: Intent) {
        val directBootAlarmStore = DirectBootAlarmStore(context.applicationContext)
        val scheduleIds = ReminderActions.idsFromCsv(
            intent.getStringExtra(ReminderActions.EXTRA_SCHEDULE_IDS)
        )
        val historyIds = ReminderActions.idsFromCsv(
            intent.getStringExtra(ReminderActions.EXTRA_HISTORY_IDS)
        )
        val notificationId = intent.getIntExtra(
            ReminderActions.EXTRA_NOTIFICATION_ID,
            scheduleIds.minOrNull()?.toInt() ?: 0
        )
        val scheduledTime = intent.getLongExtra(
            ReminderActions.EXTRA_SCHEDULED_TIME,
            System.currentTimeMillis()
        )

        if (historyIds.isEmpty()) return

        if (!isCredentialStorageUnlocked(context)) {
            DirectBootReminderNotifier.showPrivacyReminder(
                context = context,
                directBootAlarmStore = directBootAlarmStore,
                notificationId = notificationId,
                scheduledTime = scheduledTime
            )
            return
        }

        val entryPoint = entryPoint(context)
        val histories = historyIds
            .mapNotNull { historyId -> entryPoint.intakeHistoryRepository().getIntakeHistoryById(historyId) }
            .filterNot { it.isTaken() }

        if (histories.isEmpty()) {
            entryPoint.notificationManager().cancelNotification(notificationId)
            return
        }

        val items = histories.map { history ->
            ReminderNotificationItem(
                medicationId = history.medicationId,
                historyId = history.id,
                medicationName = history.medicationName,
                dosage = history.dosage,
                dosageUnit = history.dosageUnit
            )
        }

        entryPoint.notificationManager().showMedicationGroupNotification(
            scheduleIds = scheduleIds,
            scheduledTime = scheduledTime,
            items = items,
            notificationId = notificationId
        )

        // Tageszeit-Nachfass: nach Mitte ggf. Ende als nächste Erinnerung planen
        val originalStart = histories.minOf { it.scheduledTime }
        scheduleNextDayPeriodFollowUp(
            entryPoint = entryPoint,
            scheduleIds = scheduleIds,
            historyIds = histories.map { it.id },
            notificationId = notificationId,
            periodStartMillis = originalStart,
            afterMillis = scheduledTime
        )
    }

    private suspend fun scheduleNextDayPeriodFollowUp(
        entryPoint: AlarmReceiverEntryPoint,
        scheduleIds: List<Long>,
        historyIds: List<Long>,
        notificationId: Int,
        periodStartMillis: Long,
        afterMillis: Long
    ) {
        val now = System.currentTimeMillis()
        val daySettings = entryPoint.dayPeriodSettingsProvider().current()
        val schedule = scheduleIds.firstOrNull()?.let {
            entryPoint.scheduleRepository().getScheduleById(it)
        } ?: return
        val anchors = ReminderPlanFactory.dayPeriodAnchors(
            schedule = schedule,
            scheduledStartMillis = periodStartMillis,
            dayPeriodSettings = daySettings
        ) ?: return
        val next = listOf(anchors.mid, anchors.end)
            .firstOrNull { it > afterMillis + 1_000L && it > now }
            ?: return
        entryPoint.alarmScheduler().scheduleOneTimeReminder(
            triggerAtMillis = next,
            scheduleIds = scheduleIds,
            historyIds = historyIds,
            notificationId = notificationId
        )
    }

    private suspend fun showUnlockedNotification(
        entryPoint: AlarmReceiverEntryPoint,
        dueAlarms: List<DueMedicationAlarm>,
        scheduledTime: Long,
        notificationId: Int
    ) {
        val daySettings = entryPoint.dayPeriodSettingsProvider().current()
        val historyIds = mutableListOf<Long>()
        var windowEnd: Long? = null

        val items = dueAlarms.map { dueMedicationAlarm ->
            val medication = dueMedicationAlarm.medication
            val schedule = dueMedicationAlarm.schedule
            val scheduleWindowEnd = schedule?.let {
                ReminderPlanFactory.windowEndMillis(it, scheduledTime, daySettings)
            }
            if (scheduleWindowEnd != null) {
                windowEnd = listOfNotNull(windowEnd, scheduleWindowEnd).minOrNull()
            }

            val historyId = entryPoint.intakeHistoryRepository().recordIntake(
                IntakeHistory(
                    medicationId = medication.id,
                    scheduleId = schedule?.id ?: dueMedicationAlarm.alarmData.scheduleId,
                    scheduledTime = scheduledTime,
                    windowEndTime = scheduleWindowEnd,
                    dosage = medication.dosage,
                    dosageUnit = medication.dosageUnit,
                    medicationName = medication.name,
                    status = IntakeStatus.PENDING,
                    notificationId = notificationId
                )
            )
            historyIds += historyId

            ReminderNotificationItem(
                medicationId = medication.id,
                historyId = historyId,
                medicationName = medication.name,
                dosage = medication.dosage,
                dosageUnit = medication.dosageUnit
            )
        }

        if (items.isEmpty()) {
            entryPoint.notificationManager().showPrivacyMedicationNotification(notificationId.toLong())
            return
        }

        val now = System.currentTimeMillis()
        val effectiveWindowEnd = windowEnd
        if (effectiveWindowEnd != null && effectiveWindowEnd <= now) {
            entryPoint.intakeHistoryRepository().markMissed(historyIds)
            entryPoint.notificationManager().cancelNotification(notificationId)
            return
        }

        entryPoint.notificationManager().showMedicationGroupNotification(
            scheduleIds = dueAlarms.map { it.alarmData.scheduleId },
            scheduledTime = scheduledTime,
            items = items,
            notificationId = notificationId
        )

        entryPoint.intakeHistoryRepository().setNotificationId(historyIds, notificationId)

        // Tageszeit: Start-Benachrichtigung, danach Mitte/Ende als verkettete One-Time-Erinnerungen.
        // Nach Fensterende (+ kurze Nachlaufzeit) wird als verpasst markiert.
        val dayPeriodSchedule = dueAlarms.firstOrNull()?.schedule
        val anchors = dayPeriodSchedule?.let {
            ReminderPlanFactory.dayPeriodAnchors(it, scheduledTime, daySettings)
        }
        if (anchors != null) {
            val nextFollowUp = listOf(anchors.mid, anchors.end).firstOrNull { it > now }
            if (nextFollowUp != null) {
                entryPoint.alarmScheduler().scheduleOneTimeReminder(
                    triggerAtMillis = nextFollowUp,
                    scheduleIds = dueAlarms.map { it.alarmData.scheduleId },
                    historyIds = historyIds,
                    notificationId = notificationId
                )
            }
            val missAt = anchors.end + DAY_PERIOD_MISS_GRACE_MS
            if (missAt > now) {
                entryPoint.alarmScheduler().scheduleWindowEnd(
                    triggerAtMillis = missAt,
                    historyIds = historyIds,
                    notificationId = notificationId
                )
            }
        } else if (effectiveWindowEnd != null && effectiveWindowEnd > now) {
            entryPoint.alarmScheduler().scheduleWindowEnd(
                triggerAtMillis = effectiveWindowEnd,
                historyIds = historyIds,
                notificationId = notificationId
            )
        }
    }

    private fun entryPoint(context: Context): AlarmReceiverEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AlarmReceiverEntryPoint::class.java
        )

    private fun isCredentialStorageUnlocked(context: Context): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) return true
        val userManager = context.getSystemService(Context.USER_SERVICE) as android.os.UserManager
        return userManager.isUserUnlocked
    }

    companion object {
        private const val GROUP_WINDOW_MS = 10 * 60_000L
        private const val DAY_PERIOD_MISS_GRACE_MS = 60_000L
    }
}

private data class DueMedicationAlarm(
    val alarmData: DirectBootAlarmStore.AlarmScheduleData,
    val medication: Medication,
    val schedule: MedicationSchedule?
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AlarmReceiverEntryPoint {
    fun alarmScheduler(): AlarmScheduler
    fun medicationRepository(): MedicationRepository
    fun scheduleRepository(): MedicationScheduleRepository
    fun intakeHistoryRepository(): IntakeHistoryRepository
    fun notificationManager(): MedicationNotificationManager
    fun dayPeriodSettingsProvider(): DayPeriodSettingsProvider
}
