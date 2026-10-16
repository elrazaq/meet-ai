package com.enterprise.meetai.data

import android.content.Context
import android.util.Log
import com.enterprise.meetai.model.MeetingAiDocuments
import com.enterprise.meetai.model.MeetingItem
import com.enterprise.meetai.model.ProcessingStatus
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MeetingRepository(private val context: Context) {

    private val storageFile: File
        get() = File(context.filesDir, "meetings_history.json")

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
                        summarySnippet = obj.optString("summary_snippet", "")
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

    companion object {
        private const val TAG = "MeetingRepository"
    }
}
