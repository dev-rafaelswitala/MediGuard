package com.rafaelswitala.mediguard.domain.grouping

import com.rafaelswitala.mediguard.domain.model.FrequencyType
import com.rafaelswitala.mediguard.domain.model.Medication
import com.rafaelswitala.mediguard.domain.model.MedicationSchedule
import com.rafaelswitala.mediguard.domain.model.ScheduleDataCodec

/**
 * Slot-basierte Gruppierung: ein Medikament mit mehreren Uhrzeiten kann in
 * unterschiedlichen Gruppen landen (C@X und C@Y getrennt).
 */
data class ScheduleSignature(
    val frequencyType: FrequencyType,
    val dayMask: Int,
    val triggerMinutes: List<Int>
)

data class GroupingCandidate(
    val medicationA: Medication,
    val medicationB: Medication,
    val scheduleA: MedicationSchedule,
    val scheduleB: MedicationSchedule,
    val minutesA: Int,
    val minutesB: Int
)

object MedicationGroupingService {
    const val MAX_MINUTE_DIFF = 10

    fun signature(schedule: MedicationSchedule): ScheduleSignature {
        val dayMask = when (schedule.scheduleType) {
            FrequencyType.WEEKLY -> {
                ScheduleDataCodec.readDays(schedule.scheduleData)
                    .fold(0) { mask, day -> mask or (1 shl (day.dayNumber - 1)) }
            }
            else -> 0b1111111
        }
        val triggerMinutes = when (schedule.scheduleType) {
            FrequencyType.EXACT_TIME -> listOf(
                ScheduleDataCodec.readInt(schedule.scheduleData, "hour", 8) * 60 +
                    ScheduleDataCodec.readInt(schedule.scheduleData, "minute", 0)
            )
            FrequencyType.WEEKLY -> listOf(
                ScheduleDataCodec.readInt(schedule.scheduleData, "hour", 8) * 60 +
                    ScheduleDataCodec.readInt(schedule.scheduleData, "minute", 0)
            )
            FrequencyType.TIME_RANGE -> listOf(
                ScheduleDataCodec.readInt(schedule.scheduleData, "startHour", 8) * 60 +
                    ScheduleDataCodec.readInt(schedule.scheduleData, "startMinute", 0)
            )
            FrequencyType.INTERVAL -> listOf(
                ScheduleDataCodec.readInt(schedule.scheduleData, "startHour", 8) * 60 +
                    ScheduleDataCodec.readInt(schedule.scheduleData, "startMinute", 0)
            )
            FrequencyType.DAY_PERIOD -> {
                val period = ScheduleDataCodec.readPeriod(schedule.scheduleData)
                listOf(period.preferredHour() * 60)
            }
            FrequencyType.ONE_SHOT -> emptyList()
        }
        return ScheduleSignature(schedule.scheduleType, dayMask, triggerMinutes)
    }

    fun sameDayPattern(sigA: ScheduleSignature, sigB: ScheduleSignature): Boolean {
        if (sigA.frequencyType != sigB.frequencyType) return false
        return when (sigA.frequencyType) {
            FrequencyType.WEEKLY -> sigA.dayMask == sigB.dayMask && sigA.dayMask != 0
            FrequencyType.ONE_SHOT -> false
            else -> true
        }
    }

    fun findCandidates(
        medications: List<Medication>,
        schedulesByMedication: Map<Long, List<MedicationSchedule>>
    ): List<GroupingCandidate> {
        val results = mutableListOf<GroupingCandidate>()
        val pairs = medications.flatMap { medA ->
            medications.filter { it.id != medA.id }.map { medB -> medA to medB }
        }.distinctBy { (a, b) -> minOf(a.id, b.id) to maxOf(a.id, b.id) }

        pairs.forEach { (medA, medB) ->
            val schedulesA = schedulesByMedication[medA.id].orEmpty()
            val schedulesB = schedulesByMedication[medB.id].orEmpty()
            schedulesA.forEach { schedA ->
                schedulesB.forEach scheduleBLoop@{ schedB ->
                    // Already grouped on the same slot → skip
                    if (schedA.intakeGroupId != null &&
                        schedA.intakeGroupId == schedB.intakeGroupId
                    ) {
                        return@scheduleBLoop
                    }
                    val sigA = signature(schedA)
                    val sigB = signature(schedB)
                    if (!sameDayPattern(sigA, sigB)) return@scheduleBLoop
                    sigA.triggerMinutes.forEach { minA ->
                        sigB.triggerMinutes.forEach { minB ->
                            val diff = kotlin.math.abs(minA - minB)
                            if (diff <= MAX_MINUTE_DIFF) {
                                results += GroupingCandidate(medA, medB, schedA, schedB, minA, minB)
                            }
                        }
                    }
                }
            }
        }
        return results.distinctBy {
            "${it.scheduleA.id}-${it.scheduleB.id}-${it.minutesA}-${it.minutesB}"
        }
    }

    fun nextAvailableGroupId(schedules: List<MedicationSchedule>): Int {
        val used = schedules.mapNotNull { it.intakeGroupId }.toSet()
        for (id in 1..6) {
            if (id !in used) return id
        }
        return 1
    }

    fun cleanupGroups(schedules: List<MedicationSchedule>): List<Pair<Long, Int?>> {
        val byGroup = schedules.filter { it.intakeGroupId != null }.groupBy { it.intakeGroupId }
        val updates = mutableListOf<Pair<Long, Int?>>()
        byGroup.forEach { (_, groupSchedules) ->
            if (groupSchedules.size <= 1) {
                groupSchedules.forEach { updates += it.id to null }
            }
        }
        return updates
    }
}
