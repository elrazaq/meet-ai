package com.enterprise.meetai.data

import android.content.Context
import android.util.Log
import com.enterprise.meetai.model.MeetingAiDocuments
import com.enterprise.meetai.model.MeetingItem
import com.enterprise.meetai.model.ProcessingStatus
import com.enterprise.meetai.model.ScheduledMeeting
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MeetingRepository(private val context: Context) {

    private val storageFile: File
        get() = File(context.filesDir, "meetings_history.json")

    private val schedulesFile: File
        get() = File(context.filesDir, "schedules_history.json")

    fun loadMeetings(): List<MeetingItem> {
        val file = storageFile
        if (!file.exists()) {
            return emptyList()
        }

        return try {
            val jsonString = file.readText()
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<MeetingItem>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val audioPath = obj.optString("audio_path", "")
                val audioFile = if (audioPath.isNotBlank()) File(audioPath) else null

                val docsObj = obj.optJSONObject("documents")
                val docs = if (docsObj != null) {
                    MeetingAiDocuments(
                        notulenContent = docsObj.optString("notulen"),
                        ringkasanContent = docsObj.optString("ringkasan"),
                        poinPentingContent = docsObj.optString("poin_penting"),
                        tanyaJawabContent = docsObj.optString("tanya_jawab")
                    )
                } else MeetingAiDocuments()

                val statusStr = obj.optString("status", ProcessingStatus.RECORDED_LOCAL.name)
                val status = try {
                    ProcessingStatus.valueOf(statusStr)
                } catch (e: Exception) {
                    ProcessingStatus.RECORDED_LOCAL
                }

                list.add(
                    MeetingItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        dateFormatted = obj.optString("date_formatted", ""),
                        durationFormatted = obj.optString("duration_formatted", ""),
                        localAudioFile = audioFile,
                        fileSizeFormatted = obj.optString("file_size_formatted", ""),
                        status = status,
                        documents = docs,
                        summarySnippet = obj.optString("summary_snippet", ""),
                        isUploadedFile = obj.optBoolean("is_uploaded_file", false),
                        durationSeconds = obj.optLong("duration_seconds", 0L)
                    )
                )
            }
            Log.d(TAG, "Berhasil memuat ${list.size} riwayat rapat dari penyimpanan lokal.")
            list
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membaca file riwayat rapat", e)
            emptyList()
        }
    }

    fun saveMeetings(meetings: List<MeetingItem>) {
        try {
            val jsonArray = JSONArray()
            for (item in meetings) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("date_formatted", item.dateFormatted)
                    put("duration_formatted", item.durationFormatted)
                    put("audio_path", item.localAudioFile?.absolutePath ?: "")
                    put("file_size_formatted", item.fileSizeFormatted)
                    put("status", item.status.name)
                    put("summary_snippet", item.summarySnippet)
                    put("is_uploaded_file", item.isUploadedFile)
                    put("duration_seconds", item.durationSeconds)

                    val docsObj = JSONObject().apply {
                        put("notulen", item.documents.notulenContent ?: "")
                        put("ringkasan", item.documents.ringkasanContent ?: "")
                        put("poin_penting", item.documents.poinPentingContent ?: "")
                        put("tanya_jawab", item.documents.tanyaJawabContent ?: "")
                    }
                    put("documents", docsObj)
                }
                jsonArray.put(obj)
            }

            storageFile.writeText(jsonArray.toString())
            Log.d(TAG, "Riwayat ${meetings.size} rapat berhasil disimpan permanen ke disk.")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menyimpan riwayat rapat ke disk", e)
        }
    }

    fun loadScheduledMeetings(): List<ScheduledMeeting> {
        val file = schedulesFile
        if (!file.exists()) return emptyList()

        return try {
            val jsonString = file.readText()
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<ScheduledMeeting>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ScheduledMeeting(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        dateEpochMillis = obj.getLong("date_epoch_millis"),
                        formattedDateTime = obj.getString("formatted_date_time"),
                        agendaNotes = obj.optString("agenda_notes", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memuat jadwal rapat", e)
            emptyList()
        }
    }

    fun saveScheduledMeetings(schedules: List<ScheduledMeeting>) {
        try {
            val jsonArray = JSONArray()
            for (item in schedules) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("date_epoch_millis", item.dateEpochMillis)
                    put("formatted_date_time", item.formattedDateTime)
                    put("agenda_notes", item.agendaNotes)
                }
                jsonArray.put(obj)
            }
            schedulesFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menyimpan jadwal rapat", e)
        }
    }

    companion object {
        private const val TAG = "MeetingRepository"
    }
}
