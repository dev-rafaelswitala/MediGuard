package com.rafaelswitala.mediguard.domain.model

/**
 * Zeitplan eines Medikaments. intakeGroupId gilt pro Slot (Schedule), nicht pauschal pro Medikament.
 */
data class MedicationSchedule(
    val id: Long = 0,
    val medicationId: Long,
    val scheduleType: FrequencyType,
    val scheduleData: String,
    val reminderMinutes: Int = 15,
    val snoozeOptions: List<Int> = listOf(5, 10, 15, 30, 60),
    val isEnabled: Boolean = true,
    val intakeGroupId: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class ExactTimeSchedule(
    val hour: Int,
    val minute: Int
)

data class TimeRangeSchedule(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
)

data class DayPeriodSchedule(
    val period: DayPeriod
)

data class WeeklySchedule(
    val daysOfWeek: List<DayOfWeekEnum>
)

data class IntervalSchedule(
    val intervalHours: Int,
    val startHour: Int,
    val startMinute: Int
)

data class OneShotSchedule(
    val triggerAtMillis: Long
)
