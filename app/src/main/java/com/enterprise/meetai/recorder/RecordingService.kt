package com.enterprise.meetai.recorder

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
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
    private var pausedDurationMillis: Long = 0L
    private var pauseTimestamp: Long = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0)
    val currentAmplitude: StateFlow<Int> = _currentAmplitude.asStateFlow()

    private val _meetingTitle = MutableStateFlow("")
    val meetingTitle: StateFlow<String> = _meetingTitle.asStateFlow()

    private val _lastRecordedFile = MutableStateFlow<File?>(null)
    val lastRecordedFile: StateFlow<File?> = _lastRecordedFile.asStateFlow()

    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "Audio focus hilang (misal: telepon masuk). Menjeda rekaman otomatis.")
                if (_isRecording.value && !_isPaused.value) {
                    pauseRecordingSession()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus kembali (telepon selesai). Melanjutkan rekaman otomatis.")
                if (_isRecording.value && _isPaused.value) {
                    resumeRecordingSession()
                }
            }
        }
    }

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
            ACTION_PAUSE -> {
                pauseRecordingSession()
            }
            ACTION_RESUME -> {
                resumeRecordingSession()
            }
            ACTION_STOP -> {
                stopRecordingSession()
            }
        }
        return START_STICKY
    }

    fun startRecordingSession(title: String) {
        if (_isRecording.value) return

        _meetingTitle.value = title
        _isPaused.value = false
        pausedDurationMillis = 0L

        acquireWakeLock()
        requestAudioFocus()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val sanitizedTitle = title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val recordingsDir = File(filesDir, "recordings").apply { if (!exists()) mkdirs() }
        val audioFile = File(recordingsDir, "REC_${sanitizedTitle}_${timeStamp}${AudioRecorderConfig.FILE_EXTENSION}")

        val started = recorder.start(audioFile)
        if (!started) {
            releaseWakeLock()
            abandonAudioFocus()
            return
        }

        startTimeMillis = SystemClock.elapsedRealtime()
        _isRecording.value = true

        val notification = buildPersistentNotification(title, _isPaused.value)
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

        startTicker()
    }

    fun pauseRecordingSession() {
        if (!_isRecording.value || _isPaused.value) return

        if (recorder.pause()) {
            _isPaused.value = true
            pauseTimestamp = SystemClock.elapsedRealtime()
            _currentAmplitude.value = 0
            updateNotification()
            Log.d(TAG, "Sesi perekaman berhasil di-PAUSE")
        }
    }

    fun resumeRecordingSession() {
        if (!_isRecording.value || !_isPaused.value) return

        if (recorder.resume()) {
            pausedDurationMillis += (SystemClock.elapsedRealtime() - pauseTimestamp)
            _isPaused.value = false
            updateNotification()
            Log.d(TAG, "Sesi perekaman berhasil di-RESUME")
        }
    }

    fun stopRecordingSession(): File? {
        if (!_isRecording.value) return null

        tickerJob?.cancel()
        tickerJob = null

        val outputFile = recorder.stop()
        _lastRecordedFile.value = outputFile
        _isRecording.value = false
        _isPaused.value = false
        _elapsedSeconds.value = 0L
        _currentAmplitude.value = 0

        abandonAudioFocus()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        return outputFile
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive && _isRecording.value) {
                if (!_isPaused.value) {
                    val totalElapsed = (SystemClock.elapsedRealtime() - startTimeMillis - pausedDurationMillis) / 1000
                    _elapsedSeconds.value = totalElapsed.coerceAtLeast(0)
                    _currentAmplitude.value = recorder.getMaxAmplitude()
                }
                delay(100)
            }
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(playbackAttributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()

            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
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
                acquire(5 * 60 * 60 * 1000L)
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun updateNotification() {
        val notification = buildPersistentNotification(_meetingTitle.value, _isPaused.value)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildPersistentNotification(title: String, isPaused: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // PendingIntent Toggle Pause / Resume
        val toggleAction = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        val toggleText = if (isPaused) "Lanjutkan" else "Jeda (Pause)"
        val toggleIntent = Intent(this, RecordingService::class.java).apply {
            action = toggleAction
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            2,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // PendingIntent Stop Recording
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isPaused) "Dijeda (Paused) • Rapat: $title" else "Sedang Merekam • Rapat: $title"

        return NotificationCompat.Builder(this, MeetAiApplication.RECORDING_CHANNEL_ID)
            .setContentTitle(if (isPaused) "Rekaman Rapat Dijeda" else getString(R.string.recording_notification_title))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_media_play,
                toggleText,
                togglePendingIntent
            )
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
        private const val TAG = "RecordingService"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.enterprise.meetai.action.START_RECORDING"
        const val ACTION_PAUSE = "com.enterprise.meetai.action.PAUSE_RECORDING"
        const val ACTION_RESUME = "com.enterprise.meetai.action.RESUME_RECORDING"
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

        fun pauseService(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeService(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
