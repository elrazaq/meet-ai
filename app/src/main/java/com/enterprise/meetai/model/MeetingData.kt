package com.enterprise.meetai.model

import java.io.File

enum class ProcessingStatus {
    RECORDED_LOCAL,  // Belum diunggah
    UPLOADING,       // Sedang diunggah via WorkManager
    AI_PROCESSING,   // Sedang diproses Vertex AI / Gemini
    COMPLETED,       // 4 File Dokumen AI Siap Diunduh/Dibaca
    FAILED           // Terjadi kendala jaringan/koneksi
}

/**
 * 4 File Dokumen Hasil Ekstraksi AI Gemini
 */
data class MeetingAiDocuments(
    val notulenUrl: String? = null,           // 1. Notulen Rapat Formal
    val ringkasanUrl: String? = null,         // 2. Ringkasan Eksekutif (1-2 hal)
    val poinPentingUrl: String? = null,       // 3. Poin-Poin & Data Kunci
    val tanyaJawabUrl: String? = null,        // 4. Tanya-Jawab & Anti-Plot-Hole
    val notulenContent: String? = null,
    val ringkasanContent: String? = null,
    val poinPentingContent: String? = null,
    val tanyaJawabContent: String? = null
)

data class MeetingItem(
    val id: String,
    val title: String,
    val dateFormatted: String,
    val durationFormatted: String,
    val localAudioFile: File? = null,
    val fileSizeFormatted: String = "",
    val status: ProcessingStatus = ProcessingStatus.RECORDED_LOCAL,
    val documents: MeetingAiDocuments = MeetingAiDocuments(),
    val summarySnippet: String = "Menunggu analisis AI untuk menghasilkan 4 dokumen rapat...",
    val isUploadedFile: Boolean = false,
    val durationSeconds: Long = 0L
)

data class ScheduledMeeting(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val dateEpochMillis: Long,
    val formattedDateTime: String,
    val agendaNotes: String = ""
)
