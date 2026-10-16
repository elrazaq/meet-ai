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

    private val _selectedMeetingForDocs = MutableStateFlow<MeetingItem?>(null)
    val selectedMeetingForDocs: StateFlow<MeetingItem?> = _selectedMeetingForDocs.asStateFlow()

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
                viewModelScope.launch {
                    service.lastRecordedFile.collect { file ->
                        file?.let { onNewRecordingCompleted(it, _meetingTitle.value) }
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
        bindToRecordingService()
        loadSampleAndSavedRecordings()
    }

    private fun bindToRecordingService() {
        val intent = Intent(getApplication(), RecordingService::class.java)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun setShowDisclosure(show: Boolean) {
        _showDisclosure.value = show
    }

    fun startRecording(title: String) {
        val actualTitle = title.ifBlank { "Meeting Internal" }
        _meetingTitle.value = actualTitle
        RecordingService.startService(getApplication(), actualTitle)
    }

    fun pauseRecording() {
        RecordingService.pauseService(getApplication())
    }

    fun resumeRecording() {
        RecordingService.resumeService(getApplication())
    }

    fun stopRecording() {
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
                    summarySnippet = "File audio berhasil diimpor ($sizeMb). Menghubungkan ke AI..."
                )

                withContext(Dispatchers.Main) {
                    _meetings.value = listOf(newItem) + _meetings.value
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

        // 1. Update status ke UPLOADING di UI
        updateMeetingStatus(meeting.id, ProcessingStatus.UPLOADING, "Sedang mengunggah audio ke server backend...")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 2. Upload Multipart ke FastAPI Backend
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

                // 3. Update status ke AI_PROCESSING
                updateMeetingStatus(meeting.id, ProcessingStatus.AI_PROCESSING, "Audio terunggah! Gemini AI sedang menganalisis & mengekstrak 4 dokumen...")

                // 4. Polling status ke endpoint GET /api/meetings/{id} setiap 2 detik
                var attempts = 0
                val maxAttempts = 60 // Maksimal 2 menit
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
                                Log.d(TAG, "Polling selesai! Dokumen AI berhasil diperbarui di UI dan disimpan permanen.")
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

    private fun updateMeetingStatus(meetingId: String, status: ProcessingStatus, snippet: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _meetings.value = _meetings.value.map {
                if (it.id == meetingId) it.copy(status = status, summarySnippet = snippet)
                else it
            }
            repository.saveMeetings(_meetings.value)
        }
    }

    private fun onNewRecordingCompleted(audioFile: File, title: String) {
        val sizeMb = String.format(Locale.US, "%.1f MB", audioFile.length() / (1024f * 1024f))
        val newItem = MeetingItem(
            id = UUID.randomUUID().toString(),
            title = title,
            dateFormatted = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).format(Date()),
            durationFormatted = "${_elapsedSeconds.value / 60} Menit",
            localAudioFile = audioFile,
            fileSizeFormatted = sizeMb,
            status = ProcessingStatus.RECORDED_LOCAL,
            summarySnippet = "Rekaman audio berhasil disimpan ($sizeMb). Memulai upload..."
        )
        _meetings.value = listOf(newItem) + _meetings.value
        repository.saveMeetings(_meetings.value)
        uploadMeetingToAi(newItem)
    }

    fun deleteMeeting(meetingId: String) {
        _meetings.value = _meetings.value.filter { it.id != meetingId }
        repository.saveMeetings(_meetings.value)
    }

    private fun loadSampleAndSavedRecordings() {
        val saved = repository.loadMeetings()
        if (saved.isNotEmpty()) {
            _meetings.value = saved
            Log.d(TAG, "Riwayat pertemuan dimuat dari disk: ${saved.size} item")
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
                    - [x] Selma Knight: Finalisasi Figma mockup (Deadline: 20 Juni).
                    - [ ] Malvis Berry: Arsitektur API backend Gemini (Deadline: 25 Juni).
                """.trimIndent(),
                ringkasanContent = """
                    # RINGKASAN EKSEKUTIF (1 HALAMAN)
                    Rapat strategi UI/UX menyepakati modernisasi antarmuka aplikasi MeetAI untuk kuartal ketiga. Fokus utama adalah meningkatkan konversi user onboarding sebesar 25% dan mempercepat proses pembuatan notulen rapat internal dari 2 jam manual menjadi instan otomatis melalui Vertex AI.
                """.trimIndent(),
                poinPentingContent = """
                    # POIN-POIN PENTING & DATA KUNCI
                    - **Target Penghematan Waktu**: 85% efisiensi dokumentasi rapat.
                    - **Ukuran Audio**: 32 kbps AAC menghasilkan 14 MB/jam, menghemat bandwidth cloud hingga 70%.
                    - **Model AI**: Gemini 1.5/2.0 Flash dengan konteks audio multimodal 1 juta token.
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
