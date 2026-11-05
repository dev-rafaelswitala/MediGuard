package com.rafaelswitala.mediguard.backup

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.rafaelswitala.mediguard.domain.model.IntakeHistory
import com.rafaelswitala.mediguard.domain.repository.IntakeHistoryRepository
import com.rafaelswitala.mediguard.domain.repository.ProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationPdfExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intakeHistoryRepository: IntakeHistoryRepository,
    private val profileRepository: ProfileRepository
) {
    suspend fun export(
        fromDate: LocalDate,
        toDate: LocalDate,
        medicationIds: Set<Long>? = null,
        fileName: String = manualFileName(fromDate)
    ): File = withContext(Dispatchers.IO) {
        require(!toDate.isBefore(fromDate)) { "End date must not be before start date" }
        val zone = ZoneId.systemDefault()
        val from = fromDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = toDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val rows = intakeHistoryRepository.getIntakeHistoryInRange(from, to).first()
            .filter { medicationIds == null || it.medicationId in medicationIds }
            .sortedBy { it.scheduledTime }

        val directory = File(context.filesDir, "documents").also { it.mkdirs() }
        val file = File(directory, fileName)
        runCatching { writePdf(file, fromDate, toDate, rows) }
            .onFailure { file.delete() }
            .getOrThrow()
        profileRepository.insertGeneratedDocument(
            file = file,
            title = file.nameWithoutExtension,
            documentDateMillis = System.currentTimeMillis()
        )
        file
    }

    private fun writePdf(
        file: File,
        fromDate: LocalDate,
        toDate: LocalDate,
        rows: List<IntakeHistory>
    ) {
        val document = PdfDocument()
        val titlePaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply { textSize = 10f }
        val headingPaint = Paint(bodyPaint).apply { isFakeBoldText = true }
        val dateTime = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun newPage() {
            page?.let(document::finishPage)
            pageNumber++
            page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            val canvas = page!!.canvas
            canvas.drawText("MediGuard – Medication intake history", MARGIN, 38f, titlePaint)
            canvas.drawText(
                "${fromDate.format(DATE)} – ${toDate.format(DATE)}",
                MARGIN,
                58f,
                bodyPaint
            )
            canvas.drawText(
                "Scheduled | Status | Medication | Notes",
                MARGIN,
                82f,
                headingPaint
            )
            y = 102f
        }

        newPage()
        if (rows.isEmpty()) {
            page!!.canvas.drawText("No intake history in this period.", MARGIN, y, bodyPaint)
        } else {
            rows.forEach { row ->
                if (y > PAGE_HEIGHT - 45f) newPage()
                val time = Instant.ofEpochMilli(row.scheduledTime).atZone(ZoneId.systemDefault())
                    .format(dateTime)
                val text = "$time | ${row.status.name} | ${row.medicationName} | ${row.note.orEmpty()}"
                wrap(text, MAX_CHARS).forEach { line ->
                    if (y > PAGE_HEIGHT - 45f) newPage()
                    page!!.canvas.drawText(line, MARGIN, y, bodyPaint)
                    y += 14f
                }
                y += 5f
            }
        }
        page?.let(document::finishPage)
        file.outputStream().use(document::writeTo)
        document.close()
    }

    private fun wrap(text: String, maxLength: Int): List<String> {
        if (text.length <= maxLength) return listOf(text)
        val result = mutableListOf<String>()
        var remaining = text
        while (remaining.length > maxLength) {
            val split = remaining.lastIndexOf(' ', maxLength).takeIf { it > 0 } ?: maxLength
            result += remaining.substring(0, split)
            remaining = remaining.substring(split).trimStart()
        }
        if (remaining.isNotEmpty()) result += remaining
        return result
    }

    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN = 35f
        private const val MAX_CHARS = 90
        private val DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private val FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")

        fun manualFileName(fromDate: LocalDate, now: java.time.LocalDateTime = java.time.LocalDateTime.now()) =
            "Medikation_Custom_${fromDate.format(FILE_DATE)}_${now.format(FILE_STAMP)}.pdf"
    }
}
