package com.enterprise.meetai.recorder

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.enterprise.meetai.MainActivity
import com.enterprise.meetai.MeetAiApplication
import com.enterprise.meetai.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    private lateinit var recorder: MeetingAudioRecorder
    private var wakeLock: PowerManager.WakeLock? = null
    private var tickerJob: Job? = null
    private var startTimeMillis: Long = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0)
    val currentAmplitude: StateFlow<Int> = _currentAmplitude.asStateFlow()

    private val _meetingTitle = MutableStateFlow("")
    val meetingTitle: StateFlow<String> = _meetingTitle.asStateFlow()

    private val _lastRecordedFile = MutableStateFlow<File?>(null)
    val lastRecordedFile: StateFlow<File?> = _lastRecordedFile.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }

    override fun onCreate() {
        super.onCreate()
        recorder = MeetingAudioRecorder(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val title = intent.getStringExtra(EXTRA_MEETING_TITLE) ?: "Meeting Internal"
                startRecordingSession(title)
            }
            ACTION_STOP -> {
                stopRecordingSession()
            }
        }
        return START_STICKY
    }

    /**
     * Memulai sesi perekaman audio rapat dengan proteksi WakeLock dan Foreground Service
     */
    fun startRecordingSession(title: String) {
        if (_isRecording.value) return

        _meetingTitle.value = title

        // 1. Ambil WakeLock hemat daya (PARTIAL_WAKE_LOCK) agar CPU tetap hidup saat layar mati
        acquireWakeLock()

        // 2. Siapkan file output di penyimpanan internal aplikasi (privat & aman)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val sanitizedTitle = title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val recordingsDir = File(filesDir, "recordings").apply { if (!exists()) mkdirs() }
        val audioFile = File(recordingsDir, "REC_${sanitizedTitle}_${timeStamp}${AudioRecorderConfig.FILE_EXTENSION}")

        // 3. Start hardware audio recorder
        val started = recorder.start(audioFile)
        if (!started) {
            releaseWakeLock()
            return
        }

        startTimeMillis = SystemClock.elapsedRealtime()
        _isRecording.value = true

        // 4. Promosikan ke Foreground Service (Sesuai standar Android 14+ dengan MICROPHONE type)
        val notification = buildPersistentNotification(title, startTimeMillis)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // 5. Jalankan polling timer & amplitudo untuk visualizer
        startTicker()
    }

    /**
     * Menghentikan perekaman audio rapat dan melepaskan resource
     */
    fun stopRecordingSession(): File? {
        if (!_isRecording.value) return null

        tickerJob?.cancel()
        tickerJob = null

        val outputFile = recorder.stop()
        _lastRecordedFile.value = outputFile
        _isRecording.value = false
        _elapsedSeconds.value = 0L
        _currentAmplitude.value = 0

        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        return outputFile
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive && _isRecording.value) {
                val elapsed = (SystemClock.elapsedRealtime() - startTimeMillis) / 1000
                _elapsedSeconds.value = elapsed
                _currentAmplitude.value = recorder.getMaxAmplitude()
                delay(100) // Update amplitudo 10x per detik untuk visualizer halus & hemat baterai
            }
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MeetAI:AudioRecordingWakeLock"
            ).apply {
                setReferenceCounted(false)
                // Timeout maksimal 5 jam sebagai safety fallback
                acquire(5 * 60 * 60 * 1000L)
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    private fun buildPersistentNotification(title: String, baseTime: Long): Notification {
        // PendingIntent untuk membuka aplikasi saat notifikasi ditekan
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // PendingIntent tombol "Hentikan Rekaman" langsung dari notifikasi
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, MeetAiApplication.RECORDING_CHANNEL_ID)
            .setContentTitle(getString(R.string.recording_notification_title))
            .setContentText("Rapat: $title")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setUsesChronometer(true)
            .setWhen(System.currentTimeMillis() - (SystemClock.elapsedRealtime() - baseTime))
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.stop_recording),
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecordingSession()
        recorder.release()
        releaseWakeLock()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.enterprise.meetai.action.START_RECORDING"
        const val ACTION_STOP = "com.enterprise.meetai.action.STOP_RECORDING"
        const val EXTRA_MEETING_TITLE = "extra_meeting_title"

        fun startService(context: Context, meetingTitle: String) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_MEETING_TITLE, meetingTitle)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
