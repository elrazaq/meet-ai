package com.enterprise.meetai.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.meetai.model.MeetingAiDocuments
import com.enterprise.meetai.model.MeetingItem
import com.enterprise.meetai.model.ProcessingStatus
import com.enterprise.meetai.recorder.RecordingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MeetingViewModel(application: Application) : AndroidViewModel(application) {

    private var recordingService: RecordingService? = null
    private var isBound = false

    private val _isServiceRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isServiceRecording.asStateFlow()

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

    fun stopRecording() {
        RecordingService.stopService(getApplication())
    }

    fun selectMeetingForDocs(meeting: MeetingItem?) {
        _selectedMeetingForDocs.value = meeting
    }

    fun uploadMeetingToAi(meeting: MeetingItem, serverUrl: String = com.enterprise.meetai.worker.UploadMeetingWorker.getServerUrl(getApplication())) {
        val audio = meeting.localAudioFile ?: return
        com.enterprise.meetai.worker.UploadMeetingWorker.enqueueUpload(
            context = getApplication(),
            audioFile = audio,
            title = meeting.title,
            serverUrl = serverUrl
        )
        // Update status meeting di state UI
        _meetings.value = _meetings.value.map {
            if (it.id == meeting.id) it.copy(status = ProcessingStatus.UPLOADING, summarySnippet = "Sedang mengunggah audio ke server backend...")
            else it
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
            summarySnippet = "Rekaman audio berhasil disimpan (~$sizeMb). Siap diproses AI menjadi 4 dokumen rapat."
        )
        _meetings.value = listOf(newItem) + _meetings.value
        // Otomatis antrekan upload ke backend
        uploadMeetingToAi(newItem)
    }

    private fun loadSampleAndSavedRecordings() {
        // Data inisial demonstrasi sesuai desain referensi (dengan 4 dokumen lengkap)
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
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            try {
                getApplication<Application>().unbindService(serviceConnection)
            } catch (e: Exception) {
                // Ignore unbind error
            }
            isBound = false
        }
    }
}
