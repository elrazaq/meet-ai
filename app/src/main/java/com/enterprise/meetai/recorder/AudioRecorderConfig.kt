package com.enterprise.meetai.recorder

/**
 * Konfigurasi Audio Hardware-Accelerated Bawaan Android
 * Dirancang khusus untuk meeting panjang (1-3 jam):
 * - Format: AAC (Audio Specific Config / MPEG_4 container .m4a)
 * - Channel: Mono (1 Channel) -> Suara manusia tidak butuh stereo, memotong ukuran file 50%
 * - Sample Rate: 16.000 Hz (16 kHz) -> Standar emas speech-to-text / LLM, hemat CPU & memori
 * - Bitrate: 32 kbps (32.000 bps) -> Ukuran file hanya ~14.4 MB per jam!
 */
object AudioRecorderConfig {
    const val AUDIO_SAMPLING_RATE = 16000
    const val AUDIO_ENCODING_BIT_RATE = 32000
    const val AUDIO_CHANNELS = 1
    const val FILE_EXTENSION = ".m4a"
}
