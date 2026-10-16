package com.enterprise.meetai.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.meetai.data.MeetingRepository
import com.enterprise.meetai.model.MeetingAiDocuments
import com.enterprise.meetai.model.MeetingItem
import com.enterprise.meetai.model.ProcessingStatus
import com.enterprise.meetai.model.ScheduledMeeting
import com.enterprise.meetai.recorder.RecordingService
import com.enterprise.meetai.worker.UploadMeetingWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed class PingState {
    object Idle : PingState()
    object Pinging : PingState()
    data class Success(val message: String) : PingState()
    data class Error(val message: String) : PingState()
}

class MeetingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MeetingRepository(application)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private var recordingService: RecordingService? = null
    private var isBound = false

    private val _isServiceRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isServiceRecording.asStateFlow()

    private val _isServicePaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isServicePaused.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0)
    val currentAmplitude: StateFlow<Int> = _currentAmplitude.asStateFlow()

    private val _meetingTitle = MutableStateFlow("Meeting Internal")
    val meetingTitle: StateFlow<String> = _meetingTitle.asStateFlow()

    private val _showDisclosure = MutableStateFlow(false)
    val showDisclosure: StateFlow<Boolean> = _showDisclosure.asStateFlow()

    private val _meetings = MutableStateFlow<List<MeetingItem>>(emptyList())
    val meetings: StateFlow<List<MeetingItem>> = _meetings.asStateFlow()

    private val _scheduledMeetings = MutableStateFlow<List<ScheduledMeeting>>(emptyList())
    val scheduledMeetings: StateFlow<List<ScheduledMeeting>> = _scheduledMeetings.asStateFlow()

    private val _selectedMeetingForDocs = MutableStateFlow<MeetingItem?>(null)
    val selectedMeetingForDocs: StateFlow<MeetingItem?> = _selectedMeetingForDocs.asStateFlow()

    private val _pingState = MutableStateFlow<PingState>(PingState.Idle)
    val pingState: StateFlow<PingState> = _pingState.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? RecordingService.LocalBinder
            recordingService = localBinder?.getService()
            isBound = true

            recordingService?.let { service ->
                viewModelScope.launch {
                    service.isRecording.collect { recording ->
                        _isServiceRecording.value = recording
                    }
                }
                viewModelScope.launch {
                    service.isPaused.collect { paused ->
                        _isServicePaused.value = paused
                    }
                }
                viewModelScope.launch {
                    service.elapsedSeconds.collect { seconds ->
                        _elapsedSeconds.value = seconds
                    }
                }
                viewModelScope.launch {
                    service.currentAmplitude.collect { amp ->
                        _currentAmplitude.value = amp
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            recordingService = null
            isBound = false
        }
    }

    init {
        bindRecordingService()
        loadSampleAndSavedRecordings()
        loadSavedSchedules()
    }

    private fun bindRecordingService() {
        val intent = Intent(getApplication(), RecordingService::class.java)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun setShowDisclosure(show: Boolean) {
        _showDisclosure.value = show
    }

    fun startRecording(title: String = "Meeting Internal") {
        _meetingTitle.value = title
        RecordingService.startService(getApplication(), title)
    }

    fun pauseRecording() {
        RecordingService.pauseService(getApplication())
    }

    fun resumeRecording() {
        RecordingService.resumeService(getApplication())
    }

    fun stopRecording() {
        recordingService?.let { service ->
            val lastFile = service.lastRecordedFile.value
            val title = _meetingTitle.value
            val durationSec = _elapsedSeconds.value
            if (lastFile != null && lastFile.exists()) {
                onNewRecordingCompleted(lastFile, title, durationSec)
            }
        }
        RecordingService.stopService(getApplication())
    }

    fun selectMeetingForDocs(meeting: MeetingItem?) {
        _selectedMeetingForDocs.value = meeting
    }

    /**
     * Import dan unggah file audio dari galeri/penyimpanan HP
     */
    fun importAudioFile(uri: Uri, context: Context, customTitle: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val title = customTitle.ifBlank { "Rekaman Audio External" }
                val sanitized = title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")

                val recordingsDir = File(context.filesDir, "recordings").apply { if (!exists()) mkdirs() }
                val targetFile = File(recordingsDir, "IMP_${sanitized}_${timeStamp}.m4a")

                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val sizeMb = String.format(Locale.US, "%.1f MB", targetFile.length() / (1024f * 1024f))
                val newItem = MeetingItem(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    dateFormatted = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).format(Date()),
                    durationFormatted = "File Terunggah",
                    localAudioFile = targetFile,
                    fileSizeFormatted = sizeMb,
                    status = ProcessingStatus.RECORDED_LOCAL,
                    summarySnippet = "File audio berhasil diimpor ($sizeMb). Menghubungkan ke AI...",
                    isUploadedFile = true,
                    durationSeconds = 0L
                )

                withContext(Dispatchers.Main) {
                    _meetings.value = listOf(newItem) + _meetings.value
                    repository.saveMeetings(_meetings.value)
                    uploadMeetingToAi(newItem)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal mengimpor file audio", e)
            }
        }
    }

    /**
     * Mengunggah audio ke server backend & melakukan live polling hingga 4 dokumen selesai diekstrak AI
     */
    fun uploadMeetingToAi(
        meeting: MeetingItem,
        serverUrl: String = UploadMeetingWorker.getServerUrl(getApplication())
    ) {
        val audio = meeting.localAudioFile ?: return

        updateMeetingStatus(meeting.id, ProcessingStatus.UPLOADING, "Sedang mengunggah audio ke server backend...")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileRequestBody = audio.asRequestBody("audio/mp4".toMediaTypeOrNull())
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("title", meeting.title)
                    .addFormDataPart("audio_file", audio.name, fileRequestBody)
                    .build()

                val uploadRequest = Request.Builder()
                    .url("$serverUrl/api/meetings/upload")
                    .post(requestBody)
                    .build()

                val uploadResponse = httpClient.newCall(uploadRequest).execute()
                if (!uploadResponse.isSuccessful) {
                    val code = uploadResponse.code
                    updateMeetingStatus(meeting.id, ProcessingStatus.FAILED, "Upload gagal (Kode HTTP $code). Periksa IP server.")
                    return@launch
                }

                val responseJson = JSONObject(uploadResponse.body?.string() ?: "{}")
                val backendMeetingId = responseJson.optString("meeting_id")

                if (backendMeetingId.isBlank()) {
                    updateMeetingStatus(meeting.id, ProcessingStatus.FAILED, "Respons server tidak memiliki Meeting ID.")
                    return@launch
                }

                updateMeetingStatus(meeting.id, ProcessingStatus.AI_PROCESSING, "Audio terunggah! Gemini AI sedang menganalisis & mengekstrak 4 dokumen...")

                var attempts = 0
                val maxAttempts = 60
                var isCompleted = false

                while (attempts < maxAttempts && !isCompleted) {
                    delay(2000)
                    attempts++

                    try {
                        val pollRequest = Request.Builder()
                            .url("$serverUrl/api/meetings/$backendMeetingId")
                            .get()
                            .build()

                        val pollResponse = httpClient.newCall(pollRequest).execute()
                        if (pollResponse.isSuccessful) {
                            val pollJson = JSONObject(pollResponse.body?.string() ?: "{}")
                            val currentStatus = pollJson.optString("status")

                            if (currentStatus == "COMPLETED") {
                                isCompleted = true
                                val docsObj = pollJson.optJSONObject("documents")
                                val notulen = docsObj?.optString("notulen") ?: ""
                                val ringkasan = docsObj?.optString("ringkasan") ?: ""
                                val poin = docsObj?.optString("poin_penting") ?: ""
                                val tanyaJawab = docsObj?.optString("tanya_jawab_anti_plot_hole") ?: ""

                                val snippet = if (ringkasan.isNotBlank()) {
                                    ringkasan.lines().filter { it.isNotBlank() && !it.startsWith("#") }.take(2).joinToString(" ")
                                } else {
                                    "4 Dokumen rapat berhasil diekstrak tanpa plot hole."
                                }

                                withContext(Dispatchers.Main) {
                                    _meetings.value = _meetings.value.map {
                                        if (it.id == meeting.id) {
                                            it.copy(
                                                status = ProcessingStatus.COMPLETED,
                                                summarySnippet = snippet,
                                                documents = MeetingAiDocuments(
                                                    notulenContent = notulen,
                                                    ringkasanContent = ringkasan,
                                                    poinPentingContent = poin,
                                                    tanyaJawabContent = tanyaJawab
                                                )
                                            )
                                        } else it
                                    }
                                    repository.saveMeetings(_meetings.value)
                                }
                                break
                            } else if (currentStatus == "FAILED") {
                                val err = pollJson.optString("error", "AI gagal memproses audio")
                                updateMeetingStatus(meeting.id, ProcessingStatus.FAILED, "Analisis gagal: $err")
                                break
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Polling attempt $attempts error: ${e.message}")
                    }
                }

                if (!isCompleted && attempts >= maxAttempts) {
                    updateMeetingStatus(meeting.id, ProcessingStatus.FAILED, "Waktu analisis habis. Ketuk untuk mencoba lagi.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Kesalahan jaringan saat upload / polling", e)
                updateMeetingStatus(meeting.id, ProcessingStatus.FAILED, "Koneksi terputus: ${e.localizedMessage}")
            }
        }
    }

    fun testServerPing(url: String) {
        _pingState.value = PingState.Pinging
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pingClient = httpClient.newBuilder()
                    .connectTimeout(4, TimeUnit.SECONDS)
                    .readTimeout(4, TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder()
                    .url("${url.trimEnd('/')}/health")
                    .get()
                    .build()
                val response = pingClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    val json = JSONObject(body)
                    val model = json.optString("model", "gemini-3.5-flash")
                    _pingState.value = PingState.Success("Terhubung! Server Online (Model: $model)")
                } else {
                    _pingState.value = PingState.Error("Server merespons kode HTTP ${response.code}")
                }
            } catch (e: Exception) {
                _pingState.value = PingState.Error("Gagal: ${e.localizedMessage ?: "Cek IP & Wi-Fi"}")
            }
        }
    }

    fun resetPingState() {
        _pingState.value = PingState.Idle
    }

    private fun updateMeetingStatus(meetingId: String, status: ProcessingStatus, snippet: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _meetings.value = _meetings.value.map {
                if (it.id == meetingId) it.copy(status = status, summarySnippet = snippet)
                else it
            }
            repository.saveMeetings(_meetings.value)
        }
    }

    private fun onNewRecordingCompleted(audioFile: File, title: String, durationSec: Long) {
        val sizeMb = String.format(Locale.US, "%.1f MB", audioFile.length() / (1024f * 1024f))
        val newItem = MeetingItem(
            id = UUID.randomUUID().toString(),
            title = title,
            dateFormatted = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).format(Date()),
            durationFormatted = "${durationSec / 60} Menit",
            localAudioFile = audioFile,
            fileSizeFormatted = sizeMb,
            status = ProcessingStatus.RECORDED_LOCAL,
            summarySnippet = "Rekaman audio berhasil disimpan ($sizeMb). Memulai upload...",
            isUploadedFile = false,
            durationSeconds = durationSec
        )
        _meetings.value = listOf(newItem) + _meetings.value
        repository.saveMeetings(_meetings.value)
        uploadMeetingToAi(newItem)
    }

    fun deleteMeeting(meetingId: String) {
        _meetings.value = _meetings.value.filter { it.id != meetingId }
        repository.saveMeetings(_meetings.value)
    }

    fun addScheduledMeeting(title: String, notes: String, timeMillis: Long, formattedDateTime: String) {
        val item = ScheduledMeeting(
            title = title,
            dateEpochMillis = timeMillis,
            formattedDateTime = formattedDateTime,
            agendaNotes = notes
        )
        _scheduledMeetings.value = listOf(item) + _scheduledMeetings.value
        repository.saveScheduledMeetings(_scheduledMeetings.value)
    }

    fun deleteScheduledMeeting(id: String) {
        _scheduledMeetings.value = _scheduledMeetings.value.filter { it.id != id }
        repository.saveScheduledMeetings(_scheduledMeetings.value)
    }

    private fun loadSavedSchedules() {
        _scheduledMeetings.value = repository.loadScheduledMeetings()
    }

    private fun loadSampleAndSavedRecordings() {
        val saved = repository.loadMeetings()
        if (saved.isNotEmpty()) {
            _meetings.value = saved
            return
        }

        val sampleCompleted = MeetingItem(
            id = "sample-1",
            title = "UI/UX Revamp Strategy & Q3 Roadmap",
            dateFormatted = "Sabtu, 14 Juni 2025",
            durationFormatted = "56 Menit",
            fileSizeFormatted = "13.8 MB",
            status = ProcessingStatus.COMPLETED,
            summarySnippet = "Membahas perombakan navigasi dan integrasi pricing model baru dengan 100% konsensus tim.",
            isUploadedFile = false,
            durationSeconds = 3360L,
            documents = MeetingAiDocuments(
                notulenContent = """
                    # NOTULEN RAPAT RESMI
                    **Agenda**: UI/UX Revamp Strategy & Q3 Roadmap
                    **Tanggal**: 14 Juni 2025 | **Durasi**: 56 Menit
                    **Daftar Hadir**: Maria Joyce (Product Lead), Malvis Berry (Eng Lead), Selma Knight (Design)
                    
                    ### 1. Keputusan Rapat:
                    - Desain navigasi menggunakan floating bottom bar disetujui.
                    - Reduksi kompleksitas checkout menjadi 2 langkah.
                    
                    ### 2. Action Items & PIC:
                    | No | Kegiatan / Tindak Lanjut | Penanggung Jawab (PIC) | Tenggat Waktu | Status |
                    |---|---|---|---|---|
                    | 1 | Finalisasi Figma Mockup | Selma Knight | 20 Juni 2025 | Selesai |
                    | 2 | Arsitektur API Backend Gemini | Malvis Berry | 25 Juni 2025 | Proses |
                    | 3 | Pengujian Kecepatan Audio AAC | Maria Joyce | 30 Juni 2025 | Terjadwal |
                """.trimIndent(),
                ringkasanContent = """
                    # RINGKASAN EKSEKUTIF (1 HALAMAN)
                    Rapat strategi UI/UX menyepakati modernisasi antarmuka aplikasi MeetAI untuk kuartal ketiga. Fokus utama adalah meningkatkan konversi user onboarding sebesar 25% dan mempercepat proses pembuatan notulen rapat internal dari 2 jam manual menjadi instan otomatis melalui Vertex AI.
                """.trimIndent(),
                poinPentingContent = """
                    # POIN-POIN PENTING & DATA KUNCI
                    - **Target Penghematan Waktu**: 85% efisiensi dokumentasi rapat.
                    - **Ukuran Audio**: 32 kbps AAC menghasilkan 14 MB/jam, menghemat bandwidth cloud hingga 70%.
                    - **Model AI**: Gemini Flash dengan konteks audio multimodal komprehensif.
                """.trimIndent(),
                tanyaJawabContent = """
                    # REKAP TANYA-JAWAB & ANTI-PLOT-HOLE
                    ### Pertanyaan Terjawab:
                    - **Q (Maria)**: Apakah audio rapat aman dari kebocoran ke pihak ketiga?
                      **A (Malvis)**: Ya, audio hanya ditransmisikan ke VPC GCP organisasi perusahaan via private service account.
                    
                    ### Daftar Pertanyaan Belum Terjawab (Pending / Plot Hole):
                    - **Pending Q**: Berapa kuota TPM (Token Per Minute) Vertex AI untuk cabang regional? *(Akan dibahas pada sesi follow-up Jumat depan)*.
                """.trimIndent()
            )
        )

        _meetings.value = listOf(sampleCompleted)
        repository.saveMeetings(_meetings.value)
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            try {
                getApplication<Application>().unbindService(serviceConnection)
            } catch (e: Exception) {
                // Ignore
            }
            isBound = false
        }
    }

    companion object {
        private const val TAG = "MeetingViewModel"
    }
}
