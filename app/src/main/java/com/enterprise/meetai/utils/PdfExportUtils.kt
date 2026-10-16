package com.enterprise.meetai.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.enterprise.meetai.model.MeetingItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportUtils {

    private const val TAG = "PdfExportUtils"

    fun exportAndShareCorporatePdf(context: Context, meeting: MeetingItem) {
        exportAndSharePdf(context, meeting, null)
    }

    /**
     * Menghasilkan dokumen PDF formal dari 4 hasil ekstraksi AI dan membuka menu Share Android
     */
    fun exportAndSharePdf(context: Context, meeting: MeetingItem, specificDocType: String? = null) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 standard width in points (72 dpi)
            val pageHeight = 842 // A4 standard height in points
            val margin = 40f
            val contentWidth = pageWidth - (margin * 2)

            // Setup Paints
            val titlePaint = Paint().apply {
                color = Color.rgb(15, 23, 42) // Executive Deep Slate
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(2, 132, 199) // Executive Teal
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val sectionHeaderPaint = Paint().apply {
                color = Color.rgb(30, 27, 75) // Dark Navy
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val bodyPaint = Paint().apply {
                color = Color.rgb(30, 41, 59) // Slate Dark
                textSize = 9.5f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                strokeWidth = 1f
            }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas: Canvas = page.canvas
            var yPos = margin + 15f

            fun checkNewPage(neededHeight: Float) {
                if (yPos + neededHeight > pageHeight - margin) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = margin + 15f
                }
            }

            // Header Dokumen Resmi
            canvas.drawText("MEETAI - DOKUMEN LAPORAN RAPAT RESMI", margin, yPos, titlePaint)
            yPos += 18f
            canvas.drawText("Agenda: ${meeting.title}", margin, yPos, subtitlePaint)
            yPos += 14f
            canvas.drawText("Tanggal: ${meeting.dateFormatted} | Durasi: ${meeting.durationFormatted}", margin, yPos, subtitlePaint)
            yPos += 14f
            canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
            yPos += 20f

            val docsToPrint = if (specificDocType != null) {
                listOf(specificDocType to getDocContent(meeting, specificDocType))
            } else {
                listOf(
                    "1. NOTULEN RAPAT FORMAL" to (meeting.documents.notulenContent ?: "Tidak ada data"),
                    "2. RINGKASAN EKSEKUTIF" to (meeting.documents.ringkasanContent ?: "Tidak ada data"),
                    "3. POIN-POIN PENTING & DATA KUNCI" to (meeting.documents.poinPentingContent ?: "Tidak ada data"),
                    "4. REKAP TANYA-JAWAB & ANTI-PLOT-HOLE" to (meeting.documents.tanyaJawabContent ?: "Tidak ada data")
                )
            }

            for ((sectionTitle, rawContent) in docsToPrint) {
                checkNewPage(40f)
                canvas.drawText(sectionTitle, margin, yPos, sectionHeaderPaint)
                yPos += 16f
                canvas.drawLine(margin, yPos, margin + 200f, yPos, linePaint)
                yPos += 14f

                // Format baris teks dari Markdown ke PDF
                val lines = rawContent.split("\n")
                for (rawLine in lines) {
                    val line = rawLine.trim()
                    if (line.isBlank()) {
                        yPos += 6f
                        continue
                    }

                    val isSubHeader = line.startsWith("#")
                    val isBullet = line.startsWith("- ") || (line.startsWith("* ") && !line.startsWith("**"))
                    val cleanText = line.replace("#", "").replace("**", "").replace("*", "").trim()

                    val currentPaint = if (isSubHeader) sectionHeaderPaint else bodyPaint
                    val lineHeight = if (isSubHeader) 16f else 12f

                    val wrappedLines = wrapText(cleanText, contentWidth, currentPaint)
                    for (wrapped in wrappedLines) {
                        checkNewPage(lineHeight)
                        val xOffset = if (isBullet) margin + 10f else margin
                        val prefix = if (isBullet) "• " else ""
                        canvas.drawText(prefix + wrapped, xOffset, yPos, currentPaint)
                        yPos += lineHeight
                    }
                }
                yPos += 16f
            }

            pdfDocument.finishPage(page)

            // Simpan ke Cache internal dan bagikan lewat FileProvider
            val sanitized = meeting.title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val fileName = "Laporan_Rapat_${sanitized}_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.pdf"
            val outputFile = File(context.cacheDir, fileName)

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            Log.d(TAG, "File PDF berhasil dibuat: ${outputFile.absolutePath} (${outputFile.length()} bytes)")

            // Buka Share Sheet Android
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Rapat: ${meeting.title}")
                putExtra(Intent.EXTRA_TEXT, "Terlampir dokumen resmi hasil ekstraksi AI MeetAI untuk rapat: ${meeting.title}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Dokumen PDF Rapat"))

        } catch (e: Exception) {
            Log.e(TAG, "Gagal membuat atau membagikan PDF", e)
            Toast.makeText(context, "Gagal membuat PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getDocContent(meeting: MeetingItem, type: String): String {
        return when (type) {
            "notulen" -> meeting.documents.notulenContent ?: ""
            "ringkasan" -> meeting.documents.ringkasanContent ?: ""
            "poin" -> meeting.documents.poinPentingContent ?: ""
            "tanya_jawab" -> meeting.documents.tanyaJawabContent ?: ""
            else -> ""
        }
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            if (width <= maxWidth) {
                currentLine.append(if (currentLine.isEmpty()) "" else " ").append(word)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return if (lines.isEmpty()) listOf(text) else lines
    }
}
