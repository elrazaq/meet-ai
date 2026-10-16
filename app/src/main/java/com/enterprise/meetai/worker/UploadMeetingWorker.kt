package com.enterprise.meetai.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class UploadMeetingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result {
        val filePath = inputData.getString(KEY_AUDIO_PATH) ?: return Result.failure()
        val meetingTitle = inputData.getString(KEY_MEETING_TITLE) ?: "Meeting Internal"
        val serverUrl = inputData.getString(KEY_SERVER_URL) ?: DEFAULT_SERVER_URL

        val audioFile = File(filePath)
        if (!audioFile.exists()) {
            Log.e(TAG, "File audio tidak ditemukan: $filePath")
            return Result.failure()
        }

        return try {
            Log.d(TAG, "Memulai upload audio ${audioFile.name} ke $serverUrl")

            val fileRequestBody = audioFile.asRequestBody("audio/mp4".toMediaTypeOrNull())
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("title", meetingTitle)
                .addFormDataPart("audio_file", audioFile.name, fileRequestBody)
                .build()

            val request = Request.Builder()
                .url("$serverUrl/api/meetings/upload")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d(TAG, "Upload berhasil: $responseBody")
                Result.success(workDataOf(KEY_RESPONSE to responseBody))
            } else {
                Log.w(TAG, "Upload gagal dengan kode HTTP: ${response.code}, menjadwalkan retry...")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Kesalahan jaringan saat upload audio", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "UploadMeetingWorker"
        const val KEY_AUDIO_PATH = "key_audio_path"
        const val KEY_MEETING_TITLE = "key_meeting_title"
        const val KEY_SERVER_URL = "key_server_url"
        const val KEY_RESPONSE = "key_response"

        // Default URL backend jaringan Wi-Fi lokal saat ini
        const val DEFAULT_SERVER_URL = "http://192.168.1.5:8000"
        private const val PREFS_NAME = "meet_ai_settings"
        private const val PREF_KEY_SERVER_URL = "server_url"

        fun getServerUrl(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(PREF_KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        }

        fun saveServerUrl(context: Context, url: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(PREF_KEY_SERVER_URL, url).apply()
        }

        fun enqueueUpload(context: Context, audioFile: File, title: String, serverUrl: String = getServerUrl(context)) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val uploadRequest = OneTimeWorkRequestBuilder<UploadMeetingWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        KEY_AUDIO_PATH to audioFile.absolutePath,
                        KEY_MEETING_TITLE to title,
                        KEY_SERVER_URL to serverUrl
                    )
                )
                .build()

            WorkManager.getInstance(context).enqueue(uploadRequest)
            Log.d(TAG, "WorkManager upload diantrekan untuk ${audioFile.name}")
        }
    }
}
