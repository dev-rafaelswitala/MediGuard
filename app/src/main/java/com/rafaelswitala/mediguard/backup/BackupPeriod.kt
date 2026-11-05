package com.rafaelswitala.mediguard.backup

import com.rafaelswitala.mediguard.data.settings.BackupFrequency
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class BackupPeriod(
    val from: LocalDate,
    val to: LocalDate,
    val fileName: String
)

object BackupPeriodCalculator {
    fun duePeriod(frequency: BackupFrequency, today: LocalDate = LocalDate.now()): BackupPeriod? {
        return when (frequency) {
            BackupFrequency.NONE -> null
            BackupFrequency.WEEKLY -> {
                if (today.dayOfWeek != DayOfWeek.SUNDAY) return null
                val from = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                BackupPeriod(
                    from,
                    today,
                    "Medikation_Woche_${from.format(DateTimeFormatter.ISO_DATE)}.pdf"
                )
            }
            BackupFrequency.MONTHLY -> {
                val month = YearMonth.from(today)
                if (today != month.atEndOfMonth()) return null
                BackupPeriod(
                    month.atDay(1),
                    today,
                    "Medikation_Monat_${month}.pdf"
                )
            }
            BackupFrequency.QUARTERLY -> {
                if (today.monthValue !in setOf(3, 6, 9, 12) ||
                    today != YearMonth.from(today).atEndOfMonth()
                ) return null
                val startMonth = today.monthValue - 2
                val from = LocalDate.of(today.year, startMonth, 1)
                BackupPeriod(
                    from,
                    today,
                    "Medikation_Quartal_${monthShort(startMonth)}-${monthShort(today.monthValue)}_${today.year}.pdf"
                )
            }
            BackupFrequency.SEMIANNUAL -> {
                if (today.monthValue !in setOf(6, 12) ||
                    today != YearMonth.from(today).atEndOfMonth()
                ) return null
                val startMonth = if (today.monthValue == 6) 1 else 7
                val from = LocalDate.of(today.year, startMonth, 1)
                BackupPeriod(
                    from,
                    today,
                    "Medikation_Halbjahr_${monthShort(startMonth)}-${monthShort(today.monthValue)}_${today.year}.pdf"
                )
            }
        }
    }

    private fun monthShort(month: Int): String = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )[month - 1]
}
