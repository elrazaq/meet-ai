package com.enterprise.meetai.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class MeetingAudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    val isRecording: Boolean
        get() = recorder != null

    val currentFile: File?
        get() = currentOutputFile

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }

    /**
     * Memulai perekaman audio dengan akselerasi hardware langsung ke chip DSP bawaan ponsel.
     * Tidak membebani CPU, tidak membuat HP panas, dan ukuran file ultra-kompresi.
     */
    fun start(outputFile: File): Boolean {
        return try {
            currentOutputFile = outputFile
            recorder = createRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(AudioRecorderConfig.AUDIO_CHANNELS)
                setAudioSamplingRate(AudioRecorderConfig.AUDIO_SAMPLING_RATE)
                setAudioEncodingBitRate(AudioRecorderConfig.AUDIO_ENCODING_BIT_RATE)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setOutputFile(outputFile)
                } else {
                    setOutputFile(FileOutputStream(outputFile).fd)
                }

                prepare()
                start()
            }
            Log.d(TAG, "Perekaman dimulai: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memulai perekaman audio", e)
            release()
            false
        }
    }

    /**
     * Mendapatkan nilai amplitudo mikrofon saat ini (0 .. 32767)
     * Digunakan untuk visualisasi grafik gelombang suara real-time di UI
     */
    fun getMaxAmplitude(): Int {
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Menghentikan dan melepaskan resource MediaRecorder
     */
    fun stop(): File? {
        val file = currentOutputFile
        try {
            recorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saat menghentikan recorder", e)
        } finally {
            recorder = null
        }
        return file
    }

    fun release() {
        try {
            recorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error saat merilis recorder", e)
        } finally {
            recorder = null
            currentOutputFile = null
        }
    }

    companion object {
        private const val TAG = "MeetingAudioRecorder"
    }
}
